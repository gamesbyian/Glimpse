package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperAttemptTransitionTest {
    private val saved = RotationRecord(
        collectionId = "collection-a", active = true,
        remainingJson = """["later-1","later-2"]""",
        lastAcknowledgedPhotoId = "old-photo",
        lastAcknowledgedAt = 100L, lastAttemptAt = 99L,
        nextEligibleAt = 300L
    )
    private val reservedBag = """["later-2"]"""
    private fun finish(
        result: WallpaperAttemptResult,
        previous: RotationRecord = saved,
        finished: Long = 10_000L,
        remaining: String? = reservedBag
    ) = resolveWallpaperAttempt(
        previous, "new-photo", 9_000L, finished, 300_000L, remaining, result
    )

    @Test fun beginDoesNotAdvanceConfirmedPhotoOrBag() {
        val begun = beginWallpaperAttempt(saved, "new-photo", 9_000L)
        assertEquals("new-photo", begun.pendingPhotoId)
        assertEquals(9_000L, begun.lastAttemptAt)
        assertEquals("old-photo", begun.lastAcknowledgedPhotoId)
        assertEquals(saved.remainingJson, begun.remainingJson)
        assertTrue(begun.active)
        assertNull(saved.pendingPhotoId)
    }

    @Test fun onlyConfirmedPositiveIdCommitsOnePhotoAndOneWindow() {
        val after = finish(WallpaperAttemptResult.Acknowledged(42))
        assertNull(after.pendingPhotoId)
        assertEquals("new-photo", after.lastAcknowledgedPhotoId)
        assertEquals(10_000L, after.lastAcknowledgedAt)
        assertEquals(9_000L, after.lastAttemptAt)
        assertEquals(310_000L, after.nextEligibleAt)
        assertEquals(reservedBag, after.remainingJson)
        assertTrue(after.active)
        assertEquals("old-photo", saved.lastAcknowledgedPhotoId)
    }

    @Test fun uncertainAndroidResultDoesNotAdvanceAnything() {
        for (result in listOf(
            WallpaperAttemptResult.MissingAcknowledgment,
            WallpaperAttemptResult.Interrupted,
            WallpaperAttemptResult.PlatformError
        )) {
            val after = finish(result)
            assertEquals("new-photo", after.pendingPhotoId)
            assertFalse(after.active)
            assertEquals(saved.remainingJson, after.remainingJson)
            assertEquals("old-photo", after.lastAcknowledgedPhotoId)
            assertEquals(saved.nextEligibleAt, after.nextEligibleAt)
        }
    }

    @Test fun preOsErrorClearsPendingButPreservesSavedBag() {
        val after = finish(WallpaperAttemptResult.BeforePlatformError("IOException"))
        assertNull(after.pendingPhotoId)
        assertTrue(after.active)
        assertEquals(saved.remainingJson, after.remainingJson)
        assertEquals("old-photo", after.lastAcknowledgedPhotoId)
        assertEquals(310_000L, after.nextEligibleAt)
        assertTrue(after.outcome.contains("IOException"))
    }

    @Test fun manualApplyPreservesPriorScheduleAndBag() {
        val paused = saved.copy(active = false, nextEligibleAt = 123_456L)
        val after = finish(WallpaperAttemptResult.Acknowledged(1),
            previous = paused, remaining = null)
        assertEquals(paused.remainingJson, after.remainingJson)
        assertEquals(123_456L, after.nextEligibleAt)
        assertFalse(after.active)
        assertEquals("new-photo", after.lastAcknowledgedPhotoId)
    }

    @Test fun lateWorkCannotOverflowOrCatchUp() {
        val after = finish(WallpaperAttemptResult.Acknowledged(1),
            finished = Long.MAX_VALUE - 1L)
        assertEquals(Long.MAX_VALUE, after.nextEligibleAt)
        assertEquals(reservedBag, after.remainingJson)
    }

    @Test fun pendingGateAndInvalidAcknowledgmentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            beginWallpaperAttempt(saved.copy(pendingPhotoId = "unresolved"),
                "new-photo", 9_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            finish(WallpaperAttemptResult.Acknowledged(0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            finish(WallpaperAttemptResult.PlatformError,
                previous = saved.copy(pendingPhotoId = "unresolved"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            resolveWallpaperAttempt(saved, "new-photo", 9_000L,
                10_000L, 0L, reservedBag, WallpaperAttemptResult.PlatformError)
        }
    }
}
