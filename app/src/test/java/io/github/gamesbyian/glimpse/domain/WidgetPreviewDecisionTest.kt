package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreviewDecisionTest {
    @Test fun missingCollectionNeverPromisesNextPhotoOrButton() {
        val noSource = widgetPreviewDecision(null, false, "fake")
        assertEquals(WidgetPreviewKind.NO_SOURCE, noSource.kind)
        assertFalse(noSource.canRequestChange)
        assertNull(noSource.knownNextId)
        val stale = widgetPreviewDecision(RotationRecord(
            collectionId = "removed"), false, "fake")
        assertEquals(WidgetPreviewKind.NO_SOURCE, stale.kind)
    }

    @Test fun unknownPlatformOutcomeBlocksEvenPausedSource() {
        val outcome = widgetPreviewDecision(RotationRecord(
            collectionId = "real", active = false,
            pendingPhotoId = "unknown"
        ), true, "next")
        assertEquals(WidgetPreviewKind.UNCERTAIN, outcome.kind)
        assertFalse(outcome.canRequestChange)
        assertNull(outcome.knownNextId)
    }

    @Test fun persistedCandidateCanBeShownWithoutMutatingQueue() {
        val saved = RotationRecord(
            collectionId = "real", active = true,
            remainingJson = """["next","other"]"""
        )
        val outcome = widgetPreviewDecision(saved, true, "next")
        assertEquals(WidgetPreviewKind.NEXT_KNOWN, outcome.kind)
        assertEquals("next", outcome.knownNextId)
        assertTrue(outcome.canRequestChange)
        assertEquals("""["next","other"]""", saved.remainingJson)
    }

    @Test fun onlyPhotoAlreadyAppliedCannotOfferFutileChange() {
        val previous = RotationRecord(
            collectionId = "source", lastAcknowledgedPhotoId = "sole",
            remainingJson = """["sole"]"""
        )
        val decision = widgetPreviewDecision(
            previous, true, "sole", soleEligiblePhotoId = "sole"
        )
        assertEquals(WidgetPreviewKind.CURRENT_ONLY, decision.kind)
        assertFalse(decision.canRequestChange)
        assertNull(decision.knownNextId)
    }

    @Test fun onePhotoDifferentFromLastAcknowledgedMayChange() {
        val previous = RotationRecord(
            collectionId = "source", lastAcknowledgedPhotoId = "other"
        )
        val decision = widgetPreviewDecision(
            previous, true, null, soleEligiblePhotoId = "sole"
        )
        assertEquals(WidgetPreviewKind.NEXT_UNDECIDED, decision.kind)
        assertTrue(decision.canRequestChange)
    }

    @Test fun uncertainWallpaperAlwaysBlocksEvenIfOnlySourceIsAlreadyApplied() {
        val previous = RotationRecord(
            collectionId = "source", lastAcknowledgedPhotoId = "sole",
            pendingPhotoId = "sole"
        )
        val decision = widgetPreviewDecision(
            previous, true, "sole", soleEligiblePhotoId = "sole"
        )
        assertEquals(WidgetPreviewKind.UNCERTAIN, decision.kind)
        assertFalse(decision.canRequestChange)
    }

    @Test fun missingPreviewDoesNotDisableExplicitManualChange() {
        val outcome = widgetPreviewDecision(RotationRecord(
            collectionId = "real", active = false
        ), true, null)
        assertEquals(WidgetPreviewKind.NEXT_UNDECIDED, outcome.kind)
        assertTrue(outcome.canRequestChange)
        assertNull(outcome.knownNextId)
    }
}
