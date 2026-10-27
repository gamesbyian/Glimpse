package io.github.gamesbyian.glimpse.debug

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/** Debug only; publish/delete exactly two synthetic images on disposable emulator. */
class PickerBatchFixtureLauncher : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ready = File(filesDir, "batch-fixture-ready.json")
        val inserted = mutableListOf<Uri>()
        try {
            require(Build.HARDWARE.contains("ranchu", true) ||
                Build.HARDWARE.contains("goldfish", true) ||
                Build.FINGERPRINT.contains("generic", true) ||
                Build.MODEL.contains("sdk_gphone", true)) { "Real handset refused" }
            if (intent.getBooleanExtra("cleanup", false)) {
                val rows = JSONObject(ready.readText()).getJSONArray("photos")
                require(rows.length() == 2)
                var deleted = 0
                for (index in 0..1) {
                    val row = rows.getJSONObject(index)
                    require(row.getString("name") == displayName(index))
                    val uri = Uri.parse(row.getString("uri"))
                    require(uri.scheme == "content" && uri.authority == "media" &&
                        uri.path?.startsWith("/external/images/media/") == true)
                    deleted += contentResolver.delete(uri, null, null)
                }
                File(filesDir,"batch-fixture-cleanup.json").writeText(
                    JSONObject().put("deleted", deleted)
                        .put("onlyOwnSyntheticSources", true).toString())
                return
            }
            val rows = JSONArray()
            for (index in 0..1) {
                val bitmap = Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)
                val bytes = try {
                    bitmap.eraseColor(if (index==0) Color.rgb(32, 125, 103)
                        else Color.rgb(158, 46, 118))
                    for (y in 0 until 256) for (x in 0 until 192)
                        if ((x + (index+1)*y) % 31 < 8)
                            bitmap.setPixel(x, y, if (index==0)
                                Color.rgb(244, 214, 93) else Color.rgb(252, 156, 72))
                    ByteArrayOutputStream().also {
                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it))
                    }.toByteArray()
                } finally { bitmap.recycle() }
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                File(filesDir, "batch-original-$index.jpg").writeBytes(bytes)
                val v = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName(index))
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlimpseAgentCBatch/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = requireNotNull(contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v))
                inserted.add(uri)
                requireNotNull(contentResolver.openOutputStream(uri, "w")).use { it.write(bytes) }
                require(contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) == 1)
                rows.put(JSONObject().put("id", index).put("sha", sha)
                    .put("bytes", bytes.size).put("uri", uri.toString())
                    .put("name", displayName(index)))
            }
            require(rows.getJSONObject(0).getString("sha") != rows.getJSONObject(1).getString("sha"))
            ready.writeText(JSONObject().put("status", "READY")
                .put("photos", rows).put("emulatorOnly", true).toString())
        } catch (failure: Exception) {
            for (uri in inserted) runCatching { contentResolver.delete(uri, null, null) }
            ready.writeText(JSONObject().put("status", "ERROR")
                .put("failureClass", failure.javaClass.simpleName).toString())
        } finally { finish() }
    }
    private fun displayName(index: Int) =
        if (index == 0) "GLIMPSE-C-BATCH-ONE.jpg" else "GLIMPSE-C-BATCH-TWO.jpg"
}
