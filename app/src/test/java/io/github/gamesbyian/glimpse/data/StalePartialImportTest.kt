package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StalePartialImportTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = 1_700_000_000_000L
    private val grace = PhotoImporter.STALE_PARTIAL_GRACE_MS

    @Test fun onlyAgedImporterStagingNamesAreEligible() {
        assertTrue(PhotoImporter.isStaleImportPartial(
            "import-4711.partial", now - grace, now
        ))
        assertFalse(PhotoImporter.isStaleImportPartial(
            "import-4711.partial", now - grace + 1, now
        ))
    }

    @Test fun neverSelectFinishedOriginalsOrUnrelatedAppFiles() {
        val aged = now - grace - 10_000
        listOf(
            "a".repeat(64) + ".jpg",
            "a".repeat(64) + ".webp",
            "import-4711.jpg",
            "readme.partial",
            "../import-4711.part",
            "import-4711.partial.bak"
        ).forEach { name ->
            assertFalse(PhotoImporter.isStaleImportPartial(name, aged, now))
        }
    }

    @Test fun unknownTimestampsAndClockRollbackDoNotDelete() {
        assertFalse(PhotoImporter.isStaleImportPartial("import-x.partial", 0L, now))
        assertFalse(PhotoImporter.isStaleImportPartial("import-x.partial", now + 100L, now))
    }

    @Test fun filesystemRecoveryRemovesOnlyOldPartialFragments() {
        val root = temporary.newFolder("originals")
        fun create(name: String, age: Long): File = File(root, name).apply {
            writeText("private fixture")
            assertTrue(setLastModified(now - age))
        }

        val stale = create("import-abandoned.partial", grace + 1000)
        val recent = create("import-recent.partial", grace - 1000)
        val finished = create("a".repeat(64) + ".jpg", grace + 1000)
        val other = create("notes.partial", grace + 1000)
        val nested = File(root, "import-directory.partial").apply { mkdir() }

        assertEquals(1, pruneStalePartialFiles(root, now))
        assertFalse(stale.exists())
        assertTrue(recent.exists())
        assertTrue(finished.exists())
        assertTrue(other.exists())
        assertTrue(nested.isDirectory)
        assertEquals(0, pruneStalePartialFiles(root, now))
    }

    @Test fun recoveryCannotEscapeOriginalsViaSymbolicLink() {
        val root = temporary.newFolder("originals")
        val elsewhere = temporary.newFile("outside.jpg").apply {
            writeText("should survive")
            assertTrue(setLastModified(now - grace - 1000))
        }
        val alias = File(root, "import-link.partial")
        java.nio.file.Files.createSymbolicLink(alias.toPath(), elsewhere.toPath())
        assertEquals(0, pruneStalePartialFiles(root, now))
        assertTrue(alias.exists())
        assertEquals("should survive", elsewhere.readText())
    }
}
