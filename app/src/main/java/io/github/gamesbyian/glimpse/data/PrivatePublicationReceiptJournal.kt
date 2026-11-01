package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Base64
import java.util.UUID

/**
 * An app-private, fsynced provenance breadcrumb created *before* reserving a
 * final digest path. It stores only relative app-owned names, SHA, byte length
 * and an encoded destination collection ID. No provider URIs, EXIF, image
 * content, external paths or error strings are persisted.
 *
 * Crucially, a breadcrumb is NOT proof that the target's inode was created by
 * this attempt. It must never authorize moving/deleting a preexisting SHA file,
 * restoring a membership or automatic recovery. Issue #143 governs the later
 * ownership-proof, crash-injection and quarantine protocol.
 */
internal class PrivatePublicationReceiptJournal(private val originals: File) {
    private val stagePattern = Regex("^import-[A-Za-z0-9._-]{3,160}\\.partial$")
    private val receiptPattern = Regex("^publication-[0-9a-f-]{36}\\.receipt$")
    private val digestPattern = Regex("^[0-9a-f]{64}$")
    private val targetPattern = Regex("^[0-9a-f]{64}\\.(jpg|png|webp)$")

    internal class Handle internal constructor(
        val file: File,
        internal val exactBytes: ByteArray
    )

    private fun checkedRoot(): File {
        require(originals.isDirectory && !Files.isSymbolicLink(originals.toPath())) {
            "Private publication storage is unavailable."
        }
        return originals.canonicalFile
    }

    /**
     * Must be called only after the staging image was completely read, decoded,
     * SHA-checked and fsynced. A receipt write failure aborts publication but
     * the validated stage is retained by PhotoImporter.
     *
     * The receipt's CREATE_NEW reservation never modifies another file.
     * A failed/interrupted receipt write is deliberately left for read-only
     * inspection, never automatically pruned or interpreted as authority.
     */
    fun begin(
        staged: File,
        target: File,
        sha256: String,
        byteLength: Long,
        collectionId: String
    ): Handle {
        val root = checkedRoot()
        require(staged.isFile && !Files.isSymbolicLink(staged.toPath()) &&
            staged.canonicalFile.parentFile == root &&
            stagePattern.matches(staged.name)
        ) { "Unverified private import staging identity." }
        require(target.canonicalFile.parentFile == root &&
            targetPattern.matches(target.name) &&
            digestPattern.matches(sha256) &&
            target.name.substringBefore('.') == sha256 &&
            byteLength in 1L..(50L * 1024L * 1024L)
        ) { "Invalid private publication identity." }
        val encodedCollection = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(collectionId.toByteArray(Charsets.UTF_8))
        require(collectionId.isNotBlank() && encodedCollection.length <= 8192) {
            "Invalid publication collection identity."
        }
        val row = ("1\t" + staged.name + "\t" + target.name + "\t" +
            sha256 + "\t" + byteLength + "\t" + encodedCollection + "\n")
            .toByteArray(Charsets.UTF_8)
        require(row.size <= 9000)

        val receipt = File(root, "publication-${UUID.randomUUID()}.receipt")
        FileChannel.open(
            receipt.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
        ).use { channel ->
            val bytes = ByteBuffer.wrap(row)
            while (bytes.hasRemaining()) channel.write(bytes)
            channel.force(true)
        }
        return Handle(receipt, row)
    }

    /**
     * Best-effort disposal of *only this exact intact receipt* AFTER the
     * verified original and guarded Room membership have both committed.
     * A tampered, replaced or symlinked receipt remains untouched. Failure to
     * erase metadata cannot negate a successful import or delete photo bytes.
     */
    fun complete(handle: Handle): Boolean = try {
        val root = checkedRoot()
        val file = handle.file
        if (file.canonicalFile.parentFile != root ||
            !receiptPattern.matches(file.name) ||
            !file.isFile || Files.isSymbolicLink(file.toPath()) ||
            file.length() != handle.exactBytes.size.toLong() ||
            !file.readBytes().contentEquals(handle.exactBytes)
        ) false else Files.deleteIfExists(file.toPath())
    } catch (_: Exception) {
        false
    }

