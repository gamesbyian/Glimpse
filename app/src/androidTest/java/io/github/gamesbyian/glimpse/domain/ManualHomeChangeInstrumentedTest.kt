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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/** Real Room and actual Android Home wallpaper gateway on disposable emulator.
 * No mocked acknowledgment, no personal photos, never execute on a handset.
 */
@RunWith(AndroidJUnit4::class)
class ManualHomeChangeInstrumentedTest {
    private lateinit var context: Context
    private lateinit var db: GlimpseDatabase
    private var generated: File? = null

    @Before fun setup() {
        val emulator = Build.HARDWARE.contains("ranchu", true) ||
            Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.contains("generic", true) ||
            Build.MODEL.contains("sdk_gphone", true)
        assumeTrue("Refuse real wallpaper mutation on a personal device", emulator)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GlimpseDatabase::class.java).build()
    }

    @After fun cleanup() {
        if (::db.isInitialized) db.close()
        if (::context.isInitialized) {
            val root = File(context.filesDir, "originals").canonicalFile
            generated?.let { file ->
                // Only delete the exactly generated disposable file under our
                // private, non-symlink originals root. Never touch other media.
                if (file.parentFile?.canonicalFile == root &&
                    file.name.matches(Regex("[0-9a-f]{64}\\.png"))
                ) file.delete()
            }
        }
    }

    @Test fun noSourceAndUncertainPendingNeverClaimSuccess() = runBlocking {
        val command = WallpaperCommands(context, db.dao())
        assertEquals(HomeChangeStatus.NOT_APPLIED, command.changeNowOutcome().status)
        db.dao().saveRotation(RotationRecord(
            collectionId = "unavailable",
            active = false,
            pendingPhotoId = "reserved-photo",
            outcome = "Uncertain operation"
        ))
        val before = db.dao().readRotation()
        assertEquals(HomeChangeStatus.UNCERTAIN, command.changeNowOutcome().status)
        assertEquals(before, db.dao().readRotation())
    }

    @Test fun realAndroidAcknowledgmentIsTheOnlySuccessfulManualChange() = runBlocking {
        val source = "synthetic-emulator-only"
        db.dao().insertCollection(CollectionRecord(source, "Synthetic", 1L))
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        try {
            bitmap.eraseColor(Color.rgb(53, 109, 187))
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        } finally {
            bitmap.recycle()
        }
        val bytes = output.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val root = File(context.filesDir, "originals").apply { mkdirs() }
        val file = File(root, "$digest.png")
        require(!file.exists()) { "Disposable fixture unexpectedly already exists" }
        file.writeBytes(bytes)
        generated = file
        val photo = PhotoRecord(
            id = digest, sha256 = digest, localPath = file.absolutePath,
            mime = "image/png", width = 16, height = 16,
            byteLength = bytes.size.toLong(), importedAt = 2L
        )
        assertEquals(true, db.dao().attachIfCollectionExists(photo, source))
        val command = WallpaperCommands(context, db.dao())
        assertTrue(command.start(source).contains("Started"))
        val result = command.changeNowOutcome()
        assertEquals(HomeChangeStatus.ACKNOWLEDGED, result.status)
        val rotation = db.dao().readRotation()
        assertNotNull(rotation)
        assertNull(rotation!!.pendingPhotoId)
        assertEquals(digest, rotation.lastAcknowledgedPhotoId)
        assertTrue(rotation.lastAcknowledgedAt != null)
        assertTrue(rotation.outcome.contains("acknowledged"))
    }
}
