package io.github.gamesbyian.glimpse.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class ImportResult(
    val selected: Int,
    val imported: Int,
    val duplicates: Int,
    val failures: Int,
    val journalWarnings: Int = 0,
    /** Transient, in-process picker grants only; never journal or persist these. */
    val failedSources: List<Uri> = emptyList()
)

/**
 * Pure filesystem recovery boundary. Kept separate from Context/DAO so tests
 * can exercise actual deletion against isolated directories without Android.
 */
internal fun pruneStalePartialFiles(
    originals: File,
    now: Long
): Int {
    // Refuse directory-level aliases too: an app-private directory replaced
    // by a symlink must not redirect cleanup to files outside our ownership.
    if (java.nio.file.Files.isSymbolicLink(originals.toPath()) ||
        !originals.isDirectory
    ) return 0
    val root = originals.canonicalFile
    return originals.listFiles()?.count { candidate ->
        try {
            candidate.isFile &&
                !java.nio.file.Files.isSymbolicLink(candidate.toPath()) &&
                candidate.canonicalFile.parentFile == root &&
                PhotoImporter.isStaleImportPartial(candidate.name, candidate.lastModified(), now) &&
                candidate.delete()
        } catch (_: Exception) {
            false
        }
    } ?: 0
}

/**
 * Source picker URIs are untrusted, and a compromised private originals root
 * must not redirect imports to an outside directory. Refuse symlinked roots
 * before creating a single staging file or opening a provider stream.
 */
internal fun requirePrivateImportRoot(originals: File) {
    require(originals.isDirectory &&
        !java.nio.file.Files.isSymbolicLink(originals.toPath())) {
        "The private photo storage directory is unavailable."
    }
}

/**
 * If another import already committed this SHA-named original, verify its bytes
 * instead of trusting filename/length alone. This prevents a stale or corrupted
 * file from being attached to another collection as a supposedly valid copy.
 * Caller already runs in Dispatchers.IO; the source copy never gets replaced.
 */
internal fun existingOriginalMatches(
    file: File,
    expectedSha256: String,
    expectedLength: Long
): Boolean {
    if (!expectedSha256.matches(Regex("[0-9a-f]{64}")) || expectedLength <= 0L ||
        !file.isFile || java.nio.file.Files.isSymbolicLink(file.toPath()) ||
        file.length() != expectedLength
    ) return false
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) } == expectedSha256
    } catch (_: Exception) {
        false
    }
}

/**
 * Snapshot-only: never modifies provider URIs. Private copies remain valid after
 * Photo Picker's temporary URI grants expire. No broad media permission is used.
 */
class PhotoImporter(private val context: Context, private val dao: GlimpseDao) {
    private val originals = File(context.filesDir, "originals").apply { mkdirs() }
    private val journal = ImportJournal(File(context.filesDir, "import-history.log"))

    companion object {
        // A recent file may belong to an import still writing on another job.
        // Never delete imported SHA-named originals or arbitrary app files.
        const val STALE_PARTIAL_GRACE_MS = 24L * 60L * 60L * 1000L

        internal fun isStaleImportPartial(
            name: String,
            modifiedAt: Long,
            now: Long
        ): Boolean =
            name.startsWith("import-") && name.endsWith(".partial") &&
                modifiedAt > 0 && now >= modifiedAt &&
                now - modifiedAt >= STALE_PARTIAL_GRACE_MS
    }

    /**
     * Recover only abandoned staging fragments after Android process death.
     * Does not touch finished SHA originals, Room rows, provider copies or files
     * that could still be part of a recent operation. A failure is nonfatal.
     */
    suspend fun pruneStalePartialImports(now: Long = System.currentTimeMillis()): Int =
        withContext(Dispatchers.IO) { pruneStalePartialFiles(originals, now) }

    /**
     * A START lacking a terminal record is uncertain after process restart:
     * Room may already contain its photo. Re-selecting through the picker is
     * safe because immutable content hashes and memberships deduplicate.
     */
    suspend fun reconcileInterruptedImports(): Int =
        withContext(Dispatchers.IO) { journal.reconcileInterrupted() }

