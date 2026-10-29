package io.github.gamesbyian.glimpse.domain

/**
 * Battery Saver protects foreground visible-shuffle eligibility without ever
 * consuming a photo or silently changing wallpaper. A deferred due window
 * cannot overflow a Long and is *later* than the clock snapshot.
 */
internal object BatterySaverRotationPolicy {
    fun deferDue(nowMillis: Long, cadenceMillis: Long): Long {
        require(cadenceMillis > 0L) { "Cadence must be positive." }
        return if (nowMillis >= Long.MAX_VALUE - cadenceMillis)
            Long.MAX_VALUE
        else nowMillis + cadenceMillis
    }
}
