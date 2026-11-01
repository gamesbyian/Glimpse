package io.github.gamesbyian.glimpse.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.gamesbyian.glimpse.domain.checkedCollectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

internal data class CollectionRestoreResult(
    val collectionId: String, val title: String, val count: Int
)

/**
 * Restores only a user-selected Glimpse v1 originals ZIP into a *new*
 * collection, never rewrites source/rotation/acknowledged wallpaper records.
 *
 * Stage + verify the complete external archive before any Room mutation. In
 * the rare crash window between finalizing immutable files and Room commit,
 * verified but unindexed originals may remain, as with normal Photo Picker
 * import; future imports safely deduplicate by digest.
 */
internal class CollectionZipRestorer(
    private val context: Context, private val dao: GlimpseDao
) {
    suspend fun restore(uri: Uri): CollectionRestoreResult = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose an archive through Android Documents." }
        val root = File(context.filesDir, "originals")
        if (!root.exists()) require(root.mkdir()) { "Cannot create offline originals directory." }
        require(root.isDirectory && !java.nio.file.Files.isSymbolicLink(root.toPath())) {
            "Offline originals directory is unavailable."
        }
        val workingContext = coroutineContext
        val staged = context.contentResolver.openInputStream(uri)?.use { stream ->
            CollectionZipReader.stage(stream, root) { workingContext.ensureActive() }
        } ?: error("Android could not open the selected archive.")
        var completedCollection = false
        try {
            val title = checkedCollectionTitle(staged.title)
            // The v1 ZIP contains original bytes only. Confirm Android
            // recognises each image and its decoded pixel budget, not just
            // that it matched a self-reported hash.
            val ready = staged.photos.map { item ->
                workingContext.ensureActive()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(item.temporaryFile.path, options)
                require(options.outMimeType == item.mime) {
                    "An archived file is not the declared image type."
                }
                require(options.outWidth in 1..30_000 && options.outHeight in 1..30_000 &&
                    options.outWidth.toLong() * options.outHeight <= 120_000_000L
                ) { "Archive image dimensions are outside Glimpse limits." }
                val filename = expectedPrivateOriginalName(item.sha256, item.sha256, item.mime)
                val target = File(root, filename)
                Triple(item, target, options)
            }
            val rows = ready.map { (item, target, dimensions) ->
                workingContext.ensureActive()
                // Never use exists()->renameTo(): another writer may create
                // the SHA name between those steps and be overwritten.
                // The shared publisher serializes collaborators and reserves
                // the final name exclusively, validating existing bytes.
                publishPrivateOriginalExclusively(
                    item.temporaryFile, target, item.sha256, item.bytes
                )
                PhotoRecord(
                    id = item.sha256, sha256 = item.sha256,
                    localPath = target.absolutePath, mime = item.mime,
                    width = dimensions.outWidth, height = dimensions.outHeight,
                    byteLength = item.bytes, importedAt = System.currentTimeMillis()
                )
            }
            workingContext.ensureActive()
            val collection = CollectionRecord(UUID.randomUUID().toString(),
                title, System.currentTimeMillis())
            dao.insertRestoredCollection(collection, rows)
            completedCollection = true
            CollectionRestoreResult(collection.id, title, rows.size)
        } finally {
            // Every staged ZIP image passed SHA/image validation before the
            // publisher ran. If any SHA publication or Room insert fails,
            // preserve complete staged copies for provenance-backed recovery.
            // A successful guarded Room commit makes staging redundant.
            if (completedCollection) staged.discardStaging()
        }
    }
}
