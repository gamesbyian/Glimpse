package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelMotionPreferenceTest {
    @Test fun liveZeroAnimatorScaleSuppressesAnimation() {
        assertTrue(shouldReduceReelMotion(0f, false))
        assertTrue(shouldReduceReelMotion(-1f, false))
    }

    @Test fun powerSaverSuppressionOverridesNormalAnimatorScale() {
        assertTrue(shouldReduceReelMotion(1f, true))
        assertTrue(shouldReduceReelMotion(2f, true))
    }

    @Test fun positiveFiniteScaleAllowsNormalSettling() {
        assertFalse(shouldReduceReelMotion(1f, false))
        assertFalse(shouldReduceReelMotion(0.5f, false))
    }

    @Test fun invalidScaleFailsSafeWithoutAnimation() {
        assertTrue(shouldReduceReelMotion(Float.NaN, false))
        assertTrue(shouldReduceReelMotion(Float.POSITIVE_INFINITY, false))
    }
}
