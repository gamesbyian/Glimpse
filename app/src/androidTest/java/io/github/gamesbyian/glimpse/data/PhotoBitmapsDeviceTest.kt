package io.github.gamesbyian.glimpse.data

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Real Android JPEG EXIF orientation decoding, without altering original
 * content-addressed bytes. Emulator only, no Android wallpaper mutation.
 */
@RunWith(AndroidJUnit4::class)
class PhotoBitmapsDeviceTest {
    private lateinit var temp: File

    @Before fun emulatorOnly() {
        val emulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)
        assumeTrue("EXIF fixture scoped to disposable Android emulator", emulator)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        temp = File.createTempFile("glimpse-exif-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(64, 88, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(40, 140, 185))
        for (x in 0 until 25) for (y in 0 until 25) {
            bitmap.setPixel(x, y, Color.rgb(225, 35, 42))
        }
        try {
            temp.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output))
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun sha() = MessageDigest.getInstance("SHA-256")
        .digest(temp.readBytes()).joinToString("") { "%02x".format(it) }

    @Test fun cameraRotate90SwapsPreviewDimensionsWithoutChangingStoredJpeg() {
        try {
            ExifInterface(temp.path).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString()
                )
                saveAttributes()
            }
            val digest = sha()
            val preview = requireNotNull(PhotoBitmaps.decodeOriented(temp.path, 1024))
            try {
                assertEquals(88, preview.width)
                assertEquals(64, preview.height)
                val red = preview.getPixel(preview.width - 12, 12)
                assertTrue("Red top-left marker should rotate to top-right",
                    Color.red(red) > 160 && Color.red(red) > Color.green(red) * 2)
                assertEquals(digest, sha())
            } finally {
                preview.recycle()
            }
        } finally {
            temp.delete()
        }
    }

    @Test fun cameraRotate270AlsoSwapsOutputAndSmallBoundedSampleWorks() {
        try {
            ExifInterface(temp.path).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_270.toString()
                )
                saveAttributes()
            }
            val digest = sha()
            val output = requireNotNull(PhotoBitmaps.decodeOriented(temp.path, 64))
            try {
                assertEquals(44, output.width)
                assertEquals(32, output.height)
                assertEquals(digest, sha())
            } finally {
                output.recycle()
            }
        } finally {
            temp.delete()
        }
    }
}
