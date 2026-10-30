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
    @Test fun potentialOrphansAreCountedWithoutDeletingAnything() {
        val originals = temporary.newFolder("orphan-inventory")
        val keep = File(originals, "a".repeat(64) + ".jpg").apply { writeBytes(ByteArray(10)) }
        val lost = File(originals, "b".repeat(64) + ".png").apply { writeBytes(ByteArray(14)) }
        File(originals, "import-work.partial").writeBytes(ByteArray(37))
        val reg = PhotoRecord(
            id = "a".repeat(64), sha256 = "a".repeat(64), localPath = keep.absolutePath,
            mime = "image/jpeg", width = 1, height = 1, byteLength = 10, importedAt = 1L
        )
        val actual = inspectPotentialOrphans(originals, listOf(reg))
        assertEquals(PotentialOrphanInventory(2, 1, 14L), actual)
        assertTrue(keep.exists())
        assertTrue(lost.exists())
    }

    @Test fun forgedDatabasePathCannotClaimPhysicalOriginal() {
        val folder = temporary.newFolder("owned-inventory")
        val sha = "c".repeat(64)
        File(folder, "$sha.jpg").writeBytes(ByteArray(7))
        val outside = temporary.newFolder("fake-root")
        val reg = PhotoRecord(
            id = sha, sha256 = sha, localPath = File(outside, "$sha.jpg").absolutePath,
            mime = "image/jpeg", width = 1, height = 1, byteLength = 7, importedAt = 1L
        )
        assertEquals(PotentialOrphanInventory(1, 1, 7L),
            inspectPotentialOrphans(folder, listOf(reg)))
    }

    @Test fun orphanAuditNeverFollowsSymlinkAndRejectsUnsafeRoot() {
        val folder = temporary.newFolder("safe-inventory")
        val original = temporary.newFile("outside-inventory").apply {
            writeBytes(ByteArray(15))
        }
        Files.createSymbolicLink(
            File(folder, "d".repeat(64) + ".jpg").toPath(), original.toPath()
        )
        assertEquals(PotentialOrphanInventory(0, 0, 0L),
            inspectPotentialOrphans(folder, emptyList()))
        assertTrue(original.exists())
        val shortcut = File(temporary.root, "root-inventory-shortcut")
        Files.createSymbolicLink(shortcut.toPath(), folder.toPath())
        assertNull(inspectPotentialOrphans(shortcut, emptyList()))
    }

}