    suspend fun import(uris: List<Uri>, collectionId: String): ImportResult =
        withContext(Dispatchers.IO) {
            val runId = UUID.randomUUID().toString()
            var imported = 0
            var duplicates = 0
            var failed = 0
            var journalWarnings = 0
            val failedSources = mutableListOf<Uri>()
            // Journal errors must not block safe immutable source ingestion.
            // They are explicitly disclosed in the result instead.
            fun note(position: Int, state: ImportJournal.State) {
                try {
                    journal.record(runId, collectionId, position, state)
                } catch (_: Exception) {
                    journalWarnings++
                }
            }
            for ((position, uri) in uris.withIndex()) {
                note(position, ImportJournal.State.START)
                try {
                    if (copyOne(uri, collectionId)) {
                        imported++
                        note(position, ImportJournal.State.IMPORTED)
                    } else {
                        duplicates++
                        note(position, ImportJournal.State.DUPLICATE)
                    }
                } catch (cancel: CancellationException) {
                    note(position, ImportJournal.State.INTERRUPTED)
                    throw cancel
                } catch (_: Exception) {
                    failed++
                    failedSources += uri
                    // Never persist a provider URI, external filename, path or
                    // exception message in the private journal.
                    note(position, ImportJournal.State.FAILED)
                }
            }
            ImportResult(uris.size, imported, duplicates, failed, journalWarnings, failedSources.toList())
        }

    private suspend fun copyOne(uri: Uri, collectionId: String): Boolean {
        requirePrivateImportRoot(originals)
        val temp = File.createTempFile("import-", ".partial", originals)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            context.contentResolver.openInputStream(uri).use { stream ->
                requireNotNull(stream) { "Selected source is unavailable" }
                FileOutputStream(temp).use { out ->
                    val bytes = ByteArray(64 * 1024)
                    while (true) {
                        val count = stream.read(bytes)
                        if (count < 0) break
                        size += count
                        require(size <= 50L * 1024 * 1024) { "Image exceeds 50 MB limit" }
                        digest.update(bytes, 0, count)
                        out.write(bytes, 0, count)
                    }
                    out.fd.sync()
                }
            }
            require(size > 0) { "Empty image" }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.path, options)
            val mime = options.outMimeType ?: error("Not a recognized image")
            require(mime in setOf("image/jpeg", "image/png", "image/webp")) { "Unsupported format" }
            require(options.outWidth in 1..30000 && options.outHeight in 1..30000) {
                "Invalid image dimensions"
            }
            require(options.outWidth.toLong() * options.outHeight <= 120_000_000L) {
                "Image pixel budget exceeded"
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            val extension = when (mime) {
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                else -> ".jpg"
            }
            val target = File(originals, hex + extension)
            if (!target.exists()) {
                require(temp.renameTo(target)) { "Unable to finalize offline copy" }
            } else {
                require(existingOriginalMatches(target, hex, size)) {
                    // Never overwrite an existing private original, even if
                    // corrupt. The user-selected provider source and earlier
                    // collection memberships are otherwise left untouched.
                    "Existing imported original failed integrity check."
                }
            }
            val photo = PhotoRecord(
                id = hex,
                sha256 = hex,
                localPath = target.absolutePath,
                mime = mime,
                width = options.outWidth,
                height = options.outHeight,
                byteLength = size,
                importedAt = System.currentTimeMillis()
            )
            // Photo and membership commit together. An interrupted file commit may
            // leave an unreferenced immutable file; it never deletes an original.
            // The collection may disappear while an external provider takes time
            // to stream its bytes. Existence and membership insertion must share
            // one Room transaction; an earlier ViewModel check is not sufficient.
            // A rejected attach keeps the verified immutable file but creates
            // neither a phantom photo row nor a dangling membership.
            return dao.attachIfCollectionExists(photo, collectionId)
                ?: error("The destination collection was removed during import. Re-select it and retry.")
        } finally {
            temp.delete()
        }
    }
}
