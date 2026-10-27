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

data class ImportResult(
    val selected: Int,
    val imported: Int,
    val duplicates: Int,
    val failures: Int
)

/**
 * Pure filesystem recovery boundary. Kept separate from Context/DAO so tests
 * can exercise actual deletion against isolated directories without Android.
 */
internal fun pruneStalePartialFiles(
    originals: File,
    now: Long
): Int {
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
 * Snapshot-only: never modifies provider URIs. Private copies remain valid after
 * Photo Picker's temporary URI grants expire. No broad media permission is used.
 */
class PhotoImporter(private val context: Context, private val dao: GlimpseDao) {
    private val originals = File(context.filesDir, "originals").apply { mkdirs() }

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

    suspend fun import(uris: List<Uri>, collectionId: String): ImportResult =
        withContext(Dispatchers.IO) {
            var imported = 0
            var duplicates = 0
            var failed = 0
            for (uri in uris) {
                try {
                    when (copyOne(uri, collectionId)) {
                        true -> imported++
                        false -> duplicates++
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    failed++
                }
            }
            ImportResult(uris.size, imported, duplicates, failed)
        }

    private suspend fun copyOne(uri: Uri, collectionId: String): Boolean {
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
            return dao.attach(photo, collectionId)
        } finally {
            temp.delete()
        }
    }
}
