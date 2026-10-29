package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord

/** A tile never starts a shuffle or bypasses the unresolved Android outcome gate. */
internal enum class QuickTileAvailability {
    NO_SOURCE, UNCERTAIN, READY
}

internal fun quickTileAvailability(rotation: RotationRecord?): QuickTileAvailability = when {
    rotation?.pendingPhotoId != null -> QuickTileAvailability.UNCERTAIN
    rotation?.collectionId.isNullOrBlank() -> QuickTileAvailability.NO_SOURCE
    else -> QuickTileAvailability.READY
}
