package io.github.gamesbyian.glimpse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCardReflowTest {
    @Test fun fullWidthPhoneUsesEditorialSideBySideCardAtNormalType() {
        assertFalse(shouldStackPhotoCard(411, 1f))
        assertFalse(shouldStackPhotoCard(412, 1.49f))
    }

    @Test fun compactPhoneAlwaysReflows() {
        assertTrue(shouldStackPhotoCard(320, 1f))
        assertTrue(shouldStackPhotoCard(360, 1f))
        assertFalse(shouldStackPhotoCard(361, 1f))
    }

    @Test fun doubledTextNeverSquashesTheCaptionBesideThumbnail() {
        assertTrue(shouldStackPhotoCard(320, 2f))
        assertTrue(shouldStackPhotoCard(411, 2f))
        assertTrue(shouldStackPhotoCard(412, 1.5f))
    }
}
