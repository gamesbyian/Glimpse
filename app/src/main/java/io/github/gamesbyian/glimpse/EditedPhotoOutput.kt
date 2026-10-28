package io.github.gamesbyian.glimpse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bounded, non-destructive rendering of user-selected rotation/crop.
 * ImageDecoder handles EXIF before the geometry is applied. A saved PNG
 * contains edited pixels but intentionally excludes the source EXIF.
 */
internal object EditedPhotoOutput {
    const val MAX_EXPORT_EDGE = 3072
    const val MAX_PREVIEW_EDGE = 960

    fun defaultName(photo: PhotoRecord): String {
        require(photo.id == photo.sha256 &&
            photo.id.matches(Regex("[0-9a-f]{64}")))
        return "glimpse-edited-" + photo.id.take(12) + ".png"
    }

    fun renderVerified(context: Context, photo: PhotoRecord,
                       geometry: PhotoEditGeometry, maxEdge: Int): Bitmap {
        require(maxEdge in 1..MAX_EXPORT_EDGE)
        val source = verifyPrivateOriginal(File(context.filesDir, "originals"), photo)
        return renderFromOriginal(source, geometry, maxEdge)
    }

    internal fun renderFromOriginal(source: File, geometry: PhotoEditGeometry,
                                    maxEdge: Int): Bitmap {
        require(maxEdge in 1..MAX_EXPORT_EDGE)
        var current = BoundedPhotoDecode.decode(source, maxEdge)
        val uprightWidth = current.width
        val uprightHeight = current.height
        try {
            if (geometry.clockwiseQuarterTurns != 0) {
                val matrix = Matrix().apply {
                    postRotate(90f * geometry.clockwiseQuarterTurns)
                }
                val rotated = Bitmap.createBitmap(current, 0, 0, uprightWidth,
                    uprightHeight, matrix, true)
                if (rotated !== current) current.recycle()
                current = rotated
            }
            val crop = geometry.pixelCrop(uprightWidth, uprightHeight)
            if (crop.left != 0 || crop.top != 0 ||
                crop.width != current.width || crop.height != current.height) {
                val cut = Bitmap.createBitmap(current, crop.left, crop.top,
                    crop.width, crop.height)
                if (cut !== current) current.recycle()
                current = cut
            }
            return current
        } catch (failure: Throwable) {
            current.recycle()
            throw failure
        }
    }

    suspend fun writePng(context: Context, photo: PhotoRecord,
                         geometry: PhotoEditGeometry, destination: Uri): String =
        withContext(Dispatchers.IO) {
            require(destination.scheme == "content") {
                "Choose an Android document destination."
            }
            val pixels = renderVerified(context, photo, geometry, MAX_EXPORT_EDGE)
            try {
                val stream = context.contentResolver.openOutputStream(destination, "wt")
                    ?: error("Android could not open the selected destination.")
                stream.use {
                    require(pixels.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                        "Unable to encode the edited PNG."
                    }
                    it.flush()
                }
                "Android accepted the edited PNG (" + pixels.width + " × " +
                    pixels.height + " pixels). Check the selected destination. " +
                    "The unchanged original remains in Glimpse."
            } finally {
                pixels.recycle()
            }
        }
}
