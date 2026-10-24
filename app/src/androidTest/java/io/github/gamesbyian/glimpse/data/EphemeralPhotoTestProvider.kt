package io.github.gamesbyian.glimpse.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor

/**
 * Disposable instrumented content:// provider, not Photo Picker itself.
 * Test APK storage owns the file and can revoke access after snapshotting.
 */
class EphemeralPhotoTestProvider : ContentProvider() {
    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Read-only test provider" }
        val leaf = uri.lastPathSegment ?: throw IllegalArgumentException("No fixture")
        require(leaf.matches(Regex("fixture-[a-zA-Z0-9-]+\\.jpg"))) {
            "Unsafe fixture name"
        }
        // The instrumentation process runs under the app-under-test UID on
        // some Android releases. A test APK's private /data path is therefore
        // not a safe shared fixture source. Instead stream actual JPEG bytes
        // via the ContentProvider file descriptor contract.
        val pipes = ParcelFileDescriptor.createPipe()
        val reader = pipes[0]
        val writer = pipes[1]
        Thread({
            val bitmap = android.graphics.Bitmap.createBitmap(
                64, 88, android.graphics.Bitmap.Config.ARGB_8888
            )
            try {
                bitmap.eraseColor(android.graphics.Color.rgb(47, 132, 183))
                bitmap.setPixel(13, 17, android.graphics.Color.WHITE)
                ParcelFileDescriptor.AutoCloseOutputStream(writer).use { output ->
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 93, output))
                }
            } finally {
                bitmap.recycle()
                try { writer.close() } catch (_: Exception) { }
            }
        }, "glimpse-ephemeral-uri-fixture").start()
        return reader
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
