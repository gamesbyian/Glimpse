package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationRestartPolicyTest {
    @Test fun samePausedCollectionRetainsUnusedBagAndConfirmedWallpaper() {
        val before = RotationRecord(
            collectionId = "rainbows", active = false,
            remainingJson = """["p2","p3"]""",
            lastAcknowledgedPhotoId = "p1", lastAcknowledgedAt = 777L,
            outcome = "Rotation paused"
        )
        val after = prepareRotationStart(before, "rainbows", 1000, 300000)
        assertEquals("""["p2","p3"]""", after.remainingJson)
        assertEquals("p1", after.lastAcknowledgedPhotoId)
        assertEquals(777L, after.lastAcknowledgedAt)
        assertEquals(301000L, after.nextEligibleAt)
        assertTrue(after.active)
    }

    @Test fun changingSourceResetsBagWithoutInventingWallpaperAcknowledgment() {
        val before = RotationRecord(
            collectionId = "rainbows", active = false,
            remainingJson = """["p2"]""",
            lastAcknowledgedPhotoId = "p1"
        )
        val after = prepareRotationStart(before, "landscapes", 10, 300000)
        assertEquals("[]", after.remainingJson)
        assertEquals("landscapes", after.collectionId)
        assertEquals("p1", after.lastAcknowledgedPhotoId)
        assertTrue(after.active)
    }

    @Test fun transitionDoesNotMutateInputOrLosePendingGate() {
        val before = RotationRecord(
            collectionId = "a", active = false, pendingPhotoId = "uncertain",
            remainingJson = "[]"
        )
        val after = prepareRotationStart(before, "a", 10, 300000)
        // The gateway refuses any pending token *before* invoking policy.
        assertEquals("uncertain", after.pendingPhotoId)
        assertFalse(before.active)
        assertTrue(after.active)
    }
}
