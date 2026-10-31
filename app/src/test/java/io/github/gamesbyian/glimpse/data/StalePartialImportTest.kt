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

    @Test fun onlyEmptyOldPlaceholdersAreCleanedWhileDataBearingStagesSurvive() {
        val root = temporary.newFolder("originals")
        fun create(name: String, age: Long, bytes: ByteArray): File =
            File(root, name).apply {
                writeBytes(bytes)
                assertTrue(setLastModified(now - age))
            }

        val salvageable = create(
            "import-abandoned.partial", grace + 1000, "last complete staged image".toByteArray()
        )
        val empty = create("import-created-before-crash.partial", grace + 1000, byteArrayOf())
        val recentEmpty = create("import-recent.partial", grace - 1000, byteArrayOf())
        val finished = create("a".repeat(64) + ".jpg", grace + 1000, byteArrayOf())
        val other = create("notes.partial", grace + 1000, byteArrayOf())
        val nested = File(root, "import-directory.partial").apply { mkdir() }

        assertEquals(1, pruneStalePartialFiles(root, now))
        assertTrue(salvageable.exists())
        assertEquals("last complete staged image", salvageable.readText())
        assertFalse(empty.exists())
        assertTrue(recentEmpty.exists())
        assertTrue(finished.exists())
        assertTrue(other.exists())
        assertTrue(nested.isDirectory)
        assertEquals(0, pruneStalePartialFiles(root, now))
    }

    @Test fun crashedExclusivePublicationPreservesFsyncedStagingAndIncompleteTarget() {
        val root = temporary.newFolder("originals")
        val stage = File(root, "import-interrupted.partial").apply {
            writeBytes("entire verified source from Photo Picker".toByteArray())
            assertTrue(setLastModified(now - grace - 1_000))
        }
        val existingDigest = File(root, "c".repeat(64) + ".jpg").apply {
            writeBytes("partial finalization".toByteArray())
            assertTrue(setLastModified(now - grace - 1_000))
        }
        val stageBefore = stage.readBytes()
        val targetBefore = existingDigest.readBytes()
        assertEquals(0, pruneStalePartialFiles(root, now))
        assertTrue(stage.exists())
        assertTrue(existingDigest.exists())
        org.junit.Assert.assertArrayEquals(stageBefore, stage.readBytes())
        org.junit.Assert.assertArrayEquals(targetBefore, existingDigest.readBytes())
    }

    @Test fun importerFinallyPreservesLastValidatedStageWhenPublicationFailed() {
        assertFalse(canDiscardImportStaging(validated = true, published = false))
        assertTrue(canDiscardImportStaging(validated = true, published = true))
        assertTrue(canDiscardImportStaging(validated = false, published = false))
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
    @Test fun recoveryRefusesAnAliasedOriginalsDirectory() {
        val outsideRoot = temporary.newFolder("not-originals")
        val oldImport = File(outsideRoot, "import-external.partial").apply {
            writeText("must not be removed")
            assertTrue(setLastModified(now - grace - 1_000))
        }
        val alias = File(temporary.root, "aliased-originals")
        java.nio.file.Files.createSymbolicLink(alias.toPath(), outsideRoot.toPath())
        assertEquals(0, pruneStalePartialFiles(alias, now))
        assertTrue(oldImport.exists())
        assertEquals("must not be removed", oldImport.readText())
    }

}
