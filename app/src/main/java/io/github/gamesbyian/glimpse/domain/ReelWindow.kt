package io.github.gamesbyian.glimpse.domain

/**
 * Bounded five-aperture view onto a finite collection. The number of paper
 * windows never caps the underlying photo count and no phantom duplicates
 * are inserted near either endpoint.
 */
object ReelWindow {
    /**
     * Authoritative dimensions for the photograph openings in the paper object.
     * Photo clips extend slightly beyond the opaque-face alpha edges so a
     * rotation cannot reveal colored paper or bleed at the aperture borders.
     * The baked material source uses its own inset edges as documented.
     */
    const val WINDOW_INNER_RADIUS = .39f
    const val WINDOW_OUTER_RADIUS = .87f
    const val WINDOW_HALF_ANGLE = 26f
    const val FACE_HOLE_INNER_RADIUS = .425f
    const val FACE_HOLE_OUTER_RADIUS = .835f
    const val FACE_HOLE_HALF_ANGLE = 23f

    /**
     * Return the tapped aperture offset for the currently drawn cropped
     * wheel, or null when touching paper outside the photo ring. This is
     * geometry only; callers still check that a real photo occupies the slot.
     */
    /** One geometric source of truth shared by drawing and touch hit testing.
     * The large disc is docked off the bottom-left edge, with a readable curved
     * top/right perimeter rather than a full-width strip of wedges.
     */
    data class Placement(val centerX: Float, val centerY: Float, val radius: Float)
    fun placement(width: Float, height: Float): Placement =
        Placement(width * .27f, height * .87f, minOf(width * .69f, height))

    /**
     * The photographic aperture is part of the same physical turnable disc as
     * the surrounding paper. Never reserve rotation only for opaque pixels.
     */
    fun acceptsRotationStart(x: Float, y: Float, width: Float, height: Float): Boolean {
        if (!x.isFinite() || !y.isFinite() || width <= 0f || height <= 0f) return false
        val p = placement(width, height)
        return kotlin.math.hypot(x - p.centerX, y - p.centerY) <= p.radius
    }

    fun tapOffset(x: Float, y: Float, width: Float, height: Float): Int? {
        if (width <= 0f || height <= 0f) return null
        val p = placement(width, height)
        val dx = x - p.centerX
        val dy = y - p.centerY
        val radius = kotlin.math.hypot(dx, dy)
        val outer = p.radius
        if (radius < outer * FACE_HOLE_INNER_RADIUS || radius > outer * FACE_HOLE_OUTER_RADIUS) return null
        val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
        val relative = (angle + 90f + 180f + 360f) % 360f - 180f
        val offset = kotlin.math.round(relative / 72f).toInt()
        return offset.takeIf { it in -2..2 && kotlin.math.abs(relative - it * 72f) <= FACE_HOLE_HALF_ANGLE }
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
