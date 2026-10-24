package io.github.gamesbyian.glimpse.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Disposable instrumented content:// provider, not Photo Picker itself.
 * Test APK storage owns the file and can revoke access after snapshotting.
 */
class EphemeralPhotoTestProvider : ContentProvider() {
    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Read-only test provider" }
        val leaf = uri.lastPathSegment ?: throw IllegalArgumentException("No file")
        require(leaf.matches(Regex("fixture-[a-zA-Z0-9-]+\\.jpg"))) { "Unsafe file name" }
        val root = requireNotNull(context).cacheDir.canonicalFile
        val source = File(root, leaf).canonicalFile
        require(source.parentFile == root && source.isFile) { "Fixture missing" }
        return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri) = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ) = 0
}
