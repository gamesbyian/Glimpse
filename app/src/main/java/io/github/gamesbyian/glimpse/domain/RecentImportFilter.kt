package io.github.gamesbyian.glimpse.domain

/**
 * Local-only recent import filter. A photo's immutable original import time
 * is not the same as the time it was attached to a second collection.
 * The filter never mutates collection membership or the rotation shuffle bag.
 */
internal object RecentImportFilter {
    const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

    fun isRecent(importedAt: Long, nowMillis: Long, windowMillis: Long = WEEK_MILLIS): Boolean {
        require(windowMillis > 0L) { "Recent window must be positive." }
        if (importedAt > nowMillis) return false
        val cutoff = if (nowMillis < Long.MIN_VALUE + windowMillis)
            Long.MIN_VALUE else nowMillis - windowMillis
        return importedAt >= cutoff
    }
}
