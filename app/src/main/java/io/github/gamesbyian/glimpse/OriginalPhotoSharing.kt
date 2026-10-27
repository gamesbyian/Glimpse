package io.github.gamesbyian.glimpse

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.gamesbyian.glimpse.data.PhotoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Explicit, one-at-a-time share of an immutable imported original through the
 * system Sharesheet. Never supplies a file:// path or grants write permission.
 *
 * These checks defend against damaged/stale records and tampered local files.
 * Only files in Glimpse's private originals directory may reach FileProvider.
 */
internal object OriginalPhotoSharing {
    private const val MAX_SOURCE_BYTES = 50L * 1024 * 1024

    fun expectedName(id: String, sha: String, mime: String): String {
        require(id == sha && sha.matches(Regex("[0-9a-f]{64}"))) {
            "The source record has an invalid original identity."
        }
        val suffix = when (mime) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> throw IllegalArgumentException("This original format cannot be shared.")
        }
        return sha + suffix
    }

    suspend fun chooser(context: Context, photo: PhotoRecord): Intent {
        val original = withContext(Dispatchers.IO) {
            val expected = expectedName(photo.id, photo.sha256, photo.mime)
            val root = File(context.filesDir, "originals").canonicalFile
            val candidate = File(photo.localPath).canonicalFile
            require(candidate.parentFile == root && candidate.name == expected &&
                candidate.isFile && !candidate.isDirectory) {
                "The original private copy is unavailable."
            }
            require(photo.byteLength in 1..MAX_SOURCE_BYTES &&
                candidate.length() == photo.byteLength) {
                "The original copy does not match its saved size."
            }
            val digest = MessageDigest.getInstance("SHA-256")
            candidate.inputStream().buffered().use { stream ->
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val n = stream.read(chunk)
                    if (n == -1) break
                    digest.update(chunk, 0, n)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual == photo.sha256) {
                "The original no longer matches its verified import."
            }
            candidate
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.originals", original
        )
        val payload = Intent(Intent.ACTION_SEND).apply {
            type = photo.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Glimpse original photo", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(payload, "Share original photo").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

/** Dedicated concrete provider avoids OEM issues with the framework default class. */
class GlimpseOriginalFileProvider : FileProvider()
