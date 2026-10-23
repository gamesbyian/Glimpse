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
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.gamesbyian.glimpse.MainActivity
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
        val count = intent.getIntExtra("count", 22).coerceIn(0, 60)
        val active = intent.getBooleanExtra("active", false)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { seed(count, active) }
            startActivity(Intent(this@VisualFixtureLauncher, MainActivity::class.java))
            finish()
        }
    }

    private suspend fun seed(count: Int, active: Boolean) {
        val dao = GlimpseDatabase.open(applicationContext).dao()
        val collectionId = "visual-atlas-$count"
        dao.insertCollection(CollectionRecord(collectionId,
            "ATLAS FIXTURE · $count photos", 1_700_000_000_000L))
        val folder = File(filesDir, "visual-atlas").apply { mkdirs() }
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
            val path = File(folder, "image-%02d.jpg".format(index + 1))
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
            dao.attach(PhotoRecord(
                id = id,
                sha256 = id,
                localPath = path.absolutePath,
                mime = "image/jpeg",
                width = 480,
                height = 720,
                byteLength = path.length(),
                importedAt = 1_700_000_000_000L + index
            ), collectionId)
            source.recycle()
            image.recycle()
        }
        if (active) {
            dao.saveRotation(RotationRecord(
                collectionId = collectionId,
                active = true,
                nextEligibleAt = System.currentTimeMillis() + 2 * 60_000L,
                outcome = "VISUAL FIXTURE ONLY; no OS wallpaper was changed"
            ))
        }
    }
}
