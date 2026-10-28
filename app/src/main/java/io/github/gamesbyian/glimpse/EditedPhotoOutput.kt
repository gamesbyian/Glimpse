package io.github.gamesbyian.glimpse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.GlimpseDao
import io.github.gamesbyian.glimpse.data.existingOriginalMatches
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Bounded, non-destructive rendering of user-selected rotation/crop.
 * ImageDecoder handles EXIF before the geometry is applied. A saved PNG
 * contains edited pixels but intentionally excludes the source EXIF.
 */
internal data class EditedCollectionCopy(val record: PhotoRecord, val newlyAttached: Boolean)

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

    /**
     * Save a separate PNG as a regular, SHA-identified offline photo in the
     * chosen collection. The source original and its memberships are intact.
     * This first edit milestone stores baked pixels, not a revision recipe.
     */
    suspend fun addEditedVersion(
        context: Context, dao: GlimpseDao, original: PhotoRecord,
        geometry: PhotoEditGeometry, collectionId: String
    ): EditedCollectionCopy = withContext(Dispatchers.IO) {
        require(dao.collectionExists(collectionId) &&
            dao.findMembership(collectionId, original.id) != null) {
            "The original is no longer in the chosen collection."
        }
        val pixels = renderVerified(context, original, geometry, MAX_EXPORT_EDGE)
        val root = File(context.filesDir, "originals")
        val temporary = File.createTempFile("import-", ".partial", root)
        try {
            FileOutputStream(temporary).use { output ->
                require(pixels.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Unable to encode edited image."
                }
                output.fd.sync()
            }
            val byteCount = temporary.length()
            require(byteCount in 1..(50L * 1024 * 1024)) {
                "The edited image is too large for local importing."
            }
            val digest = MessageDigest.getInstance("SHA-256")
            temporary.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            val id = digest.digest().joinToString("") { "%02x".format(it) }
            val target = File(root, id + ".png")
            if (!target.exists()) {
                require(temporary.renameTo(target)) {
                    "Unable to finalize the edited offline copy."
                }
            } else {
                require(existingOriginalMatches(target, id, byteCount)) {
                    "An existing edited original is damaged; it was not overwritten."
                }
            }
            val record = PhotoRecord(
                id = id, sha256 = id, localPath = target.absolutePath,
                mime = "image/png", width = pixels.width, height = pixels.height,
                byteLength = byteCount, importedAt = System.currentTimeMillis()
            )
            val attached = dao.attachIfCollectionExists(record, collectionId)
                ?: error("The destination collection disappeared during editing.")
            EditedCollectionCopy(record, attached)
        } finally {
            pixels.recycle()
            temporary.delete()
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
