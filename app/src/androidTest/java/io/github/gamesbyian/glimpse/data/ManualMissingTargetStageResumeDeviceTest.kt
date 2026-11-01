package io.github.gamesbyian.glimpse.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android image decoding, private filesystem and real in-memory Room. */
@RunWith(AndroidJUnit4::class)
class ManualMissingTargetStageResumeDeviceTest {
    private lateinit var root: File
    private lateinit var db: GlimpseDatabase
    private lateinit var dao: GlimpseDao
    private lateinit var stage: File
    private lateinit var target: File
    private lateinit var bytes: ByteArray
    private lateinit var sha: String
    private lateinit var journal: PrivatePublicationReceiptJournal
    private lateinit var receipt: PrivatePublicationReceiptJournal.Handle
    private val collection = "test-recovery-collection"

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "glimpse-recovery-test-${UUID.randomUUID()}")
        assertTrue(root.mkdir())
        db = Room.inMemoryDatabaseBuilder(context, GlimpseDatabase::class.java).build()
        dao = db.dao()
        stage = File.createTempFile("import-", ".partial", root)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.rgb(12, 34, 56))
            FileOutputStream(stage).use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.fd.sync()
            }
        } finally {
            bitmap.recycle()
        }
        bytes = stage.readBytes()
        sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        target = File(root, "$sha.png")
        journal = PrivatePublicationReceiptJournal(root)
        receipt = journal.begin(stage, target, sha, bytes.size.toLong(), collection)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::root.isInitialized && root.name.startsWith("glimpse-recovery-test-") &&
            root.canonicalFile.parentFile == root.parentFile.canonicalFile
        ) root.deleteRecursively()
    }

    @Test fun explicitConsentIsRequiredEvenForCompletelyVerifiedStage() = runBlocking {
        dao.insertCollection(CollectionRecord(collection, "Synthetic", 1L))
        assertTrue(runCatching {
            ManualMissingTargetStageResume(root, dao)
                .resumeOnlyAfterExplicitConfirmation(userConfirmed = false)
        }.isFailure)
        assertFalse(target.exists())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
        assertNull(dao.findPhoto(sha))
    }

    @Test fun confirmedResumePublishesNewDigestAndAttachesExactlyOnce() = runBlocking {
        dao.insertCollection(CollectionRecord(collection, "Synthetic", 1L))
        val resumed = ManualMissingTargetStageResume(root, dao)
            .resumeOnlyAfterExplicitConfirmation(userConfirmed = true)
        assertEquals(1, resumed.possibleCandidates)
        assertEquals(1, resumed.attached)
        assertEquals(0, resumed.refused)
        assertFalse(resumed.unavailable)
        assertArrayEquals(bytes, target.readBytes())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
        val photo = dao.findPhoto(sha)!!
        assertEquals(target.canonicalPath, File(photo.localPath).canonicalPath)
        assertEquals("image/png", photo.mime)
        assertEquals(4, photo.width)
        assertEquals(4, photo.height)
        assertEquals(1, dao.membershipCount(sha))
        assertEquals(collection, dao.findMembership(collection, sha)?.collectionId)
        assertNull(dao.readRotation())
    }

    @Test fun existingEquivalentPrivatePathSpellingKeepsRegisteredRowIntact() = runBlocking {
        dao.insertCollection(CollectionRecord(collection, "Synthetic", 1L))
        // Android can expose one private directory as /data/user/0 and
        // /data/data. The stage scanner uses the canonical parent, whereas
        // an older Room registration may retain the lexical app data path.
        val already = PhotoRecord(
            id = sha, sha256 = sha, localPath = target.absolutePath,
            mime = "image/png", width = 4, height = 4,
            byteLength = bytes.size.toLong(), importedAt = 1L
        )
        dao.insertPhoto(already)
        val resumed = ManualMissingTargetStageResume(root, dao)
            .resumeOnlyAfterExplicitConfirmation(userConfirmed = true)
        assertEquals(1, resumed.attached)
        assertEquals(0, resumed.refused)
        assertEquals(already.localPath, dao.findPhoto(sha)?.localPath)
        assertEquals(1, dao.membershipCount(sha))
        assertArrayEquals(bytes, target.readBytes())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
    }

    @Test fun existingForeignDigestIsNeverTruncatedOrUsedForRoomAdmission() = runBlocking {
        dao.insertCollection(CollectionRecord(collection, "Synthetic", 1L))
        val foreign = byteArrayOf(9, 8, 7, 6)
        target.writeBytes(foreign)
        val resumed = ManualMissingTargetStageResume(root, dao)
            .resumeOnlyAfterExplicitConfirmation(userConfirmed = true)
        assertEquals(0, resumed.possibleCandidates)
        assertEquals(0, resumed.attached)
        assertArrayEquals(foreign, target.readBytes())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
        assertNull(dao.findPhoto(sha))
    }

    @Test fun deletedDestinationCollectionRefusesBeforePublishingFiles() = runBlocking {
        val resumed = ManualMissingTargetStageResume(root, dao)
            .resumeOnlyAfterExplicitConfirmation(userConfirmed = true)
        assertEquals(1, resumed.possibleCandidates)
        assertEquals(0, resumed.attached)
        assertEquals(1, resumed.refused)
        assertFalse(target.exists())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
        assertNull(dao.findPhoto(sha))
    }

    @Test fun tamperedReceiptCannotDriveNewFileOrRoomMutation() = runBlocking {
        dao.insertCollection(CollectionRecord(collection, "Synthetic", 1L))
        receipt.file.appendText("\tforeign")
        val resumed = ManualMissingTargetStageResume(root, dao)
            .resumeOnlyAfterExplicitConfirmation(userConfirmed = true)
        assertEquals(0, resumed.possibleCandidates)
        assertEquals(0, resumed.attached)
        assertFalse(target.exists())
        assertArrayEquals(bytes, stage.readBytes())
        assertTrue(receipt.file.exists())
        assertNull(dao.findPhoto(sha))
    }
}
