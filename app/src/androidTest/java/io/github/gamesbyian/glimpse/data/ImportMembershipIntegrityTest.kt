package io.github.gamesbyian.glimpse.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Room transactions, not mocked DAO responses or source-text tests.
 * Only private ephemeral in-memory data, never the user's Glimpse database.
 */
@RunWith(AndroidJUnit4::class)
class ImportMembershipIntegrityTest {
    private lateinit var database: GlimpseDatabase
    private lateinit var dao: GlimpseDao

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, GlimpseDatabase::class.java
        ).build()
        dao = database.dao()
    }

    @After fun closeDatabase() {
        database.close()
    }

    private fun fixturePhoto(): PhotoRecord {
        val sha = "a".repeat(64)
        return PhotoRecord(
            id = sha, sha256 = sha, localPath = "/fixture/never-opened/" + sha + ".jpg",
            mime = "image/jpeg", width = 12, height = 12, byteLength = 32,
            importedAt = 1700000000000L
        )
    }

    @Test fun vanishedCollectionCannotCreatePhotoOrMembership() = runBlocking {
        val id = "removed-while-provider-open"
        dao.insertCollection(CollectionRecord(id, "Temporary", 1L))
        assertEquals(1, dao.dropCollectionRecord(id))
        val outcome = dao.attachIfCollectionExists(fixturePhoto(), id)
        assertNull(outcome)
        assertNull(dao.findPhoto(fixturePhoto().id))
        assertNull(dao.findMembership(id, fixturePhoto().id))
        assertFalse(dao.collectionExists(id))
    }

    @Test fun stillExistingCollectionAttachesExactlyOnce() = runBlocking {
        val id = "valid-source"
        val photo = fixturePhoto()
        dao.insertCollection(CollectionRecord(id, "Valid", 1L))
        assertEquals(true, dao.attachIfCollectionExists(photo, id))
        assertEquals(false, dao.attachIfCollectionExists(photo, id))
        assertNotNull(dao.findPhoto(photo.id))
        assertNotNull(dao.findMembership(id, photo.id))
        assertEquals(1, dao.membershipCount(photo.id))
    }

    @Test fun recoveredOriginalAttachmentRejectsConflictingPhotoInOneTransaction() = runBlocking {
        val source = "source"
        val recovery = "recovery"
        val original = fixturePhoto()
        dao.insertCollection(CollectionRecord(source, "Original", 1L))
        dao.insertCollection(CollectionRecord(recovery, "Recovered", 2L))
        assertEquals(true, dao.attachIfCollectionExists(original, source))
        val conflicting = original.copy(localPath = "/fixture/different-owner/photo.jpg")
        assertNull(dao.attachRecoveredOriginalIfMatching(conflicting, recovery))
        assertNull(dao.findMembership(recovery, original.id))
        assertEquals(original.localPath, dao.findPhoto(original.id)?.localPath)
        assertEquals(1, dao.membershipCount(original.id))
        // Matching bytes/metadata may deduplicate without replacing the row.
        assertEquals(true, dao.attachRecoveredOriginalIfMatching(original, recovery))
        assertEquals(false, dao.attachRecoveredOriginalIfMatching(original, recovery))
        assertEquals(2, dao.membershipCount(original.id))
        assertEquals(original.localPath, dao.findPhoto(original.id)?.localPath)
    }

    @Test fun vanishedRecoveryCollectionNeverAdmitsPhotoRecord() = runBlocking {
        val recovered = fixturePhoto()
        assertNull(dao.attachRecoveredOriginalIfMatching(recovered, "absent"))
        assertNull(dao.findPhoto(recovered.id))
        assertNull(dao.findMembership("absent", recovered.id))
    }

    @Test fun deletingRedundantCollectionCannotStrandAnOriginal() = runBlocking {
        val source = "source"
        val second = "second"
        val photo = fixturePhoto()
        dao.insertCollection(CollectionRecord(source, "One", 1L))
        dao.insertCollection(CollectionRecord(second, "Two", 2L))
        assertEquals(true, dao.attachIfCollectionExists(photo, source))
        assertEquals(1, dao.copyMembership(source, second, photo.id))
        assertEquals(2, dao.membershipCount(photo.id))
        assertTrue(dao.detachOnlyIfElsewhere(source, photo.id))
        assertEquals(1, dao.membershipCount(photo.id))
        assertNotNull(dao.findPhoto(photo.id))
        assertNotNull(dao.findMembership(second, photo.id))
        assertFalse(dao.detachOnlyIfElsewhere(second, photo.id))
    }
}
