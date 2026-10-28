package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCropExportGeometryTest {
    @Test fun sameNormalizedCropAgreesAcrossPreviewAndExportResolutions() {
        val edit = PhotoEditGeometry(0, NormalizedCrop(.10, .20, .90, .75))
        assertEquals(PixelCrop(10,20,90,75), edit.pixelCrop(100,100))
        assertEquals(PixelCrop(100,200,900,750), edit.pixelCrop(1000,1000))
    }

    @Test fun cropIsAppliedAfterClockwiseRotation() {
        val edit = PhotoEditGeometry(1, NormalizedCrop(.25,.1,.75,.9))
        assertEquals(PixelCrop(50,10,150,90), edit.pixelCrop(100,200))
        assertTrue(edit.pixelCrop(100,200).width > 0)
    }

    @Test fun resetRestoresUnrotatedOriginalFrame() {
        val edit = PhotoEditGeometry(3, NormalizedCrop(.2,.3,.8,.9))
        assertEquals(PixelCrop(0,0,300,200), edit.reset().pixelCrop(300,200))
    }
}
