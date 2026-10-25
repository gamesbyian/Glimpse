package io.github.gamesbyian.glimpse.debug

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Debug APK ONLY: publish one deterministic synthetic JPEG into the *actual*
 * Android MediaStore for end-to-end OS Photo Picker -> Glimpse import proof.
 * The emitted original is not user media and never triggers WallpaperManager.
 * Refuse physical devices, even when an adb activity start is attempted.
 */
class PickerMediaFixtureLauncher : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val verdict = File(filesDir, "picker-fixture-ready.json")
        try {
            val emulator = Build.HARDWARE.contains("ranchu", true) ||
                Build.HARDWARE.contains("goldfish", true) ||
                Build.FINGERPRINT.contains("generic", true) ||
                Build.MODEL.contains("sdk_gphone", true)
            require(emulator) { "Picker fixture refuses non-emulator hardware" }
            val image = Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)
            val bytes = try {
                image.eraseColor(Color.rgb(31, 93, 180))
                for (y in 0 until 256) for (x in 0 until 192) {
                    if ((x + y) % 29 < 7) {
                        image.setPixel(x, y, Color.rgb(238, 200, 57))
                    }
                }
                val out = java.io.ByteArrayOutputStream()
                check(image.compress(Bitmap.CompressFormat.JPEG, 90, out))
                out.toByteArray()
            } finally {
                image.recycle()
            }
            val hex = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            File(filesDir, "picker-fixture-original.jpg").writeBytes(bytes)
            val name = "GLIMPSE-C-PRIVATE-PICKER-FIXTURE.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlimpseAgentCQA/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val provider = contentResolver
            val uri = requireNotNull(
                provider.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ) { "MediaStore declined disposable JPEG" }
            try {
                requireNotNull(provider.openOutputStream(uri, "w")).use {
                    it.write(bytes)
                }
                require(provider.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) == 1) { "Could not publish disposable JPEG" }
            } catch (error: Exception) {
                provider.delete(uri, null, null)
                throw error
            }
            verdict.writeText(JSONObject().apply {
                put("status", "READY")
                put("sourceUri", uri.toString())
                put("sourceSHA256", hex)
                put("sourceBytes", bytes.size)
                put("sourceMime", "image/jpeg")
                put("sourceWidth", 192)
                put("sourceHeight", 256)
                put("sourceDisplayName", name)
                put("fixtureOnly", true)
                put("isWallPaperMutation", false)
            }.toString())
        } catch (error: Exception) {
            verdict.writeText(JSONObject().apply {
                put("status", "ERROR")
                put("reason", error.javaClass.simpleName)
            }.toString())
        } finally {
            finish()
        }
    }
}
