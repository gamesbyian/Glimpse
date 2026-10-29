package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class RecentImportFilterTest {
    private val now = 1_800_000_000_000L

    @Test fun inclusiveSevenDayWindowHasNoMutationOrFutureClockClaims() {
        assertTrue(RecentImportFilter.isRecent(now, now))
        assertTrue(RecentImportFilter.isRecent(now - RecentImportFilter.WEEK_MILLIS, now))
        assertFalse(RecentImportFilter.isRecent(now - RecentImportFilter.WEEK_MILLIS - 1, now))
        assertFalse(RecentImportFilter.isRecent(now + 1, now))
    }

    @Test fun expiryFollowsClockAndDoesNotOverflowAtLongMin() {
        val imported = now - RecentImportFilter.WEEK_MILLIS + 15_000L
        assertTrue(RecentImportFilter.isRecent(imported, now))
        assertFalse(RecentImportFilter.isRecent(imported,
            now + 60_000L))
        assertTrue(RecentImportFilter.isRecent(Long.MIN_VALUE, Long.MIN_VALUE))
    }

    @Test fun invalidWindowsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RecentImportFilter.isRecent(now, now, 0)
        }
    }
}
