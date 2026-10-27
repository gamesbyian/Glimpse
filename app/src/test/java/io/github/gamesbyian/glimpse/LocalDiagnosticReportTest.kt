package io.github.gamesbyian.glimpse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDiagnosticReportTest {
    @Test fun reportIsAllowlistedNumbersAndBuildFactsOnly() {
        val text = LocalDiagnosticReport.render(
            DiagnosticFacts(17, true, 34, 22, 823L, 1, 9L)
        )
        assertTrue(text.contains("App version code: 17"))
        assertTrue(text.contains("Debug/development"))
        assertTrue(text.contains("Android API level: 34"))
        assertTrue(text.contains("22 originals / 823 bytes"))
        assertTrue(text.contains("1 staging fragments / 9 bytes"))
        assertTrue(text.contains("[Describe the issue here before sharing.]"))
        assertFalse(text.contains("content://"))
        assertFalse(text.contains("/data/user/"))
    }

    @Test fun unavailableStorageIsNotReportedAsZeroUse() {
        val text = LocalDiagnosticReport.render(
            DiagnosticFacts(1, false, 37, null, null, null, null)
        )
        assertTrue(text.contains("Distribution unverified"))
        assertTrue(text.contains("Unavailable (no files modified)"))
        assertFalse(text.contains("0 originals"))
    }
}
