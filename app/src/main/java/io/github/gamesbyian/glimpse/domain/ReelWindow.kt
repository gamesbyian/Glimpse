package io.github.gamesbyian.glimpse.domain

/**
 * Bounded five-aperture view onto a finite collection. The number of paper
 * windows never caps the underlying photo count and no phantom duplicates
 * are inserted near either endpoint.
 */
object ReelWindow {
    /**
     * Return the tapped aperture offset for the currently drawn cropped
     * wheel, or null when touching paper outside the photo ring. This is
     * geometry only; callers still check that a real photo occupies the slot.
     */
    fun tapOffset(x: Float, y: Float, width: Float, height: Float): Int? {
        if (width <= 0f || height <= 0f) return null
        val dx = x - width * .5f
        val dy = y - height * .82f
        val radius = kotlin.math.hypot(dx, dy)
        val outer = width * .84f
        if (radius < outer * .31f || radius > outer * .95f) return null
        val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
        val relative = (angle + 90f + 540f) % 360f - 180f
        val offset = kotlin.math.round(relative / 72f).toInt()
        return offset.takeIf { it in -2..2 && kotlin.math.abs(relative - it * 72f) <= 31f }
    }

    fun focusIndex(ids: List<String>, selectedId: String?): Int =
        if (ids.isEmpty()) -1
        else ids.indexOf(selectedId).takeIf { it >= 0 } ?: 0

    fun slots(ids: List<String>, selectedId: String?): List<String?> {
        val index = focusIndex(ids, selectedId)
        return (-2..2).map { offset ->
            ids.getOrNull(index + offset)
        }
    }

    /** One discrete notch per swipe; never wrap past a finite collection edge. */
    fun next(ids: List<String>, selectedId: String?, delta: Int): String? {
        val index = focusIndex(ids, selectedId)
        return if (index < 0) null else ids[(index + delta.coerceIn(-1, 1)).coerceIn(0, ids.lastIndex)]
    }
}
