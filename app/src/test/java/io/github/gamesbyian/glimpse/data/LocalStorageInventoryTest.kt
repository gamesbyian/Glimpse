package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class LocalStorageInventoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun missingDirectoryReportsEmptyNotAnError() {
        assertEquals(
            LocalStorageInventory(0, 0L, 0, 0L),
            inspectLocalStorage(File(temporary.root, "not-yet-created"))
        )
    }

    @Test fun countsOnlyShaNamedOriginalsAndImporterStaging() {
        val folder = temporary.newFolder("originals")
        fun add(name: String, length: Int) =
            File(folder, name).apply { writeBytes(ByteArray(length)) }
        add("a".repeat(64) + ".jpg", 10)
        add("b".repeat(64) + ".png", 20)
        add("c".repeat(64) + ".webp", 30)
        add("import-work.partial", 7)
        add("import-done.jpg", 100)
        add("not-a-digest.jpg", 100)
        add("D".repeat(64) + ".jpg", 100)
        File(folder, "f".repeat(64) + ".jpg").mkdir()

        assertEquals(
            LocalStorageInventory(3, 60L, 1, 7L),
            inspectLocalStorage(folder)
        )
    }

    @Test fun neverFollowsSymlinksOrCountsOutsideData() {
        val folder = temporary.newFolder("photos")
        val outside = temporary.newFile("outside").apply { writeBytes(ByteArray(52)) }
        val alias = File(folder, "a".repeat(64) + ".jpg")
        Files.createSymbolicLink(alias.toPath(), outside.toPath())
        assertEquals(
            LocalStorageInventory(0, 0L, 0, 0L),
            inspectLocalStorage(folder)
        )
        assertTrue(outside.exists())
    }

    @Test fun rejectsFileOrAliasedDirectoryAsInventoryRoot() {
        assertNull(inspectLocalStorage(temporary.newFile("not-a-directory")))
        val actual = temporary.newFolder("actual")
        val alias = File(temporary.root, "alias")
        Files.createSymbolicLink(alias.toPath(), actual.toPath())
        assertNull(inspectLocalStorage(alias))
    }
}
