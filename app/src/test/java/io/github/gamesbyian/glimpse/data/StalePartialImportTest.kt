package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalePartialImportTest {
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
}
