package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.CollectionRecord

/** Selection and scheduled-shuffle source are independent domains. */
internal data class RotationSource(
    val title: String,
    val available: Boolean,
    val differsFromBrowsing: Boolean
)

/**
 * The source is derived exclusively from the saved rotation collection ID.
 * A user changing the browse cursor must never silently change the source of
 * an active shuffle or the source of the explicit Change Home now command.
 */
internal fun describeRotationSource(
    rotationCollectionId: String?,
    browsingCollectionId: String?,
    collections: List<CollectionRecord>
): RotationSource? {
    if (rotationCollectionId == null) return null
    val source = collections.firstOrNull { it.id == rotationCollectionId }
    return RotationSource(
        title = source?.title ?: "Unavailable collection",
        available = source != null,
        differsFromBrowsing = rotationCollectionId != browsingCollectionId
    )
}
