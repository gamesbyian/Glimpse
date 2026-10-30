package io.github.gamesbyian.glimpse.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android Room v2 SQLite tests, with no production database access. */
@RunWith(AndroidJUnit4::class)
class LibraryRelationshipAuditTest {
    private lateinit var db: GlimpseDatabase
    private lateinit var dao: GlimpseDao
    private val sha = "f".repeat(64)

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GlimpseDatabase::class.java).build()
        dao = db.dao()
    }

    @After fun teardown() { db.close() }

    @Test fun validLinksHaveZeroProblemsAndKeepUserData() = runBlocking {
        dao.insertCollection(CollectionRecord("safe", "Saved", 1L))
        val photo = PhotoRecord(
            sha, sha, "/fixture/never-read/$sha.jpg", "image/jpeg",
            10, 10, 128, 1L
        )
        assertEquals(true, dao.attachIfCollectionExists(photo, "safe"))
        assertEquals(true, dao.setFavorite(sha, true))
        dao.saveRotation(RotationRecord(
            collectionId = "safe", lastAcknowledgedPhotoId = sha
        ))
        val audit = dao.auditLibraryRelationships()
        assertEquals(0, audit.issueCount)
        assertNotNull(dao.findPhoto(sha))
        assertNotNull(dao.findFavorite(sha))
        assertNotNull(dao.findMembership("safe", sha))
    }

    @Test fun oldOrphanLinksAreCountedButNeverDeletedOrSilentlyRepaired() = runBlocking {
        // Room v2 intentionally has no FKs yet. This reproduces historical
        // interrupted/imported metadata without ever touching physical files.
        dao.insertMembership(MembershipRecord("lost-collection", "lost-photo"))
        dao.addFavorite(PhotoFavoriteRecord("lost-photo", 1L))
        dao.saveRotation(RotationRecord(
            collectionId = "lost-collection",
            pendingPhotoId = "lost-photo",
            lastAcknowledgedPhotoId = "lost-photo",
            outcome = "Wallpaper outcome uncertain"
        ))
        val audit = dao.auditLibraryRelationships()
        assertEquals(1, audit.membershipsWithoutPhoto)
        assertEquals(1, audit.membershipsWithoutCollection)
        assertEquals(1, audit.favoritesWithoutPhoto)
        assertTrue(audit.rotationCollectionMissing)
        assertTrue(audit.pendingPhotoMissing)
        assertTrue(audit.acknowledgedPhotoMissing)
        assertEquals(6, audit.issueCount)
        // A non-destructive audit cannot decide that a historical file or
        // unresolved OS request should be discarded.
        assertNotNull(dao.findMembership("lost-collection", "lost-photo"))
        assertNotNull(dao.findFavorite("lost-photo"))
        assertEquals("lost-photo", dao.readRotation()?.pendingPhotoId)
        assertFalse(dao.collectionExists("lost-collection"))
    }

    @Test fun missingPhotoDoesNotFlagValidRotationSource() = runBlocking {
        dao.insertCollection(CollectionRecord("existing", "Valid", 1L))
        dao.saveRotation(RotationRecord(
            collectionId = "existing", pendingPhotoId = "not-imported"
        ))
        val audit = dao.auditLibraryRelationships()
        assertFalse(audit.rotationCollectionMissing)
        assertTrue(audit.pendingPhotoMissing)
        assertFalse(audit.acknowledgedPhotoMissing)
        assertEquals(1, audit.issueCount)
    }
}
