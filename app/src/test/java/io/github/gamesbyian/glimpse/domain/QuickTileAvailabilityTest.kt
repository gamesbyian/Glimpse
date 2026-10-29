package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickTileAvailabilityTest {
    @Test fun freshInstallAndMissingSourceCannotRequestNext() {
        assertEquals(QuickTileAvailability.NO_SOURCE, quickTileAvailability(null))
        assertEquals(QuickTileAvailability.NO_SOURCE,
            quickTileAvailability(RotationRecord(active = true)))
    }

    @Test fun unresolvedAndroidAttemptTakesPrecedenceOverSourceAndPause() {
        assertEquals(QuickTileAvailability.UNCERTAIN,
            quickTileAvailability(RotationRecord(
                collectionId = "rainbows", active = false,
                pendingPhotoId = "unconfirmed"
            )))
        assertEquals(QuickTileAvailability.UNCERTAIN,
            quickTileAvailability(RotationRecord(pendingPhotoId = "uncertain")))
    }

    @Test fun pausedSourceStillPermitsAnExplicitManualChange() {
        assertEquals(QuickTileAvailability.READY,
            quickTileAvailability(RotationRecord(
                collectionId = "rainbows", active = false
            )))
        assertEquals(QuickTileAvailability.READY,
            quickTileAvailability(RotationRecord(
                collectionId = "rainbows", active = true
            )))
    }
}
