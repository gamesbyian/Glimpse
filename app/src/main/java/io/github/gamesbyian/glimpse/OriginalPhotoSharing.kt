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
