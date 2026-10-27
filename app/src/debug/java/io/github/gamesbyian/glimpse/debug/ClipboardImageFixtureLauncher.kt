package io.github.gamesbyian.glimpse.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import org.json.JSONObject
import java.io.File

/**
 * Disposable debug-only synthetic clipboard image, never user media.
 * Android 34 emulator only. Published source is the existing picker fixture.
 */
class ClipboardImageFixtureLauncher : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outcome = File(filesDir, "clipboard-fixture-ready.json")
        try {
            val emulator = Build.HARDWARE.contains("ranchu", true) ||
                Build.HARDWARE.contains("goldfish", true) ||
                Build.FINGERPRINT.contains("generic", true) ||
                Build.MODEL.contains("sdk_gphone", true)
            require(emulator) { "Never manipulate clipboard on a physical phone." }
            val source = JSONObject(File(filesDir, "picker-fixture-ready.json").readText())
            require(source.getString("status") == "READY")
            val uri = Uri.parse(source.getString("sourceUri"))
            require(uri.scheme == "content" && uri.authority == "media")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = if (intent.getBooleanExtra("textOnly", false)) {
                ClipData.newPlainText("Synthetic text", "no image copied")
            } else {
                ClipData.newUri(contentResolver, "Synthetic image fixture", uri)
            }
            clipboard.setPrimaryClip(clip)
            outcome.writeText(JSONObject().apply {
                put("status", "READY")
                put("mode", if (intent.getBooleanExtra("textOnly", false)) "TEXT" else "IMAGE")
                put("sourceSHA256", source.getString("sourceSHA256"))
            }.toString())
        } catch (error: Exception) {
            outcome.writeText(JSONObject().apply {
                put("status", "ERROR")
                put("reason", error.javaClass.simpleName)
            }.toString())
        } finally {
            finish()
        }
    }
}
