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
 * Conservative filesystem cleanup boundary. Only zero-byte, aged importer
 * staging placeholders are removable. Nonempty partial files may contain
 * irreplaceable user-selected bytes after process death and are preserved
 * pending ownership-proven recovery (#143). Tests exercise isolated directories.
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
                // An aged nonempty stage may be the *only complete fsynced*
                // source after process death midway through CREATE_NEW copy.
                // Do not destroy recoverable bytes merely because the
                // provisional name is old. #143 must prove ownership before
                // quarantining/cleaning any data-bearing staging file.
                candidate.length() == 0L &&
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
 * Finalize a verified, fsynced private import without replacing any digest name.
 *
 * Android's untrusted_app SELinux policy can deny hard links under filesDir,
 * despite Linux/JVM tests succeeding. Prefer same-directory link creation when
 * supported: it atomically publishes complete bytes and fails if target exists.
 * On OS denial, use FileChannel.open with CREATE_NEW (O_EXCL): the
 * existence check AND entry creation are atomic against concurrent writers.
 * Files.copy without REPLACE_EXISTING is insufficient because its contract
 * explicitly permits a non-atomic target check. Never truncate an original.
 *
 * Copy fallback has different crash behavior: a failed or interrupted copy
 * could leave an incomplete, unregistered digest-name file. Never delete or
 * overwrite such a file automatically. The next importer refuses a corrupted
 * existing target, preserving evidence for explicit non-destructive recovery.
 * A genuine atomic no-replace rename remains desirable for a future NDK lane.
 */
internal fun publishPrivateOriginalExclusively(
    staged: File,
    target: File,
    expectedSha256: String,
    expectedLength: Long,
    preferHardLink: Boolean = true
) {
    require(staged.isFile && !java.nio.file.Files.isSymbolicLink(staged.toPath())) {
        "Verified private staging file is unavailable."
    }
    require(staged.canonicalFile.parentFile == target.canonicalFile.parentFile) {
        "Staged and final private originals must share one owned directory."
    }
    fun requireMatchingExisting() {
        require(existingOriginalMatches(target, expectedSha256, expectedLength)) {
            "Existing imported original failed integrity check."
        }
    }

    if (preferHardLink) {
        try {
            java.nio.file.Files.createLink(target.toPath(), staged.toPath())
            return
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            requireMatchingExisting()
            return
        } catch (_: java.io.IOException) {
            // Android app-private SELinux often rejects hard links. No
            // renameTo/copy-overwrite fallback can ever be used here.
        } catch (_: UnsupportedOperationException) {
            // Filesystem does not support hard links. Try exclusive create.
        } catch (_: SecurityException) {
            // Java hard-link privileges are optional, even on API 26+.
        }
    }

    try {
        // CREATE_NEW atomically reserves the digest path and refuses existing
        // regular files, symlinks and simultaneous writers. No preflight
        // exists() check and no truncate/replace-capable operation occurs.
        java.nio.channels.FileChannel.open(
            target.toPath(),
            java.nio.file.StandardOpenOption.WRITE,
            java.nio.file.StandardOpenOption.CREATE_NEW
        ).use { destination ->
            staged.inputStream().buffered().use { input ->
                val bytes = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(bytes)
                    if (count < 0) break
                    val chunk = java.nio.ByteBuffer.wrap(bytes, 0, count)
                    while (chunk.hasRemaining()) destination.write(chunk)
                }
            }
            destination.force(true)
        }
    } catch (_: java.nio.file.FileAlreadyExistsException) {
        requireMatchingExisting()
        return
    }
    // No Room record is committed until the freshly written copy has passed
    // its full SHA and length check. Failed partial targets remain untouched.
    requireMatchingExisting()
}

/**
 * A complete validated stage is itself user data until the immutable SHA
 * publication has successfully finished. A failed CREATE_NEW copy must not
 * destroy the only complete source in the importer's finally block.
 */
internal fun canDiscardImportStaging(validated: Boolean, published: Boolean): Boolean =
    !validated || published

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
     * Reclaim only aged zero-byte importer placeholders, never a data-bearing
     * stage. A crash between final-file reservation and completed copy can
     * leave the only good full image in a day-old staging file. Preserve it
     * for independently provenance-verified recovery (#143). Normal finally
     * cleanup still removes the current import's own temporary name.
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
        var validatedStaging = false
        var publishedOriginal = false
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
            // A fully read, fsynced, image-validated source must be retained
            // if the final SHA-name publication fails. A future recovery
            // receipt will link the two names without persisting provider URIs.
            validatedStaging = true
            // Atomic no-replace publication. A raced matching SHA verifies and
            // deduplicates; an existing corrupt/symlinked file is never replaced.
            publishPrivateOriginalExclusively(temp, target, hex, size)
            publishedOriginal = true
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
            // Preserve the last complete staging copy on a failed publish.
            // Normal success and unverified/empty staging clean up as before.
            if (canDiscardImportStaging(validatedStaging, publishedOriginal)) {
                temp.delete()
            }
        }
    }
}