    /**
     * Bounded, strictly read-only corroboration of durable publication receipts.
     *
     * The receipt describes an attempt, NEVER ownership of the digest target.
     * A matching SHA only establishes current byte integrity, not which writer
     * created an inode. Concurrent publication can change this snapshot while
     * it is read. No caller may use these counts to quarantine/delete/replace
     * any file or to admit a Room membership.
     *
     * Limit both receipt count and cumulative content hashing so a future
     * diagnostic cannot synchronously scan an arbitrarily large photo library.
     * Call on Dispatchers.IO with a cancellation checkpoint.
     */
    fun inspectFileEvidence(
        maxReceipts: Int = 64,
        maxHashedBytes: Long = 64L * 1024L * 1024L,
        checkpoint: () -> Unit = {}
    ): PendingPublicationEvidence? {
        require(maxReceipts in 1..256)
        require(maxHashedBytes in 1L..(256L * 1024L * 1024L))
        return try {
            val root = checkedRoot()
            // DirectoryStream iterates lazily: do not allocate a list of
            // every imported original merely to inspect a few receipts.
            // A separate omitted counter preserves visibility into large
            // backlogs while the retained file list stays bounded.
            val receipts = mutableListOf<File>()
            var omitted = 0
            Files.newDirectoryStream(root.toPath(), "publication-*.receipt").use { stream ->
                for (path in stream) {
                    checkpoint()
                    val candidate = path.toFile()
                    if (!receiptPattern.matches(candidate.name)) continue
                    if (receipts.size < maxReceipts) receipts += candidate
                    else omitted++
                }
            }
            receipts.sortBy { it.name }
            var suspect = 0
            var stagesExact = 0
            var stagesMissing = 0
            var stagesUnverified = 0
            var targetsExact = 0
            var targetsMissing = 0
            var targetsUnverified = 0
            var hashedBytes = 0L
            var exhausted = false

            for (candidate in receipts) {
                checkpoint()
                if (!candidate.isFile ||
                    Files.isSymbolicLink(candidate.toPath()) ||
                    candidate.canonicalFile.parentFile != root ||
                    candidate.length() !in 1L..9000L
                ) {
                    suspect++
                    continue
                }
                val parts = candidate.readText(Charsets.UTF_8).trimEnd('\n').split('\t')
                val byteCount = parts.getOrNull(4)?.toLongOrNull()
                val acceptable = parts.size == 6 &&
                    parts[0] == "1" &&
                    stagePattern.matches(parts[1]) &&
                    targetPattern.matches(parts[2]) &&
                    digestPattern.matches(parts[3]) &&
                    parts[2].substringBefore('.') == parts[3] &&
                    byteCount != null && byteCount in 1L..(50L * 1024L * 1024L) &&
                    runCatching {
                        Base64.getUrlDecoder().decode(parts[5]).isNotEmpty()
                    }.getOrDefault(false)
                if (!acceptable || byteCount == null) {
                    suspect++
                    continue
                }
                val expectedBytes = byteCount
                val expectedSha = parts[3]

                // 0 = exact bytes; 1 = missing path; 2 = untrusted, invalid,
                // inaccessible or intentionally not hashed within the budget.
                fun classify(name: String): Int {
                    val file = File(root, name)
                    if (Files.isSymbolicLink(file.toPath())) return 2
                    if (!file.exists()) return 1
                    if (!file.isFile || file.canonicalFile.parentFile != root) return 2
                    if (file.length() != expectedBytes) return 2
                    if (expectedBytes > maxHashedBytes - hashedBytes) {
                        exhausted = true
                        return 2
                    }
                    hashedBytes += expectedBytes
                    return if (existingOriginalMatches(file, expectedSha, expectedBytes)) 0 else 2
                }
                when (classify(parts[1])) {
                    0 -> stagesExact++
                    1 -> stagesMissing++
                    else -> stagesUnverified++
                }
                when (classify(parts[2])) {
                    0 -> targetsExact++
                    1 -> targetsMissing++
                    else -> targetsUnverified++
                }
            }
            PendingPublicationEvidence(
                examinedReceipts = receipts.size,
                omittedReceipts = omitted,
                untrustedReceipts = suspect,
                stagesExact = stagesExact,
                stagesMissing = stagesMissing,
                stagesUnverified = stagesUnverified,
                targetsExact = targetsExact,
                targetsMissing = targetsMissing,
                targetsUnverified = targetsUnverified,
                hashBudgetExhausted = exhausted
            )
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Unreadable storage or racing mutations must yield unknown, never
            // an invented recovery authorization.
            null
        }
    }

    /** Read-only aggregate evidence; no receipt content or IDs leave callers. */
    fun inspect(): ReceiptCounts? {
        return try {
        val root = checkedRoot()
        val entries = originals.listFiles() ?: return null
        var intact = 0
        var suspect = 0
        for (candidate in entries) {
            if (!receiptPattern.matches(candidate.name)) continue
            if (!candidate.isFile || Files.isSymbolicLink(candidate.toPath()) ||
                candidate.canonicalFile.parentFile != root ||
                candidate.length() !in 1L..9000L
            ) {
                suspect++
                continue
            }
            val pieces = candidate.readText(Charsets.UTF_8).trimEnd('\n').split('\t')
            if (pieces.size == 6 && pieces[0] == "1" &&
                stagePattern.matches(pieces[1]) &&
                targetPattern.matches(pieces[2]) &&
                pieces[2].substringBefore('.') == pieces[3] &&
                digestPattern.matches(pieces[3]) &&
                (pieces[4].toLongOrNull()?.let { it in 1L..(50L * 1024L * 1024L) } == true) &&
                runCatching {
                    Base64.getUrlDecoder().decode(pieces[5]).isNotEmpty()
                }.getOrDefault(false)
            ) intact++ else suspect++
        }
        ReceiptCounts(intact, suspect)
    } catch (_: Exception) {
        null
    }
    }
}

internal data class ReceiptCounts(
    val structurallyValid: Int,
    val damagedOrUntrusted: Int
)

/**
 * Aggregate evidence only. Exact SHA/length do not prove source/target inode
 * ownership. An unverified result may simply reflect an active writer or a
 * bounded hash budget, and a missing stage is not permission to delete a target.
 */
internal data class PendingPublicationEvidence(
    val examinedReceipts: Int,
    val omittedReceipts: Int,
    val untrustedReceipts: Int,
    val stagesExact: Int,
    val stagesMissing: Int,
    val stagesUnverified: Int,
    val targetsExact: Int,
    val targetsMissing: Int,
    val targetsUnverified: Int,
    val hashBudgetExhausted: Boolean
)
