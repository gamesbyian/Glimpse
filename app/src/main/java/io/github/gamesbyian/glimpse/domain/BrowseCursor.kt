package io.github.gamesbyian.glimpse.domain

/**
 * Immutable per-collection browsing cursor. IDs, not picture indices or angles,
 * survive a change of view, a new import, or a remembered collection.
 *
 * This is preview-only state. No operation here applies a wallpaper.
 */
data class BrowseCursor(
    val collectionId: String? = null,
    val selectedPhotoId: String? = null
) {
    fun open(collectionId: String?, recalledPhotoId: String?): BrowseCursor =
        BrowseCursor(collectionId, if (collectionId == null) null else recalledPhotoId)

    /** Ignore late taps or animation callbacks for another collection. */
    fun select(sourceCollectionId: String, photoId: String, visibleIds: List<String>): BrowseCursor =
        if (collectionId != sourceCollectionId || photoId !in visibleIds) this
        else copy(selectedPhotoId = photoId)

    fun resolvedId(visibleIds: List<String>): String? =
        selectedPhotoId?.takeIf { it in visibleIds } ?: visibleIds.firstOrNull()

    /** Finite, saturating navigation, including an empty collection. */
    fun step(delta: Int, visibleIds: List<String>): BrowseCursor {
        if (visibleIds.isEmpty()) return copy(selectedPhotoId = null)
        val index = visibleIds.indexOf(resolvedId(visibleIds)).coerceAtLeast(0)
        val next = (index.toLong() + delta.toLong())
            .coerceIn(0L, visibleIds.lastIndex.toLong()).toInt()
        return copy(selectedPhotoId = visibleIds[next])
    }
}
