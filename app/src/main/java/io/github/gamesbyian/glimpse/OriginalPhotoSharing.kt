package io.github.gamesbyian.glimpse

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.expectedPrivateOriginalName
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Explicit, one-at-a-time share of an immutable imported original through the
 * system Sharesheet. Never supplies a file:// path or grants write permission.
 *
 * These checks defend against damaged/stale records and tampered local files.
 * Only files in Glimpse's private originals directory may reach FileProvider.
 */
internal object OriginalPhotoSharing {
    const val MAX_BATCH = 25

    /** Small, deliberate batch. Provider grants remain read-only and scoped. */
    fun requireValidBatch(photos: List<PhotoRecord>) {
        require(photos.size in 1..MAX_BATCH) {
            "Share one to 25 originals at a time."
        }
        require(photos.map { it.id }.toSet().size == photos.size) {
            "The photo selection contains duplicates."
        }
    }

    suspend fun batchChooser(context: Context, photos: List<PhotoRecord>): Intent {
        requireValidBatch(photos)
        val files = withContext(Dispatchers.IO) {
            photos.map { photo ->
                verifyPrivateOriginal(File(context.filesDir, "originals"), photo)
            }
        }
        val uris = ArrayList(files.map { file ->
            FileProvider.getUriForFile(
                context, context.packageName + ".originals", file
            )
        })
        val grants = ClipData.newRawUri("Glimpse original 1", uris.first())
        uris.drop(1).forEach { grants.addItem(ClipData.Item(it)) }
        val type = photos.map { it.mime }.distinct().singleOrNull() ?: "image/*"
        val payload = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            clipData = grants
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(payload, "Share collection originals").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun expectedName(id: String, sha: String, mime: String): String =
        expectedPrivateOriginalName(id, sha, mime)

    suspend fun chooser(context: Context, photo: PhotoRecord): Intent {
        val original = withContext(Dispatchers.IO) {
            verifyPrivateOriginal(File(context.filesDir, "originals"), photo)
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
