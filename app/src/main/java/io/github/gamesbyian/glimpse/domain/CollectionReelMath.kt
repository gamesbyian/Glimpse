package io.github.gamesbyian.glimpse.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Pure hit geometry for the compact, top-right, five-window collection reel. */
internal object CollectionReelMath {
    const val SELECTED_DEGREES = 126f
    const val SLOT_DEGREES = 72f

    /**
     * Only the actual inset windows respond to taps; paper sectors respond
     * to turning gestures. The hub and exterior do not secretly select items.
     */
    fun windowOffset(
        x: Float, y: Float, centerX: Float, centerY: Float, radius: Float
    ): Int? {
        if (listOf(x, y, centerX, centerY, radius).any { !it.isFinite() } ||
            radius <= 0f
        ) return null
        val dx = x - centerX
        val dy = y - centerY
        val distance = hypot(dx, dy)
        if (distance < radius * .43f || distance > radius * .81f) return null
        val bearing = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        val delta = (bearing - SELECTED_DEGREES + 540f) % 360f - 180f
        val offset = (delta / SLOT_DEGREES).roundToInt().coerceIn(-2, 2)
        return offset.takeIf { abs(delta - offset * SLOT_DEGREES) <= 25f }
    }

    fun target(index: Int, count: Int, offset: Int): Int? {
        if (index !in 0 until count) return null
        val next = index + offset
        return next.takeIf { it in 0 until count }
    }
}
