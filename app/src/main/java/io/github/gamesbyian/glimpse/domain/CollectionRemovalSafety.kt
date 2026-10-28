package io.github.gamesbyian.glimpse.domain

/** Collection cleanup cannot strand originals or discard rotation identity. */
enum class CollectionRemovalSafety {
    SAFE, NOT_FOUND, HAS_EXCLUSIVE_ORIGINALS, OWNS_ROTATION, UNCERTAIN_WALLPAPER
}

internal fun collectionRemovalSafety(
    exists: Boolean,
    exclusiveOriginals: Int,
    ownsRotation: Boolean,
    uncertainWallpaper: Boolean
): CollectionRemovalSafety = when {
    !exists -> CollectionRemovalSafety.NOT_FOUND
    uncertainWallpaper -> CollectionRemovalSafety.UNCERTAIN_WALLPAPER
    ownsRotation -> CollectionRemovalSafety.OWNS_ROTATION
    exclusiveOriginals > 0 -> CollectionRemovalSafety.HAS_EXCLUSIVE_ORIGINALS
    else -> CollectionRemovalSafety.SAFE
}
