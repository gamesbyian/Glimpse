package io.github.gamesbyian.glimpse

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * An explicit Share action for edited pixels. Never exports an original
 * URI, original metadata, a private path, or an unverified source.
 *
 * The separate FileProvider authority exposes only this narrow cache folder,
 * not the immutable originals or internal Room/settings data. Android gets a
 * temporary read-only content grant when the user opens its Sharesheet.
 */
internal object EditedPhotoSharing {
    private const val DIRECTORY = "shared-edits"
    private const val MAX_BYTES = 50L * 1024 * 1024
    private const val STALE_MILLIS = 24L * 60 * 60 * 1000

    internal fun shareCache(context: Context): File {
        val folder = File(context.cacheDir, DIRECTORY)
        require(folder.isDirectory || folder.mkdirs()) {
            "Unable to prepare the temporary edited-share cache."
        }
        require(folder.canonicalFile.parentFile == context.cacheDir.canonicalFile) {
            "Unexpected temporary-share directory."
        }
        return folder
    }

    private fun removeExpired(folder: File, now: Long) {
        folder.listFiles()?.filter { file ->
            file.isFile && !file.isDirectory &&
                (file.name.endsWith(".png") || file.name.endsWith(".partial")) &&
                file.lastModified() < now - STALE_MILLIS
        }?.forEach { it.delete() }
    }

    suspend fun chooser(
        context: Context, original: PhotoRecord, geometry: PhotoEditGeometry
    ): Intent = withContext(Dispatchers.IO) {
        val folder = shareCache(context)
        removeExpired(folder, System.currentTimeMillis())
        // The same verified, EXIF-upright and bounded renderer powers the
        // edited PNG Save As flow. PNG contains only transformed image pixels.
        val pixels = EditedPhotoOutput.renderVerified(
            context, original, geometry, EditedPhotoOutput.MAX_EXPORT_EDGE
        )
        val staged = File.createTempFile("glimpse-", ".partial", folder)
        var finished: File? = null
        try {
            FileOutputStream(staged).use { output ->
                require(pixels.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    "Unable to render the edited PNG for sharing."
                }
                output.fd.sync()
            }
            require(staged.length() in 1..MAX_BYTES) {
                "The edited PNG exceeds the safe sharing size limit."
            }
            // Do not use the original's SHA or identifying filename for a
            // share target; only the user-chosen transformation is relevant.
            val target = File(folder, "glimpse-edited-" + UUID.randomUUID() + ".png")
            require(staged.renameTo(target)) {
                "Unable to prepare the edited photo for Android sharing."
            }
            finished = target
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".edits", target
            )
            val payload = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Glimpse edited PNG", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(payload, "Share edited PNG").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (failure: Exception) {
            finished?.delete()
            throw failure
        } finally {
            staged.delete()
            pixels.recycle()
        }
    }
}

/** Dedicated authority limits shared-file access to temporary edited PNGs. */
class GlimpseEditedFileProvider : FileProvider()
