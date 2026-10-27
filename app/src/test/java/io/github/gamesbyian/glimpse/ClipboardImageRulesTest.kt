package io.github.gamesbyian.glimpse

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardImageRulesTest {
    @Test fun acceptsExplicitImageContentUris() {
        assertEquals(ClipboardImageCheck.READY,
            checkClipboardImages(listOf("image/png"), listOf("content")))
        assertEquals(ClipboardImageCheck.READY,
            checkClipboardImages(listOf("image/jpeg", "text/plain"),
                listOf("content", "content")))
    }

    @Test fun textEmptyAndNonContentClipsNeverPassAsImages() {
        assertEquals(ClipboardImageCheck.EMPTY,
            checkClipboardImages(listOf("image/png"), emptyList()))
        assertEquals(ClipboardImageCheck.NOT_IMAGE,
            checkClipboardImages(listOf("text/plain"), listOf("content")))
        assertEquals(ClipboardImageCheck.NOT_IMAGE,
            checkClipboardImages(emptyList(), listOf("content")))
        assertEquals(ClipboardImageCheck.NON_CONTENT_URI,
            checkClipboardImages(listOf("image/png"), listOf("file")))
        assertEquals(ClipboardImageCheck.NON_CONTENT_URI,
            checkClipboardImages(listOf("image/png"), listOf("content", null)))
    }

    @Test fun oversizedClipboardCannotBeSilentlyTruncated() {
        assertEquals(ClipboardImageCheck.READY,
            checkClipboardImages(listOf("image/*"), List(100) { "content" }))
        assertEquals(ClipboardImageCheck.TOO_MANY,
            checkClipboardImages(listOf("image/png"), List(101) { "content" }))
    }
}
