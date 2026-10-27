package io.github.gamesbyian.glimpse.domain

import kotlin.math.roundToInt

/**
 * Pure angular navigation for the five-window cropped photo reel.
 *
 * Presentation angle is ephemeral: the source of truth is always the stable
 * photo ID in the collection, not an ever-increasing angle or a wallpaper ID.
 * A negative angle advances toward later items, exactly one 72° notch per step.
 */
object ReelMotion {
    const val NOTCH_DEGREES = 72f
    const val MAX_NOTCHES_PER_GESTURE = 2

    /** Valid signed travel from a known focus index without looping or copying slots. */
    fun legalSteps(index: Int, count: Int, requested: Int): Int {
        if (count <= 0 || index !in 0 until count) return 0
        val target = (index.toLong() + requested.toLong())
            .coerceIn(0L, (count - 1).toLong()).toInt()
        return target - index
    }

    /**
     * Nearest legal stop for a drag's temporary angular displacement.
     * The caller renders the settling rotation before committing the target ID.
     */
    fun settleSteps(
        visualAngleDegrees: Float,
        index: Int,
        count: Int,
        maxSteps: Int = MAX_NOTCHES_PER_GESTURE
    ): Int {
        if (!visualAngleDegrees.isFinite() || maxSteps <= 0) return 0
        val requested = (-visualAngleDegrees / NOTCH_DEGREES).roundToInt()
            .coerceIn(-maxSteps, maxSteps)
        return legalSteps(index, count, requested)
    }

    fun targetAngle(steps: Int): Float = -steps * NOTCH_DEGREES

    /**
     * Convert drag X into signed angular displacement on a clipped bottom reel.
     * Width-normalized sensitivity is deterministic across phone widths.
     */
    fun dragDegrees(dxPx: Float, viewportWidthPx: Float): Float =
        if (!dxPx.isFinite() || !viewportWidthPx.isFinite() || viewportWidthPx <= 0f) 0f
        else dxPx / viewportWidthPx * 260f
}
