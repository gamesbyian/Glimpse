package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CollectionReelMathTest {
    private fun window(degrees: Double, fraction: Double = .61): Pair<Float, Float> =
        (100 * fraction * cos(Math.toRadians(degrees))).toFloat() to
            (100 * fraction * sin(Math.toRadians(degrees))).toFloat()

    @Test fun allFiveWindowsAcceptPhysicalTaps() {
        for (offset in -2..2) {
            val (x, y) = window(126.0 + 72.0 * offset)
            assertEquals(offset,
                CollectionReelMath.windowOffset(x, y, 0f, 0f, 100f))
        }
    }

    @Test fun hubPaperRimAndExteriorDoNotSelectHiddenCollections() {
        val (hubX, hubY) = window(126.0, .1)
        assertNull(CollectionReelMath.windowOffset(hubX, hubY, 0f, 0f, 100f))
        val (rimX, rimY) = window(126.0, .98)
        assertNull(CollectionReelMath.windowOffset(rimX, rimY, 0f, 0f, 100f))
        val (paperX, paperY) = window(126.0 + 35.0)
        assertNull(CollectionReelMath.windowOffset(paperX, paperY, 0f, 0f, 100f))
        assertNull(CollectionReelMath.windowOffset(Float.NaN, 10f, 0f, 0f, 100f))
        assertNull(CollectionReelMath.windowOffset(50f, 50f, 0f, 0f, 0f))
    }

    @Test fun realCollectionIdsNeverWrapOrInventIndices() {
        assertEquals(1, CollectionReelMath.target(0, 3, 1))
        assertEquals(0, CollectionReelMath.target(1, 3, -1))
        assertNull(CollectionReelMath.target(0, 3, -1))
        assertNull(CollectionReelMath.target(2, 3, 1))
        assertNull(CollectionReelMath.target(-1, 3, 0))
        assertNull(CollectionReelMath.target(0, 0, 1))
    }
}
