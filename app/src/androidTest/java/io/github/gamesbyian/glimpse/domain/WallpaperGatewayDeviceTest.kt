package io.github.gamesbyian.glimpse.domain

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Actual wallpaper OS ID readback and persisted uncertainty behavior.
 * Destructive Home wallpaper mutation is emulator-only and uses synthetic
 * fixture pixels. Passing this does NOT prove the displayed crop, Lockscreen,
 * arbitrary OEMs, background/Doze eligibility, or current wallpaper at a
 * future time.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperGatewayDeviceTest {
    private lateinit var context: Context

    @Before fun onlyDisposableEmulators() {
        val emulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)
        assumeTrue("Never change a real user's wallpaper during acceptance tests", emulator)
        context = ApplicationProvider.getApplicationContext()
    }

    private fun testBitmapFile(base: Int = Color.rgb(39, 126, 168)): File {
        val file = File.createTempFile("glimpse-wallpaper-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(320, 560, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(base)
            for (y in 40 until 520 step 40) {
                for (x in 0 until 320) {
                    bitmap.setPixel(x, y, Color.rgb(240, (x + y) % 240, 90))
                }
            }
            file.outputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream))
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun sha(file: File) = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test fun explicitHomeCommandAgreesWithImmediateOsWallpaperIdReadback() = runBlocking {
        val system = WallpaperManager.getInstance(context)
        assumeTrue(
            "Wallpaper setting not available on this emulator: cannot claim OS acceptance",
            system.isWallpaperSupported && system.isSetWallpaperAllowed
        )
        val image = testBitmapFile()
        val database = Room.inMemoryDatabaseBuilder(
            context, GlimpseDatabase::class.java
        ).build()
        try {
            val dao = database.dao()
            dao.insertCollection(CollectionRecord("os-proof", "OS proof", 1L))
            val id = sha(image)
            val photo = PhotoRecord(
                id = id, sha256 = id, localPath = image.absolutePath,
                mime = "image/jpeg", width = 320, height = 560,
                byteLength = image.length(), importedAt = 1L
            )
            dao.attach(photo, "os-proof")
            val before = system.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val outcome = WallpaperCommands(context, dao).applySelected(id)
            val record = requireNotNull(dao.readRotation())
            val actual = system.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            assertTrue("Expected positive new wallpaper ID, got " + actual, actual > 0)
            assertTrue("Expected new Home wallpaper ID: " + before + " -> " + actual,
                actual != before)
            assertEquals(id, record.lastAcknowledgedPhotoId)
            assertNull(record.pendingPhotoId)
            assertTrue(outcome, outcome.contains("acknowledged", ignoreCase = true))
            val recordedId = Regex("ID ([0-9]+)").find(record.outcome)
                ?.groupValues?.get(1)?.toInt()
            assertEquals("Gateway ID must match actual immediate OS readback", actual, recordedId)
            assertEquals(false, record.active)
            // An API acknowledgement is a precise instant in time. This test
            // never claims the device will keep that wallpaper later.
        } finally {
            database.close()
            image.delete()
        }
    }

    @Test fun decodableButChangedPixelsNeverReachAndroidWallpaperManager() = runBlocking {
        val system = WallpaperManager.getInstance(context)
        assumeTrue(system.isWallpaperSupported && system.isSetWallpaperAllowed)
        val original = testBitmapFile()
        val replacement = testBitmapFile(Color.rgb(214, 48, 104))
        val db = Room.inMemoryDatabaseBuilder(context, GlimpseDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.insertCollection(CollectionRecord("sha-rejection", "SHA rejection", 1L))
            val originalId = sha(original)
            dao.attach(PhotoRecord(
                id = originalId, sha256 = originalId, localPath = original.absolutePath,
                mime = "image/jpeg", width = 320, height = 560,
                byteLength = original.length(), importedAt = 1L
            ), "sha-rejection")
            val before = system.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            // Valid JPEG with different photographic bytes. BitmapFactory can
            // still decode this; filename and DB row have not changed.
            original.writeBytes(replacement.readBytes())
            assertTrue(original.length() > 0)
            assertTrue(originalId != sha(original))
            val result = WallpaperCommands(context, dao).applySelected(originalId)
            val after = system.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            assertEquals("Corrupted content must never be passed to wallpaper OS", before, after)
            assertTrue("Failure must be reported truthfully: " + result,
                result.contains("failed", ignoreCase = true))
            val rotation = requireNotNull(dao.readRotation())
            assertNull(rotation.pendingPhotoId)
            assertNull(rotation.lastAcknowledgedPhotoId)
            assertTrue(rotation.outcome.contains("Apply failed"))
        } finally {
            db.close()
            original.delete()
            replacement.delete()
        }
    }

    @Test fun exceptionAtOsBoundaryPreservesPendingAndNeverRetries() = runBlocking {
        val image = testBitmapFile()
        val database = Room.inMemoryDatabaseBuilder(
            context, GlimpseDatabase::class.java
        ).build()
        try {
            val dao = database.dao()
            dao.insertCollection(CollectionRecord("os-uncertain", "OS uncertain", 1L))
            val id = sha(image)
            dao.attach(PhotoRecord(
                id = id, sha256 = id, localPath = image.absolutePath,
                mime = "image/jpeg", width = 320, height = 560,
                byteLength = image.length(), importedAt = 1L
            ), "os-uncertain")
            val previousBag = "[\"prior-photo-in-bag\"]"
            dao.saveRotation(RotationRecord(
                collectionId = "os-uncertain", active = true,
                remainingJson = previousBag,
                nextEligibleAt = 987654L,
                outcome = "Rotation enabled"
            ))
            val invocations = java.util.concurrent.atomic.AtomicInteger(0)
            // This *simulates* the OS throwing at an irreversible boundary.
            // It does not assert whether Android actually changed wallpaper.
            val commands = WallpaperCommands(context, dao, setHomeWallpaper = {
                invocations.incrementAndGet()
                throw IllegalStateException("Simulated unknown OS outcome")
            })
            val outcome = commands.applySelected(id)
            assertTrue(outcome, outcome.contains("uncertain", ignoreCase = true))
            assertEquals(1, invocations.get())
            val pending = requireNotNull(dao.readRotation())
            assertEquals(id, pending.pendingPhotoId)
            assertEquals(false, pending.active)
            assertNull(pending.lastAcknowledgedPhotoId)
            assertEquals(previousBag, pending.remainingJson)
            assertEquals(987654L, pending.nextEligibleAt)
            assertTrue(pending.outcome.contains("uncertain"))
            val retry = commands.applySelected(id)
            assertTrue(retry, retry.contains("uncertain", ignoreCase = true))
            assertEquals("No implicit retry of an uncertain OS operation", 1, invocations.get())
            commands.acknowledgeUncertain()
            val resolved = requireNotNull(dao.readRotation())
            assertNull(resolved.pendingPhotoId)
            assertEquals(false, resolved.active)
            assertNull(resolved.lastAcknowledgedPhotoId)
            assertEquals(previousBag, resolved.remainingJson)
        } finally {
            database.close()
            image.delete()
        }
    }

    @Test fun pendingUncertainOperationBlocksApplyWithoutCallingOs() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            context, GlimpseDatabase::class.java
        ).build()
        val image = testBitmapFile()
        try {
            val dao = database.dao()
            val id = sha(image)
            dao.insertCollection(CollectionRecord("pending", "Pending", 1L))
            dao.attach(PhotoRecord(
                id = id, sha256 = id, localPath = image.absolutePath,
                mime = "image/jpeg", width = 320, height = 560,
                byteLength = image.length(), importedAt = 1L
            ), "pending")
            dao.saveRotation(RotationRecord(
                collectionId = "pending", pendingPhotoId = "prior-unresolved",
                active = false, outcome = "Prior attempt unresolved"
            ))
            val wallpaper = WallpaperManager.getInstance(context)
            val before = wallpaper.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val outcome = WallpaperCommands(context, dao).applySelected(id)
            val after = wallpaper.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            assertEquals("A pending operation must prevent any new OS mutation", before, after)
            assertTrue(outcome, outcome.contains("uncertain", ignoreCase = true))
            assertEquals("prior-unresolved", dao.readRotation()?.pendingPhotoId)
        } finally {
            database.close()
            image.delete()
        }
    }
}
