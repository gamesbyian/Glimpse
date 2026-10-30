package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord

/**
 * Read-only widget truth: a saved unused bag can establish an upcoming image;
 * an exhausted bag cannot, because live rotation uses Random.Default.
 */
internal enum class WidgetPreviewKind {
    NO_SOURCE, UNCERTAIN, NEXT_KNOWN, NEXT_UNDECIDED
}

internal data class WidgetPreviewDecision(
    val kind: WidgetPreviewKind,
    val knownNextId: String? = null,
    val canRequestChange: Boolean = false
)

/**
 * [knownValidQueueHead] is either a membership-validated candidate that the
 * existing saved no-repeat bag would use next, or null. No random sampling or
 * side effect is permissible when a widget is merely being drawn.
 */
internal fun widgetPreviewDecision(
    rotation: RotationRecord?,
    sourceExists: Boolean,
    knownValidQueueHead: String?
): WidgetPreviewDecision = when {
    rotation?.pendingPhotoId != null -> WidgetPreviewDecision(
        WidgetPreviewKind.UNCERTAIN
    )
    rotation?.collectionId.isNullOrBlank() || !sourceExists -> WidgetPreviewDecision(
        WidgetPreviewKind.NO_SOURCE
    )
    !knownValidQueueHead.isNullOrBlank() -> WidgetPreviewDecision(
        WidgetPreviewKind.NEXT_KNOWN, knownValidQueueHead, canRequestChange = true
    )
    else -> WidgetPreviewDecision(
        WidgetPreviewKind.NEXT_UNDECIDED, canRequestChange = true
    )
}
