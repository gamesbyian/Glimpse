package io.github.gamesbyian.glimpse.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.content.Intent
import java.security.MessageDigest
import java.util.UUID

/**
 * Checks the *actual* ContentResolver -> private-files -> Room import pipeline
 * on a disposable Android emulator, not a fake DAO, SVG mockup or JVM-only
 * model. File URI is a controlled stand-in for an Android picker provider:
 * successful offline snapshots and source mutation/deletion are tested,
 * but temporary content:// grant revocation is NOT established here.
 *
 * No real user data, network provider or WallpaperManager call is involved.
 */
@RunWith(AndroidJUnit4::class)
class PhotoImporterDeviceTest {
    private lateinit var context: Context

    @Before fun emulatorOnly() {
        val emulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)
        assumeTrue("Fixture import tests are only permitted on disposable emulators", emulator)
        context = ApplicationProvider.getApplicationContext()
    }

    private fun sourceFixture(seed: Int): File {
        val file = File.createTempFile("glimpse-c-source-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(64, 88, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb((seed * 31) % 255, (seed * 71) % 255, 132))
            bitmap.setPixel(seed % 64, seed % 88, Color.WHITE)
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 93, output))
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun sha256(file: File): String {
        val bytes = file.readBytes()
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private fun withDatabase(block: suspend (GlimpseDao) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            context,
            GlimpseDatabase::class.java
        ).build()
        try {
            block(db.dao())
        } finally {
            db.close()
        }
    }

    @Test fun verifiedOfflineCopySurvivesSourceDeletion() = withDatabase { dao ->
        val source = sourceFixture(21)
        try {
            dao.insertCollection(CollectionRecord("one", "One", 1L))
            val originalSha = sha256(source)
            val originalBytes = source.readBytes()
            val imported = PhotoImporter(context, dao).import(
                listOf(Uri.fromFile(source)), "one"
            )
            assertEquals(1, imported.selected)
            assertEquals(1, imported.imported)
            assertEquals(0, imported.duplicates)
            assertEquals(0, imported.failures)
            val rows = dao.photosIn("one")
            assertEquals(1, rows.size)
            val row = rows.single()
            assertEquals(originalSha, row.id)
            assertEquals(originalSha, row.sha256)
            assertEquals(64, row.width)
            assertEquals(88, row.height)
            assertEquals("image/jpeg", row.mime)
            val offlineFile = File(row.localPath)
            assertTrue(offlineFile.isFile)
            assertArrayEquals(originalBytes, offlineFile.readBytes())
            assertTrue("Fixture source should be deletable independently", source.delete())
            assertFalse(source.exists())
            assertEquals(originalSha, sha256(offlineFile))
            assertEquals(row.id, dao.findPhoto(row.id)?.id)
        } finally {
            source.delete()
        }
    }

    @Test fun duplicateBytesDeduplicateButMembershipIsCollectionSpecific() =
        withDatabase { dao ->
            val source = sourceFixture(32)
            try {
                dao.insertCollection(CollectionRecord("a", "A", 1L))
                dao.insertCollection(CollectionRecord("b", "B", 2L))
                val importer = PhotoImporter(context, dao)
                val twice = importer.import(
                    listOf(Uri.fromFile(source), Uri.fromFile(source)), "a"
                )
                assertEquals(2, twice.selected)
                assertEquals(1, twice.imported)
                assertEquals(1, twice.duplicates)
                assertEquals(0, twice.failures)
                val sameIntoNewCollection = importer.import(
                    listOf(Uri.fromFile(source)), "b"
                )
                assertEquals(1, sameIntoNewCollection.imported)
                assertEquals(0, sameIntoNewCollection.duplicates)
                val a = dao.photosIn("a").single()
                val b = dao.photosIn("b").single()
                assertEquals(a.id, b.id)
                assertEquals(a.localPath, b.localPath)
                assertTrue(File(a.localPath).exists())
                assertEquals(a.id, dao.findMembership("a", a.id)?.photoId)
                assertEquals(a.id, dao.findMembership("b", a.id)?.photoId)
            } finally {
                source.delete()
            }
        }

    @Test fun invalidInputDoesNotDeleteSourceOrPersistPhotos() = withDatabase { dao ->
        val corrupt = File.createTempFile("glimpse-c-invalid-", ".jpg", context.cacheDir)
        val absent = File(context.cacheDir, "absent-c-" + UUID.randomUUID())
        try {
            corrupt.writeText("not a decodable JPEG")
            val unchanged = corrupt.readBytes()
            dao.insertCollection(CollectionRecord("fail", "Fail", 1L))
            val result = PhotoImporter(context, dao).import(
                listOf(Uri.fromFile(corrupt), Uri.fromFile(absent)), "fail"
            )
            assertEquals(2, result.selected)
            assertEquals(0, result.imported)
            assertEquals(0, result.duplicates)
            assertEquals(2, result.failures)
            assertTrue(corrupt.exists())
            assertArrayEquals(unchanged, corrupt.readBytes())
            assertTrue(dao.photosIn("fail").isEmpty())
            assertFalse(absent.exists())
        } finally {
            corrupt.delete()
        }
    }

    @Test fun controlledContentProviderGrantCanExpireAfterOfflineImport() =
        withDatabase { dao ->
            val testPackage = InstrumentationRegistry.getInstrumentation().context
            val transientSource = sourceFixture(91)
            val externalFixture = File(
                testPackage.cacheDir, "fixture-" + UUID.randomUUID() + ".jpg"
            )
            try {
                transientSource.copyTo(externalFixture, overwrite = true)
                val exact = externalFixture.readBytes()
                val expectedSha = sha256(externalFixture)
                val uri = Uri.parse(
                    "content://io.github.gamesbyian.glimpse.test.ephemeral/" +
                        externalFixture.name
                )
                dao.insertCollection(CollectionRecord("granted", "Granted", 1L))
                testPackage.grantUriPermission(
                    context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val accepted = PhotoImporter(context, dao).import(listOf(uri), "granted")
                assertEquals(1, accepted.imported)
                assertEquals(0, accepted.failures)
                val saved = dao.photosIn("granted").single()
                assertEquals(expectedSha, saved.id)
                val offline = File(saved.localPath)
                assertArrayEquals(exact, offline.readBytes())
                // This is a private test provider, not Google's Photo Picker.
                // Its explicit grant is revoked before retry, and the source
                // remains in place so rejection measures access, not deletion.
                testPackage.revokeUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val denied = PhotoImporter(context, dao).import(listOf(uri), "granted")
                assertEquals(0, denied.imported)
                assertEquals(1, denied.failures)
                assertArrayEquals(exact, offline.readBytes())
                assertEquals(expectedSha, sha256(offline))
                assertEquals(saved.id, dao.photosIn("granted").single().id)
            } finally {
                testPackage.revokeUriPermission(
                    Uri.parse("content://io.github.gamesbyian.glimpse.test.ephemeral/" +
                        externalFixture.name),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                transientSource.delete()
                externalFixture.delete()
            }
        }

    @Test fun corruptedPrivateCopyIsAtomicallyRestoredBeforeReusingHashId() =
        withDatabase { dao ->
            val source = sourceFixture(55)
            try {
                dao.insertCollection(CollectionRecord("repair", "Repair", 1L))
                val importer = PhotoImporter(context, dao)
                val originalBytes = source.readBytes()
                val sha = sha256(source)
                assertEquals(1, importer.import(listOf(Uri.fromFile(source)), "repair").imported)
                val photo = dao.photosIn("repair").single()
                val offline = File(photo.localPath)
                assertEquals(sha, sha256(offline))
                offline.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) // synthetic fixture only
                assertNotEquals(sha, sha256(offline))
                val retry = importer.import(listOf(Uri.fromFile(source)), "repair")
                assertEquals(0, retry.imported)
                assertEquals(1, retry.duplicates) // membership was already present
                assertEquals(0, retry.failures)
                assertArrayEquals(originalBytes, offline.readBytes())
                assertEquals(sha, sha256(offline))
                assertEquals(1, dao.photosIn("repair").size)
            } finally {
                source.delete()
            }
        }

    @Test fun sameUriWithMutatedPixelsCreatesDifferentImmutableIdentity() =
        withDatabase { dao ->
            val source = sourceFixture(43)
            val other = sourceFixture(79)
            try {
                dao.insertCollection(CollectionRecord("changed", "Changed", 1L))
                val importer = PhotoImporter(context, dao)
                val originalSha = sha256(source)
                assertEquals(1, importer.import(listOf(Uri.fromFile(source)), "changed").imported)
                other.copyTo(source, overwrite = true)
                val editedSha = sha256(source)
                assertNotEquals(originalSha, editedSha)
                assertEquals(1, importer.import(listOf(Uri.fromFile(source)), "changed").imported)
                val rows = dao.photosIn("changed")
                assertEquals(2, rows.size)
                assertEquals(setOf(originalSha, editedSha), rows.map { it.id }.toSet())
                assertEquals(originalSha, sha256(File(rows.single { it.id == originalSha }.localPath)))
                assertEquals(editedSha, sha256(File(rows.single { it.id == editedSha }.localPath)))
            } finally {
                source.delete()
                other.delete()
            }
        }
}
