package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord

/**
 * Starting a different source begins a new bag. Resuming a paused source
 * preserves its unused no-repeat entries, last acknowledged image, and OS
 * state. Only the next *foreground* eligibility time is reset.
 */
internal fun prepareRotationStart(
    previous: RotationRecord,
    requestedCollectionId: String,
    nowMillis: Long,
    intervalMillis: Long
): RotationRecord {
    require(requestedCollectionId.isNotBlank())
    require(intervalMillis > 0)
    val continuation = previous.collectionId == requestedCollectionId
    val nextTime = Math.addExact(nowMillis, intervalMillis)
    return previous.copy(
        collectionId = requestedCollectionId,
        active = true,
        remainingJson = if (continuation) previous.remainingJson else "[]",
        nextEligibleAt = nextTime,
        outcome = if (continuation)
            "Resumed existing no-repeat shuffle; foreground eligibility reset"
        else
            "Started new collection shuffle; foreground eligibility reset"
    )
}
