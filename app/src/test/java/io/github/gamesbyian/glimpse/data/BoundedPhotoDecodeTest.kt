package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedPhotoDecodeTest {
    @Test fun neverUpsamplesSmallImages() {
        assertEquals(320 to 240, BoundedPhotoDecode.targetSize(320, 240, 1024))
        assertEquals(2 to 4, BoundedPhotoDecode.targetSize(2, 4, 4096))
    }

    @Test fun landscapeAndPortraitObserveSameEdgeBound() {
        assertEquals(1024 to 768, BoundedPhotoDecode.targetSize(4000, 3000, 1024))
        assertEquals(768 to 1024, BoundedPhotoDecode.targetSize(3000, 4000, 1024))
        assertEquals(2400 to 3200, BoundedPhotoDecode.targetSize(3000, 4000, 3200))
    }

    @Test fun extremelyElongatedInputsRemainNonzeroAndBounded() {
        assertEquals(1 to 1024, BoundedPhotoDecode.targetSize(1, 50000, 1024))
        assertEquals(1024 to 1, BoundedPhotoDecode.targetSize(50000, 1, 1024))
    }

    @Test fun invalidSourceDimensionsOrBudgetAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BoundedPhotoDecode.targetSize(0, 100, 1024)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoundedPhotoDecode.targetSize(100, -1, 1024)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoundedPhotoDecode.targetSize(100, 100, 0)
        }
    }
}
