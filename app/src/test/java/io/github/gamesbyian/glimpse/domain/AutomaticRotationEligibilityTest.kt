package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticRotationEligibilityTest {
    private val active = RotationRecord(
        collectionId = "real-collection", active = true, nextEligibleAt = 300_000L
    )

    @Test fun processStartAndPausedRotationCannotApplyAutomatically() {
        assertEquals(AutomaticRotationGate.PAUSED,
            automaticRotationGate(null, 500_000L, false))
        assertEquals(AutomaticRotationGate.PAUSED,
            automaticRotationGate(active.copy(active = false), 500_000L, false))
        assertEquals(AutomaticRotationGate.PAUSED,
            automaticRotationGate(active.copy(collectionId = null), 500_000L, false))
    }

    @Test fun pendingUnknownOutcomeBlocksAnyNewBackgroundAttempt() {
        val pending = active.copy(pendingPhotoId = "unconfirmed", nextEligibleAt = 0L)
        assertEquals(AutomaticRotationGate.UNCERTAIN,
            automaticRotationGate(pending, 500_000L, false))
        assertEquals(AutomaticRotationGate.UNCERTAIN,
            automaticRotationGate(pending, 500_000L, true))
    }

    @Test fun periodicWorkerNeverReplaysEarlyOrMissedIntervals() {
        assertEquals(AutomaticRotationGate.NOT_DUE,
            automaticRotationGate(active, 299_999L, false))
        assertEquals(AutomaticRotationGate.ELIGIBLE,
            automaticRotationGate(active, 300_000L, false))
        assertEquals(AutomaticRotationGate.ELIGIBLE,
            automaticRotationGate(active, 50_000_000L, false))
        // The actual serialized writer performs only ONE eligible attempt.
        // Its success stores the next due time; no catch-up loop is present.
    }

    @Test fun batterySaverBlocksEvenWhenAndroidWakesWorkerLate() {
        assertEquals(AutomaticRotationGate.BATTERY_SAVER,
            automaticRotationGate(active, 500_000L, true))
        assertEquals(AutomaticRotationGate.NOT_DUE,
            automaticRotationGate(active, 250_000L, true))
    }
}
