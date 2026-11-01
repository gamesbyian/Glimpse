package io.github.gamesbyian.glimpse

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EditedPhotoShareCacheTest {
    @get:Rule val temp = TemporaryFolder()
    private val now = 2_000_000_000_000L
    private val age = 26L * 60L * 60L * 1000L

    private fun expired(folder: File, name: String, bytes: ByteArray): File {
        val file = File(folder, name).apply { writeBytes(bytes) }
        assertTrue(file.setLastModified(now - age))
        return file
    }

    @Test fun cleanupRemovesOnlyExpiredGeneratedShareFiles() {
        val root = temp.newFolder("shared-edits")
        val oldShare = expired(root, "glimpse-edited-12345678.png", byteArrayOf(1, 2))
        val oldStage = expired(root, "glimpse-87654321.partial", byteArrayOf(3))
        val unowned = expired(root, "user-copy.png", byteArrayOf(4, 5))
        val arbitrary = expired(root, "glimpse-edited-short.png", byteArrayOf(6))
        val fresh = File.createTempFile("glimpse-edited-", ".png", root).apply {
            writeBytes(byteArrayOf(7, 8))
            assertTrue(setLastModified(now - 60_000L))
        }
        assertEquals(2, pruneOwnedEditedShareCache(root, now))
        assertFalse(oldShare.exists())
        assertFalse(oldStage.exists())
        assertTrue(unowned.exists())
        assertTrue(arbitrary.exists())
        assertTrue(fresh.exists())
        assertArrayEquals(byteArrayOf(4, 5), unowned.readBytes())
    }

    @Test fun symlinkAndOutsideFileAreNotFollowedOrDeleted() {
        val root = temp.newFolder("shared-edits")
        val outside = temp.newFolder("outside")
        val actual = expired(outside, "important.png", byteArrayOf(1, 2, 3))
        val link = File(root, "glimpse-edited-link987654.png")
        Files.createSymbolicLink(link.toPath(), actual.toPath())
        assertEquals(0, pruneOwnedEditedShareCache(root, now))
        assertTrue(Files.isSymbolicLink(link.toPath()))
        assertArrayEquals(byteArrayOf(1, 2, 3), actual.readBytes())
    }

    @Test fun aliasRootIsRejectedWithoutAnyCleanup() {
        val outside = temp.newFolder("actual")
        val owned = expired(outside, "glimpse-edited-12345678.png", byteArrayOf(9))
        val alias = File(temp.root, "alias")
        Files.createSymbolicLink(alias.toPath(), outside.toPath())
        assertEquals(0, pruneOwnedEditedShareCache(alias, now))
        assertArrayEquals(byteArrayOf(9), owned.readBytes())
    }
}
