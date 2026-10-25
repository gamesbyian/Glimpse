package io.github.gamesbyian.glimpse.domain

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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground-only policy acceptance. Synthetic, locally generated JPEGs
 * and injected OS acknowledgements: absolutely no device wallpaper changes.
 * No WorkManager / Doze / background scheduling acceptance is inferred.
 */
@RunWith(AndroidJUnit4::class)
class ForegroundRotationDeviceTest {
    private lateinit var context: Context
    @Before fun emulatorOnly() {
        assumeTrue("Only disposable emulators", Build.HARDWARE.contains("ranchu", true)
            || Build.HARDWARE.contains("goldfish", true)
            || Build.FINGERPRINT.contains("generic", true))
        context = ApplicationProvider.getApplicationContext()
    }
    private fun fixture(seed: Int): File {
        val file = File.createTempFile("rotation-clock-", ".jpg", context.cacheDir)
        val image = Bitmap.createBitmap(64, 96, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(Color.rgb(seed, 63, 133))
            image.setPixel(5, seed % 96, Color.WHITE)
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.JPEG, 93, it)) }
        } finally {
            image.recycle()
        }
        return file
    }
    private fun sha(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private fun withPhotos(
        count: Int,
        block: suspend (GlimpseDatabase, List<String>) -> Unit
    ) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, GlimpseDatabase::class.java).build()
        val images = (0 until count).map { index -> fixture(38 + index * 35) }
        try {
            val dao = database.dao()
            dao.insertCollection(CollectionRecord("timing", "Timing", 1L))
            val ids = images.mapIndexed { index, image ->
                val id = sha(image)
                dao.attach(PhotoRecord(
                    id = id, sha256 = id, localPath = image.path,
                    mime = "image/jpeg", width = 64, height = 96,
                    byteLength = image.length(), importedAt = index.toLong()
                ), "timing")
                id
            }
            block(database, ids)
        } finally {
            database.close()
            images.forEach { it.delete() }
        }
    }

    private fun withTwoPhotos(block: suspend (GlimpseDatabase, List<String>) -> Unit) =
        withPhotos(2, block)

    @Test fun noApplyBeforeFiveMinutesAndOneApplyAtEligibility() = withTwoPhotos { db, ids ->
        var clock = 1_000_000L
        val count = AtomicInteger()
        val commands = WallpaperCommands(context, db.dao(), now = { clock },
            setHomeWallpaper = { count.incrementAndGet(); 101 + count.get() })
        assertTrue(commands.start("timing").contains("Rotation enabled"))
        assertTrue(requireNotNull(db.dao().readRotation()).outcome.contains("five minutes"))
        assertEquals(clock + 300_000L, db.dao().readRotation()?.nextEligibleAt)
        clock += 299_999L
        commands.pollWhileVisible()
        assertEquals(0, count.get())
        assertNull(db.dao().readRotation()?.lastAcknowledgedPhotoId)
        clock++
        commands.pollWhileVisible()
        assertEquals(1, count.get())
        val record = requireNotNull(db.dao().readRotation())
        assertTrue(record.active)
        assertTrue(record.lastAcknowledgedPhotoId in ids)
        assertEquals(clock + 300_000L, record.nextEligibleAt)
        commands.pollWhileVisible()
        assertEquals("Repeated same-tick poll must not set wallpaper again", 1, count.get())
    }

    @Test fun foregroundReturnAfterMissedHoursDoesNotReplayMissedIntervals() =
        withTwoPhotos { db, _ ->
            var clock = 3_000_000L
            val count = AtomicInteger()
            val commands = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { count.incrementAndGet(); 400 + count.get() })
            commands.start("timing")
            clock += 6L * 60 * 60 * 1000
            // Simulate one foreground poll after hours away; *not* six hours
            // of alarm delivery or proof of background scheduling.
            commands.pollWhileVisible()
            assertEquals(1, count.get())
            val next = requireNotNull(db.dao().readRotation())
            assertEquals(clock + 300_000L, next.nextEligibleAt)
            repeat(25) { commands.pollWhileVisible() }
            assertEquals(1, count.get())
            clock += 300_000L
            commands.pollWhileVisible()
            assertEquals(2, count.get())
        }

    @Test fun pauseAndPendingUncertainPreventAnyForegroundAutomaticMutation() =
        withTwoPhotos { db, ids ->
            var clock = 5_000_000L
            val count = AtomicInteger()
            val commands = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { count.incrementAndGet(); 900 + count.get() })
            commands.start("timing")
            commands.pause()
            clock += 600_000L
            commands.pollWhileVisible()
            assertEquals(0, count.get())
            // Independent unresolved record blocks both foreground poll and
            // explicit Change now, even when automatic rotation is otherwise eligible.
            val previous = requireNotNull(db.dao().readRotation())
            db.dao().saveRotation(previous.copy(active = true,
                pendingPhotoId = ids[0], nextEligibleAt = clock - 1))
            commands.pollWhileVisible()
            assertEquals(0, count.get())
            assertTrue(commands.changeNow().contains("uncertain"))
            assertEquals(0, count.get())
            assertEquals(ids[0], db.dao().readRotation()?.pendingPhotoId)
        }
    @Test fun onePhotoNeverRepeatsAtAnyFiveMinuteBoundary() =
        withPhotos(1) { db, ids ->
            var clock = 10_000_000L
            val setterCount = AtomicInteger()
            val commands = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { setterCount.incrementAndGet(); 1001 })
            commands.start("timing")
            clock += 300_000L
            commands.pollWhileVisible()
            assertEquals(1, setterCount.get())
            assertEquals(ids.single(), db.dao().readRotation()?.lastAcknowledgedPhotoId)
            repeat(12) {
                clock += 300_000L
                commands.pollWhileVisible()
                assertEquals("One-item collection must not reapply the same photo", 1, setterCount.get())
                assertTrue(requireNotNull(db.dao().readRotation()).active)
                assertEquals(clock + 300_000L, db.dao().readRotation()?.nextEligibleAt)
                assertNull(db.dao().readRotation()?.pendingPhotoId)
            }
        }

    @Test fun noDuplicatesInsideThreePhotoCycleOrAtBoundary() =
        withPhotos(3) { db, ids ->
            var clock = 12_000_000L
            val setterCount = AtomicInteger()
            val commands = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { setterCount.incrementAndGet(); 1100 + setterCount.get() })
            commands.start("timing")
            val firstCycle = mutableListOf<String>()
            repeat(3) {
                clock += 300_000L
                commands.pollWhileVisible()
                assertEquals(it + 1, setterCount.get())
                firstCycle += requireNotNull(db.dao().readRotation()?.lastAcknowledgedPhotoId)
                assertNull(db.dao().readRotation()?.pendingPhotoId)
            }
            assertEquals("A full three-photo cycle visits each ID once", ids.toSet(),
                firstCycle.toSet())
            clock += 300_000L
            commands.pollWhileVisible()
            val boundary = requireNotNull(db.dao().readRotation()?.lastAcknowledgedPhotoId)
            assertTrue("Cycle boundary must not repeat immediately",
                boundary != firstCycle.last())
            assertEquals(4, setterCount.get())
        }

    @Test fun gatewayRecreationPreservesRoomDeadlineAndShuffleCursor() =
        withPhotos(3) { db, _ ->
            var clock = 14_000_000L
            val setterCount = AtomicInteger()
            fun gateway() = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { setterCount.incrementAndGet(); 1200 + setterCount.get() })
            val starter = gateway()
            starter.start("timing")
            clock += 300_000L
            starter.pollWhileVisible()
            assertEquals(1, setterCount.get())
            val first = requireNotNull(db.dao().readRotation())
            // Recreate the gateway as an app Activity or ViewModel might; this
            // is NOT evidence of Android process-death or Doze delivery.
            val second = gateway()
            clock += 299_999L
            second.pollWhileVisible()
            assertEquals(1, setterCount.get())
            assertEquals(first.remainingJson, db.dao().readRotation()?.remainingJson)
            clock++
            second.pollWhileVisible()
            assertEquals(2, setterCount.get())
            val record = requireNotNull(db.dao().readRotation())
            assertTrue(record.lastAcknowledgedPhotoId != first.lastAcknowledgedPhotoId)
            assertEquals(clock + 300_000L, record.nextEligibleAt)
        }

    @Test fun emptyCollectionNeverStartsOrCallsAndroidSetter() =
        withPhotos(0) { db, _ ->
            var clock = 16_000_000L
            val setterCount = AtomicInteger()
            val commands = WallpaperCommands(context, db.dao(), now = { clock },
                setHomeWallpaper = { setterCount.incrementAndGet(); 1300 })
            assertTrue(commands.start("timing").contains("Import at least one photo"))
            clock += 20L * 300_000L
            repeat(10) { commands.pollWhileVisible() }
            assertTrue(commands.changeNow().contains("Start a collection"))
            assertNull(db.dao().readRotation())
            assertEquals(0, setterCount.get())
        }

}
