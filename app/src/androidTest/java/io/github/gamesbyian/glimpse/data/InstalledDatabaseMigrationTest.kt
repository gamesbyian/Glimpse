package io.github.gamesbyian.glimpse.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Upgrade a POPULATED version-1 database with the exact table/index layout
 * recorded in app/schemas/.../1.json. Never operate on the app's real database.
 * The Room v2 open validates the target schema after MIGRATION_1_2.
 */
@RunWith(AndroidJUnit4::class)
class InstalledDatabaseMigrationTest {
    @Test fun version1To2PreservesOriginalsMembershipsAndUncertainWallpaper() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "glimpse-migration-test-" + UUID.randomUUID() + ".db"
        val sha = "b".repeat(64)
        val collection = "original-collection"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                // These are the literal schema-v1 createSql definitions
                // (TABLE_NAME expanded), including the unique SHA index.
                db.execSQL("CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS photos (id TEXT NOT NULL, sha256 TEXT NOT NULL, localPath TEXT NOT NULL, mime TEXT NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, byteLength INTEGER NOT NULL, importedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_photos_sha256 ON photos (sha256)")
                db.execSQL("CREATE TABLE IF NOT EXISTS memberships (collectionId TEXT NOT NULL, photoId TEXT NOT NULL, PRIMARY KEY(collectionId, photoId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS rotation (id INTEGER NOT NULL, collectionId TEXT, active INTEGER NOT NULL, remainingJson TEXT NOT NULL, lastAcknowledgedPhotoId TEXT, lastAcknowledgedAt INTEGER, lastAttemptAt INTEGER, nextEligibleAt INTEGER, pendingPhotoId TEXT, outcome TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '004d495b61f9baf9fd48e3820ce90b6f')")
                db.execSQL("INSERT INTO collections VALUES(?,?,?)", arrayOf(collection, "Saved photos", 100L))
                db.execSQL(
                    "INSERT INTO photos VALUES(?,?,?,?,?,?,?,?)",
                    arrayOf(sha, sha, "/fixture/immutable/" + sha + ".jpg",
                        "image/jpeg", 64, 64, 1000L, 101L)
                )
                db.execSQL("INSERT INTO memberships VALUES(?,?)", arrayOf(collection, sha))
                // A pending wallpaper operation must remain UNKNOWN, not be
                // discarded or reattempted by a storage-schema migration.
                db.execSQL(
                    "INSERT INTO rotation VALUES(1,?,?,?,?,?,?,?,?,?)",
                    arrayOf(collection, 0, "[]", sha, 101L, 100L, 400L,
                        sha, "Wallpaper outcome uncertain")
                )
                db.version = 1
            }
            val upgraded = Room.databaseBuilder(
                context, GlimpseDatabase::class.java, name
            ).addMigrations(GlimpseDatabase.MIGRATION_1_2).build()
            try {
                val dao = upgraded.dao()
                val original = dao.findPhoto(sha)
                assertNotNull(original)
                assertEquals(sha, original!!.sha256)
                assertEquals(1000L, original.byteLength)
                assertNotNull(dao.findMembership(collection, sha))
                assertEquals(1, dao.membershipCount(sha))
                assertEquals("Saved photos", dao.findCollection(collection)?.title)
                val rotation = dao.readRotation()
                assertNotNull(rotation)
                assertEquals(collection, rotation!!.collectionId)
                assertEquals(sha, rotation.pendingPhotoId)
                assertEquals(sha, rotation.lastAcknowledgedPhotoId)
                assertEquals("Wallpaper outcome uncertain", rotation.outcome)
                assertNull(dao.findFavorite(sha))
                assertEquals(true, dao.setFavorite(sha, true))
                assertNotNull(dao.findFavorite(sha))
                assertTrue(upgraded.openHelper.readableDatabase.version == 2)
            } finally {
                upgraded.close()
            }
            // Validate a second Room open after successful migration, too.
            val reopened = Room.databaseBuilder(
                context, GlimpseDatabase::class.java, name
            ).addMigrations(GlimpseDatabase.MIGRATION_1_2).build()
            try {
                assertNotNull(reopened.dao().findPhoto(sha))
                assertEquals(sha, reopened.dao().readRotation()?.pendingPhotoId)
                assertNotNull(reopened.dao().findFavorite(sha))
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
