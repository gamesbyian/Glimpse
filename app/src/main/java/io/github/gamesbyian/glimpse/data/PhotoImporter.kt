package io.github.gamesbyian.glimpse.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

data class ImportResult(
    val selected: Int,
    val imported: Int,
    val duplicates: Int,
    val failures: Int
)

/**
 * Snapshot-only: never modifies provider URIs. Private copies remain valid after
 * Photo Picker's temporary URI grants expire. No broad media permission is used.
 */
class PhotoImporter(private val context: Context, private val dao: GlimpseDao) {
    private val originals = File(context.filesDir, "originals").apply { mkdirs() }

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
            // Content-addressed naming is not enough: an existing private
            // copy could have been truncated or corrupted after an earlier
            // import. Verify bytes before reusing its identity.
            val existingVerified = Files.isRegularFile(
                target.toPath(), LinkOption.NOFOLLOW_LINKS
            ) && target.length() == size && sha256(target) == hex
            if (!existingVerified) {
                // The temp and target share a directory, so a successful
                // atomic move never exposes a partially written original.
                // An unsupported atomic move must fail closed rather than
                // replacing a previously indexed photo non-atomically.
                Files.move(
                    temp.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
                require(target.isFile && target.length() == size && sha256(target) == hex) {
                    "Offline copy failed final SHA-256 verification"
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
            return dao.attach(photo, collectionId)
        } finally {
            temp.delete()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
