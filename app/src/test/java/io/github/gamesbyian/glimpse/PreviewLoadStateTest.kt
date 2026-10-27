package io.github.gamesbyian.glimpse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewLoadStateTest {
    @Test fun oldBitmapCannotAppearForNewSelection() {
        val completedOld = PreviewLoadState(
            sourcePath = "/originals/first.jpg",
            image = "first-photo-bitmap",
            complete = true
        )
        assertEquals("first-photo-bitmap", completedOld.imageFor("/originals/first.jpg"))
        assertNull(completedOld.imageFor("/originals/second.jpg"))
        assertTrue(completedOld.isLoading("/originals/second.jpg"))
    }

    @Test fun pendingBitmapMustNotBeShownUntilSourceDecodeCompletes() {
        val pending = PreviewLoadState(
            sourcePath = "/originals/new.jpg", image = "unfinished-image", complete = false
        )
        assertNull(pending.imageFor("/originals/new.jpg"))
        assertTrue(pending.isLoading("/originals/new.jpg"))
        val finished = pending.copy(complete = true)
        assertEquals("unfinished-image", finished.imageFor("/originals/new.jpg"))
        assertFalse(finished.isLoading("/originals/new.jpg"))
    }

    @Test fun emptyAndUnreadableSourcesRemainDistinct() {
        val noPhoto = PreviewLoadState<String>(sourcePath = null, complete = true)
        val corrupted = PreviewLoadState<String>(
            sourcePath = "/originals/broken.jpg", complete = true
        )
        assertFalse(noPhoto.isLoading(null))
        assertNull(noPhoto.imageFor(null))
        assertFalse(corrupted.isLoading("/originals/broken.jpg"))
        assertNull(corrupted.imageFor("/originals/broken.jpg"))
    }
}
