package io.github.gamesbyian.glimpse

import android.content.Context
import android.net.Uri
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * User-initiated ACTION_CREATE_DOCUMENT copy, never a move or original edit.
 * The external destination comes only from Android's document chooser.
 */
internal object OriginalPhotoSave {
    fun defaultName(photo: PhotoRecord): String {
        val ext = when (photo.mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("Unsupported original image type.")
        }
        require(photo.id == photo.sha256 && photo.id.matches(Regex("[0-9a-f]{64}"))) {
            "Invalid private photo identity."
        }
        return "Glimpse-original-" + photo.id.take(12) + "." + ext
    }

    fun acceptsDocumentScheme(scheme: String?): Boolean = scheme == "content"

    suspend fun copy(context: Context, source: PhotoRecord, destination: Uri): String =
        withContext(Dispatchers.IO) {
            require(acceptsDocumentScheme(destination.scheme)) {
                "Android did not provide a writable document URI."
            }
            val original = verifyPrivateOriginal(File(context.filesDir, "originals"), source)
            // Truncation requested explicitly, to avoid stale trailing bytes.
            val stream = context.contentResolver.openOutputStream(destination, "wt")
                ?: error("The selected document cannot be opened for writing.")
            stream.use { output ->
                original.inputStream().buffered().use { input ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
                output.flush()
            }
            // Providers may not offer a read-back grant or guaranteed sync.
            "Android accepted an original-image copy. Check the selected destination " +
                "to verify that its provider saved the file."
        }
}
