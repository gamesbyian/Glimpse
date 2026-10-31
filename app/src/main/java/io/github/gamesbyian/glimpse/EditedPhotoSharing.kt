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
import java.nio.file.Files

/**
 * An explicit Share action for edited pixels. Never exports an original
 * URI, original metadata, a private path, or an unverified source.
 *
 * The separate FileProvider authority exposes only this narrow cache folder,
 * not the immutable originals or internal Room/settings data. Android gets a
 * temporary read-only content grant when the user opens its Sharesheet.
 */
/**
 * Remove only explicitly Glimpse-owned, expired share-cache objects.
 * Arbitrarily named PNGs and links can appear inside this app-private cache,
 * but their presence never grants permission to delete them.
 */
internal fun pruneOwnedEditedShareCache(folder: File, now: Long): Int {
    if (!folder.isDirectory || Files.isSymbolicLink(folder.toPath())) return 0
    val root = folder.canonicalFile
    val owned = Regex("^(glimpse-edited-[A-Za-z0-9_-]{6,80}\\.png|" +
        "glimpse-[A-Za-z0-9_-]{6,80}\\.partial)$")
    return folder.listFiles()?.count { file ->
        try {
            val modified = file.lastModified()
            file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.canonicalFile.parentFile == root &&
                owned.matches(file.name) &&
                modified > 0 && now >= modified &&
                now - modified >= 24L * 60L * 60L * 1000L &&
                file.delete()
        } catch (_: Exception) {
            false
        }
    } ?: 0
}

internal object EditedPhotoSharing {
    private const val DIRECTORY = "shared-edits"
    private const val MAX_BYTES = 50L * 1024 * 1024

    internal fun shareCache(context: Context): File {
        val folder = File(context.cacheDir, DIRECTORY)
        require(folder.isDirectory || folder.mkdirs()) {
            "Unable to prepare the temporary edited-share cache."
        }
        require(!Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == context.cacheDir.canonicalFile) {
            "Unexpected temporary-share directory."
        }
        return folder
    }

    suspend fun chooser(
        context: Context, original: PhotoRecord, geometry: PhotoEditGeometry
    ): Intent = withContext(Dispatchers.IO) {
        val folder = shareCache(context)
        pruneOwnedEditedShareCache(folder, System.currentTimeMillis())
        // The same verified, EXIF-upright and bounded renderer powers the
        // edited PNG Save As flow. PNG contains only transformed image pixels.
        val pixels = EditedPhotoOutput.renderVerified(
            context, original, geometry, EditedPhotoOutput.MAX_EXPORT_EDGE
        )
        var finished: File? = null
        try {
            // CREATE a unique, owned cache destination atomically. Do not
            // perform staged.renameTo(target): POSIX rename can replace an
            // existing file even if its random name appeared after a check.
            val target = File.createTempFile("glimpse-edited-", ".png", folder)
            finished = target
            FileOutputStream(target).use { output ->
                require(pixels.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    "Unable to render the edited PNG for sharing."
                }
                output.fd.sync()
            }
            require(target.length() in 1..MAX_BYTES) {
                "The edited PNG exceeds the safe sharing size limit."
            }
            // Only after successful fsync does Android receive this freshly
            // generated image through a scoped, read-only FileProvider grant.
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
            pixels.recycle()
        }
    }
}

/** Dedicated authority limits shared-file access to temporary edited PNGs. */
class GlimpseEditedFileProvider : FileProvider()
