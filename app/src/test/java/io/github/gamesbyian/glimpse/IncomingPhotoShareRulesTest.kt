package io.github.gamesbyian.glimpse

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingPhotoShareRulesTest {
    @Test fun acceptsSingleAndMultipleContentImages() {
        assertEquals(IncomingShareCheck.READY,
            validateIncomingShare("android.intent.action.SEND", "image/png", listOf("content")))
        assertEquals(IncomingShareCheck.READY,
            validateIncomingShare("android.intent.action.SEND_MULTIPLE", "image/*",
                listOf("content", "content")))
    }

    @Test fun cannotLaunchThroughAnUnrelatedActionOrNonImageMime() {
        assertEquals(IncomingShareCheck.NOT_SHARE,
            validateIncomingShare("android.intent.action.VIEW", "image/png", listOf("content")))
        assertEquals(IncomingShareCheck.UNSUPPORTED_TYPE,
            validateIncomingShare("android.intent.action.SEND", "text/plain", listOf("content")))
        assertEquals(IncomingShareCheck.UNSUPPORTED_TYPE,
            validateIncomingShare("android.intent.action.SEND", null, listOf("content")))
        assertEquals(IncomingShareCheck.UNSUPPORTED_TYPE,
            validateIncomingShare("android.intent.action.SEND", "image/", listOf("content")))
    }

    @Test fun emptyOrFileOrHttpUrisCannotBeImported() {
        assertEquals(IncomingShareCheck.MISSING_IMAGES,
            validateIncomingShare("android.intent.action.SEND", "image/jpeg", emptyList()))
        assertEquals(IncomingShareCheck.INVALID_URI,
            validateIncomingShare("android.intent.action.SEND", "image/jpeg", listOf("file")))
        assertEquals(IncomingShareCheck.INVALID_URI,
            validateIncomingShare("android.intent.action.SEND_MULTIPLE", "image/png",
                listOf("content", "https")))
        assertEquals(IncomingShareCheck.INVALID_URI,
            validateIncomingShare("android.intent.action.SEND", "image/png", listOf(null)))
    }

    @Test fun rejectsOversizedBatchWithoutSilentlyDroppingAnyPhotos() {
        assertEquals(IncomingShareCheck.READY,
            validateIncomingShare("android.intent.action.SEND_MULTIPLE", "image/*",
                List(100) { "content" }))
        assertEquals(IncomingShareCheck.TOO_MANY,
            validateIncomingShare("android.intent.action.SEND_MULTIPLE", "image/*",
                List(101) { "content" }))
    }
}
