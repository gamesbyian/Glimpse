package io.github.gamesbyian.glimpse.domain

/**
 * Bounded five-aperture view onto a finite collection. The number of paper
 * windows never caps the underlying photo count and no phantom duplicates
 * are inserted near either endpoint.
 */
object ReelWindow {
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
