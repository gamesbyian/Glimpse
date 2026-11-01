package io.github.gamesbyian.glimpse.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Narrow, explicit-consent recovery for a digest path that is truly absent.
 * Never cleans up a receipt, staged source, ambiguous existing digest or
 * database row. No automatic startup, worker, widget, or background caller.
 *
 * This backend is not exposed in the product UI until independent Android,
 * consent and accessibility acceptance. It does NOT solve occupied/crashed
 * digest ownership, rollback, power loss or disk-full recovery under #143.
 */
internal class ManualMissingTargetStageResume(
    private val originals: File,
    private val dao: GlimpseDao
) {
    suspend fun resumeOnlyAfterExplicitConfirmation(
        userConfirmed: Boolean
    ): MissingTargetResumeResult = withContext(Dispatchers.IO) {
        require(userConfirmed) { "An explicit user decision is required." }
        val ctx = currentCoroutineContext()
        val journal = PrivatePublicationReceiptJournal(originals)
        val scan = journal.inspectMissingTargetStages(
            checkpoint = { ctx.ensureActive() }
        ) ?: return@withContext MissingTargetResumeResult(unavailable = true)

        var attached = 0
        var duplicates = 0
        var refused = 0
        for (candidate in scan.candidates) {
            ctx.ensureActive()
            try {
                // Preflight is a snapshot. Revalidate receipt, source bytes,
                // destination absence and collection immediately before use.
                if (!journal.isReceiptIntact(candidate.receipt) ||
                    !Files.notExists(candidate.target.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                    !existingOriginalMatches(
                        candidate.staged, candidate.sha256, candidate.byteLength,
                        checkpoint = { ctx.ensureActive() }
                    ) ||
                    dao.findCollection(candidate.collectionId) == null
                ) {
                    refused++
                    continue
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(candidate.staged.path, options)
                val mime = options.outMimeType
                val extension = when (mime) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    "image/webp" -> ".webp"
                    else -> null
                }
                if (extension == null ||
                    candidate.target.name != candidate.sha256 + extension ||
                    options.outWidth !in 1..30000 ||
                    options.outHeight !in 1..30000 ||
                    options.outWidth.toLong() * options.outHeight > 120_000_000L
                ) {
                    refused++
                    continue
                }

                val prior = dao.findPhoto(candidate.sha256)
                if (prior != null &&
                    (prior.sha256 != candidate.sha256 ||
                     prior.localPath != candidate.target.absolutePath ||
                     prior.mime != mime ||
                     prior.byteLength != candidate.byteLength ||
                     prior.width != options.outWidth ||
                     prior.height != options.outHeight)
                ) {
                    refused++
                    continue
                }
                // The existing SHA-scoped JVM/OS gate and CREATE_NEW publisher
                // protect concurrent writers. A newly occupied corrupt or
                // foreign target is refused, never replaced or quarantined.
                publishPrivateOriginalExclusively(
                    candidate.staged, candidate.target,
                    candidate.sha256, candidate.byteLength
                )
                val photo = PhotoRecord(
                    id = candidate.sha256,
                    sha256 = candidate.sha256,
                    localPath = candidate.target.absolutePath,
                    mime = requireNotNull(mime),
                    width = options.outWidth,
                    height = options.outHeight,
                    byteLength = candidate.byteLength,
                    importedAt = System.currentTimeMillis()
                )
                val result = dao.attachRecoveredOriginalIfMatching(photo, candidate.collectionId)
                when (result) {
                    true -> attached++
                    false -> duplicates++
                    null -> refused++
                }
                // Preserve receipt and source even after success. Retirement
                // and storage cleanup require a separate safe owner action.
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Fail closed and preserve all bytes, including a partial
                // newly reserved target if a write was interrupted.
                refused++
            }
        }
        MissingTargetResumeResult(
            inspectedReceipts = scan.examinedReceipts,
            omittedReceipts = scan.omittedReceipts,
            possibleCandidates = scan.possibleCount,
            attached = attached,
            duplicates = duplicates,
            refused = refused,
            partialScan = scan.hashBudgetExhausted,
            unavailable = false
        )
    }
}

/** Only bounded counts may leave the data layer, never paths or photo IDs. */
internal data class MissingTargetResumeResult(
    val inspectedReceipts: Int = 0,
    val omittedReceipts: Int = 0,
    val possibleCandidates: Int = 0,
    val attached: Int = 0,
    val duplicates: Int = 0,
    val refused: Int = 0,
    val partialScan: Boolean = false,
    val unavailable: Boolean = false
)
