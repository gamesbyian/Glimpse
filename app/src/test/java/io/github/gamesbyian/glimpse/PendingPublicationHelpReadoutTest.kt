package io.github.gamesbyian.glimpse

import io.github.gamesbyian.glimpse.data.PendingPublicationEvidence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPublicationHelpReadoutTest {
    private fun example(
        examined: Int = 2, omitted: Int = 0,
        untrusted: Int = 0, exhausted: Boolean = false
    ) = PendingPublicationEvidence(
        examinedReceipts = examined, omittedReceipts = omitted,
        untrustedReceipts = untrusted,
        stagesExact = 1, stagesMissing = 0, stagesUnverified = 1,
        targetsExact = 0, targetsMissing = 1, targetsUnverified = 1,
        hashBudgetExhausted = exhausted
    )

    @Test fun displaysAggregateOnlyAndDoesNotClaimRecoveredPhotos() {
        val text = describePublicationEvidence(example())
        assertTrue(text.contains("Examined 2 attempt receipt(s)"))
        assertTrue(text.contains("Staging: 1 exact, 0 missing, 1 unverified"))
        assertTrue(text.contains("Targets: 0 exact, 1 missing, 1 unverified"))
        assertTrue(text.contains("cannot establish file ownership"))
        assertTrue(text.contains("No photos, receipts or library records were modified"))
        assertFalse(text.contains("recovered successfully"))
        assertFalse(text.contains("Delete"))
    }

    @Test fun exhaustedScanDisclosesLimitsWithoutDeclaringCorruption() {
        val text = describePublicationEvidence(example(omitted = 39, exhausted = true))
        assertTrue(text.contains("39 omitted"))
        assertTrue(text.contains("some files were not hashed"))
        assertFalse(text.contains("Corrupt original"))
    }

    @Test fun noReceiptsDoesNotImplyNoPriorInterruptedImports() {
        val text = describePublicationEvidence(example(examined = 0))
        assertTrue(text.contains("Older interruptions may have no receipt"))
        assertTrue(text.contains("not recovery clearance"))
        assertFalse(text.contains("All imports succeeded"))
    }

    @Test fun damagedReceiptIsOnlyUntrustedEvidence() {
        val text = describePublicationEvidence(example(untrusted = 1))
        assertTrue(text.contains("1 untrusted"))
        assertFalse(text.contains("safe to discard"))
    }
}
