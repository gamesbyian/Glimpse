package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class CropResolutionAdvisoryTest {
    @Test fun fullResolutionSourceClearsACompactDisplay() {
        val a = estimateCropResolution(PhotoEditGeometry(), 4000, 6000, 1080, 2400)
        assertEquals(4000, a.width)
        assertEquals(6000, a.height)
        assertFalse(a.belowDisplay)
    }

    @Test fun severeCropWarnsWithoutUpscalingSourcePixels() {
        val edit = PhotoEditGeometry(0, NormalizedCrop(.2, .1, .4, .5))
        val a = estimateCropResolution(edit, 2000, 3000, 1080, 2400)
        assertEquals(400, a.width)
        assertEquals(1200, a.height)
        assertTrue(a.belowDisplay)
    }

    @Test fun quarterTurnSwapsDimensionsBeforeCropAssessment() {
        val edit = PhotoEditGeometry(clockwiseQuarterTurns = 1)
        val a = estimateCropResolution(edit, 4000, 2000, 1080, 2400)
        assertEquals(2000, a.width)
        assertEquals(4000, a.height)
        assertFalse(a.belowDisplay)
    }

    @Test fun invalidDisplayDimensionsNeverPassAsGoodQuality() {
        assertThrows(IllegalArgumentException::class.java) {
            estimateCropResolution(PhotoEditGeometry(), 2000, 1000, 0, 1920)
        }
    }
}
