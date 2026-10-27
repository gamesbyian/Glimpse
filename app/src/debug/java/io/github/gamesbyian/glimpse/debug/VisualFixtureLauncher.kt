package io.github.gamesbyian.glimpse.debug

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Build
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.gamesbyian.glimpse.MainActivity
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Debug build only: seed deterministic actual Room content then open the
 * production MainActivity, not a mock Compose screen. Disposable emulator use.
 *
 * adb shell am start -n io.github.gamesbyian.glimpse/.debug.VisualFixtureLauncher --ei count 22
 */
class VisualFixtureLauncher : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(Build.HARDWARE.contains("ranchu", true) ||
            Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.contains("generic", true) ||
            Build.MODEL.contains("sdk_gphone", true)) {
            "Visual fixtures refuse a physical handset"
        }
        val count = intent.getIntExtra("count", 22).coerceIn(0, 60)
        val active = intent.getBooleanExtra("active", false)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { seed(count, active) }
            startActivity(Intent(this@VisualFixtureLauncher, MainActivity::class.java))
            finish()
        }
    }

    private suspend fun seed(count: Int, active: Boolean) {
        verifyOrientationDecode()
        val dao = GlimpseDatabase.open(applicationContext).dao()
        val collectionId = "visual-atlas-$count"
        dao.insertCollection(CollectionRecord(collectionId,
            "ATLAS FIXTURE · $count photos", 1_700_000_000_000L))
        val staging = File(filesDir, "visual-atlas").apply { mkdirs() }
        val originals = File(filesDir, "originals").apply { mkdirs() }
        repeat(count) { index ->
            // Vary six fixed photos with a unique number overlay so 22 thumbnails
            // have distinct bytes and verified content hashes.
            val source = requireNotNull(
                assets.open("art" + (index % 6 + 1) + ".jpg").use(BitmapFactory::decodeStream)
            )
            val image = Bitmap.createBitmap(480, 720, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, null, Rect(0, 0, 480, 720), paint)
            paint.color = Color.argb(225, 245, 238, 222)
            canvas.drawRoundRect(RectF(18f, 18f, 138f, 92f), 14f, 14f, paint)
            paint.color = Color.rgb(22, 70, 78)
            paint.textSize = 44f
            paint.isFakeBoldText = true
            canvas.drawText("%02d".format(index + 1), 33f, 72f, paint)
            val path = File(staging, "image-%02d.jpg".format(index + 1))
            path.outputStream().use { output ->
                check(image.compress(Bitmap.CompressFormat.JPEG, 88, output))
            }
            val id = path.inputStream().use { stream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            // Mirror PhotoImporter storage and immutable SHA filename so the
            // native Sharesheet tests the production security boundary.
            val original = File(originals, "$id.jpg")
            if (!original.exists()) {
                check(path.renameTo(original)) { "Synthetic photo finalization failed" }
            } else {
                path.delete()
            }
            dao.attach(PhotoRecord(
                id = id,
                sha256 = id,
                localPath = original.absolutePath,
                mime = "image/jpeg",
                width = 480,
                height = 720,
                byteLength = original.length(),
                importedAt = 1_700_000_000_000L + index
            ), collectionId)
            source.recycle()
            image.recycle()
        }
        if (active) {
            // Real memberships in two differently named collections let the
            // atlas prove that browse changes never reassign shuffle source.
            val alternate = "visual-atlas-alternate"
            dao.insertCollection(CollectionRecord(
                alternate, "ALTERNATE ATLAS", 1_700_000_010_000L
            ))
            dao.photosIn(collectionId).take(2).forEach { photo ->
                dao.attach(photo, alternate)
            }
            dao.saveRotation(RotationRecord(
                collectionId = collectionId,
                active = true,
                // Explicitly no wallpaper command during the capture window.
                nextEligibleAt = System.currentTimeMillis() + 12 * 60_000L,
                outcome = "VISUAL FIXTURE ONLY; no OS wallpaper was changed"
            ))
        }
        // The atlas harness reads this file via run-as, which is available
        // only for debug builds. It proves that all database writes ended,
        // unlike screen-text heuristics and slow uiautomator polling.
        File(filesDir, "visual-atlas-ready.txt").writeText(
            "READY:" + count + ":" + collectionId
        )
    }

    /**
     * Synthetic 16x8 JPEG: left half red, right half blue, EXIF Orientation=6.
     * Correct decoded pixels are upright 8x16, red above blue. This validates
     * the actual API34 platform decoder used by Home, Reel and Wallpaper.
     * No private/user photo or network content is involved.
     */
    private fun verifyOrientationDecode() {
        val encoded = Base64.decode(
            "/9j/4AAQSkZJRgABAQAAAQABAAD/4QAiRXhpZgAATU0AKgAAAAgAAQESAAMAAAABAAYAAAAAAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAAIABADASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFAEBAAAAAAAAAAAAAAAAAAAAB//EABgRAAIDAAAAAAAAAAAAAAAAAAAHRYPD/9oADAMBAAIRAxEAPwCLp4BPQsnTqMj5jLsj/9k=",
            Base64.DEFAULT
        )
        val path = File(cacheDir, "synthetic-exif-orientation-6.jpg")
        try {
            path.writeBytes(encoded)
            val bitmap = BoundedPhotoDecode.decode(path, maxEdge = 64)
            try {
                check(bitmap.width == 8 && bitmap.height == 16) {
                    "EXIF rotated JPEG did not decode as upright 8x16"
                }
                val top = bitmap.getPixel(4, 2)
                val bottom = bitmap.getPixel(4, 13)
                check(Color.red(top) > Color.blue(top) + 90 &&
                    Color.blue(bottom) > Color.red(bottom) + 90) {
                    "EXIF rotated JPEG has incorrect upright pixel order"
                }
            } finally {
                bitmap.recycle()
            }
        } finally {
            path.delete()
        }
    }
}
