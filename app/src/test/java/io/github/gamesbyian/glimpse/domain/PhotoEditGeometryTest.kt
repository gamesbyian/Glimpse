package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditGeometryTest {
    @Test fun defaultRecipeIsExactFullFrameAtAnyShape() {
        val recipe = PhotoEditGeometry()
        assertEquals(PixelCrop(0, 0, 30000, 4000), recipe.pixelCrop(30000, 4000))
        assertEquals(PixelCrop(0, 0, 1, 1), recipe.pixelCrop(1, 1))
    }

    @Test fun quarterTurnsSwapFrameDimensionsAndWrapWithoutDrift() {
        val original = PhotoEditGeometry()
        val ninety = original.rotateClockwise()
        assertEquals(PixelCrop(0, 0, 240, 120), ninety.pixelCrop(120, 240))
        assertEquals(PhotoEditGeometry(), ninety.rotateCounterclockwise())
        val fullCycle = (1..4).fold(original) { recipe, _ -> recipe.rotateClockwise() }
        assertEquals(original, fullCycle)
    }

    @Test fun cropUsesOrientedCoordinatesAndRoundsOutward() {
        val recipe = PhotoEditGeometry(
            1, NormalizedCrop(0.25, 0.1, 0.75, 0.9)
        )
        assertEquals(PixelCrop(50, 10, 150, 90), recipe.pixelCrop(100, 200))
        assertEquals(PixelCrop(0, 0, 7, 5),
            PhotoEditGeometry().pixelCrop(7, 5))
    }

    @Test fun nearSubpixelPositiveCropDoesNotCollapseToZero() {
        val micro = PhotoEditGeometry(
            0, NormalizedCrop(0.50001, 0.50001, 0.50002, 0.50002)
        )
        val resolved = micro.pixelCrop(10, 5)
        assertEquals(1, resolved.width)
        assertEquals(1, resolved.height)
    }

    @Test fun malformedAndNonFiniteCropsRejectBeforeStorageOrRendering() {
        listOf(
            { NormalizedCrop(Double.NaN, 0.0, 1.0, 1.0) },
            { NormalizedCrop(0.0, 0.0, Double.POSITIVE_INFINITY, 1.0) },
            { NormalizedCrop(-0.1, 0.0, 1.0, 1.0) },
            { NormalizedCrop(0.4, 0.0, 0.4, 1.0) },
            { NormalizedCrop(0.0, 0.8, 1.0, 0.2) }
        ).forEach { construct ->
            assertThrows(IllegalArgumentException::class.java) { construct() }
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoEditGeometry(4)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoEditGeometry().pixelCrop(0, 100)
        }
    }

    @Test fun resetDropsAllRotationAndCropEditsWithoutModifyingSource() {
        val geometry = PhotoEditGeometry(3, NormalizedCrop(0.2, 0.3, 0.7, 0.8))
        assertEquals(PhotoEditGeometry(), geometry.reset())
        assertTrue(geometry != geometry.reset())
    }
}
