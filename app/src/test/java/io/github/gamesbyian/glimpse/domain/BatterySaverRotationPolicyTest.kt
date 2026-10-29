package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BatterySaverRotationPolicyTest {
    @Test fun dueWindowMovesOneCadenceIntoFutureWithoutAnyCatchUpSlots() {
        val current = 1_800_000L
        assertEquals(2_100_000L,
            BatterySaverRotationPolicy.deferDue(current, 300_000L))
        // Deferred eligibility is measured once from NOW, not from an old
        // missed appointment. A 4-hour sleep must not cause a replay burst.
        assertEquals(14_700_000L,
            BatterySaverRotationPolicy.deferDue(14_400_000L, 300_000L))
    }

    @Test fun boundaryOverflowCannotSchedulePastLongMax() {
        assertEquals(Long.MAX_VALUE,
            BatterySaverRotationPolicy.deferDue(Long.MAX_VALUE - 1L, 300_000L))
    }

    @Test fun invalidCadenceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BatterySaverRotationPolicy.deferDue(0L, 0L)
        }
    }
}
