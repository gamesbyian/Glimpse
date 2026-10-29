package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord

/** Explicit precedence is shared by foreground and OS-delayed worker checks. */
internal enum class AutomaticRotationGate {
    PAUSED, UNCERTAIN, NOT_DUE, BATTERY_SAVER, ELIGIBLE
}

/**
 * Pure and deterministic. A false/paused decision never reserves a photo,
 * advances the no-repeat queue or invokes Android. WorkManager must NOT run
 * this as a simulated five-minute clock; it is only an eligibility gate.
 */
internal fun automaticRotationGate(
    rotation: RotationRecord?,
    now: Long,
    batterySaver: Boolean
): AutomaticRotationGate = when {
    rotation?.active != true || rotation.collectionId == null ->
        AutomaticRotationGate.PAUSED
    rotation.pendingPhotoId != null ->
        AutomaticRotationGate.UNCERTAIN
    now < (rotation.nextEligibleAt ?: Long.MAX_VALUE) ->
        AutomaticRotationGate.NOT_DUE
    batterySaver ->
        AutomaticRotationGate.BATTERY_SAVER
    else -> AutomaticRotationGate.ELIGIBLE
}
