package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelMotionTest {
    @Test fun endpointsNeverWrapOrInventPhotos() {
        assertEquals(0, ReelMotion.legalSteps(-1, 0, 1))
        assertEquals(0, ReelMotion.legalSteps(0, 1, -10))
        assertEquals(0, ReelMotion.legalSteps(0, 1, 10))
        assertEquals(0, ReelMotion.legalSteps(0, 22, -1))
        assertEquals(0, ReelMotion.legalSteps(21, 22, 1))
        assertEquals(2, ReelMotion.legalSteps(0, 22, 2))
        assertEquals(-2, ReelMotion.legalSteps(21, 22, -2))
        assertEquals(1, ReelMotion.legalSteps(1, 3, 2))
    }

    @Test fun directionAndSettlingAreDeterministic() {
        assertEquals(0, ReelMotion.settleSteps(-31f, 7, 22))
        assertEquals(1, ReelMotion.settleSteps(-40f, 7, 22))
        assertEquals(-1, ReelMotion.settleSteps(52f, 7, 22))
        assertEquals(2, ReelMotion.settleSteps(-149f, 7, 22))
        assertEquals(-2, ReelMotion.settleSteps(150f, 7, 22))
        assertEquals(0, ReelMotion.settleSteps(Float.NaN, 7, 22))
        assertEquals(0, ReelMotion.settleSteps(Float.POSITIVE_INFINITY, 7, 22))
        assertEquals(-72f, ReelMotion.targetAngle(1))
        assertEquals(72f, ReelMotion.targetAngle(-1))
    }

    @Test fun shortListsAndManyPhotosUseExactlyTheSameViewportLogic() {
        for (count in listOf(0, 1, 2, 3, 5, 22, 60, 10_000)) {
            val ids = (0 until count).map { "real-${it}" }
            for (index in listOf(0, count / 2, count - 1)) {
                if (index !in 0 until count) continue
                val actual = ReelMotion.settleSteps(-144f, index, count)
                val next = index + actual
                assert(next in 0 until count)
                assertEquals(ids[next], ReelWindow.next(ids, ids[index], actual.coerceIn(-1, 1)).takeIf { actual in -1..1 } ?: ids[next])
                assertEquals(5, ReelWindow.slots(ids, ids[next]).size)
                assertEquals(ids[next], ReelWindow.slots(ids, ids[next])[2])
            }
        }
    }

    @Test fun dragProjectionIsWidthNormalizedAndCannotGenerateNaN() {
        assertEquals(-72.22222f, ReelMotion.dragDegrees(-100f, 360f), .0001f)
        assertEquals(-72.22222f, ReelMotion.dragDegrees(-50f, 180f), .0001f)
        assertEquals(0f, ReelMotion.dragDegrees(100f, 0f))
        assertEquals(0f, ReelMotion.dragDegrees(Float.NaN, 360f))
        assertEquals(0, ReelMotion.settleSteps(-72f, -1, 3))
    }

    @Test fun repeatedAdvanceAndReverseNeverEscapesFiniteCollection() {
        for (n in listOf(1, 3, 5, 22, 60)) {
            var focus = 0
            repeat(n * 3) { focus += ReelMotion.settleSteps(-75f, focus, n) }
            assertEquals(n - 1, focus)
            repeat(n * 3) { focus += ReelMotion.settleSteps(75f, focus, n) }
            assertEquals(0, focus)
        }
    }
    @Test fun angularMotionWorksFromPhotoWindowsInEitherDirection() {
        assertEquals(112.5f, ReelMotion.angularDragDegrees(
            0f, -100f, 100f, 0f, 0f, 0f, 200f, 400f), .01f)
        assertEquals(-112.5f, ReelMotion.angularDragDegrees(
            0f, -100f, -100f, 0f, 0f, 0f, 200f, 400f), .01f)
        // Crossing the atan2 seam must remain a tiny motion, not a full turn.
        val seam = ReelMotion.angularDragDegrees(
            -100f, -1f, -100f, 1f, 0f, 0f, 200f, 400f)
        assert(kotlin.math.abs(seam) < 5f)
        assertEquals(0f, ReelMotion.angularDragDegrees(
            Float.NaN, 0f, 100f, 0f, 0f, 0f, 200f, 400f))
        assertEquals(0f, ReelMotion.angularDragDegrees(
            0f, 0f, 100f, 0f, 0f, 0f, 0f, 400f))
    }


}
