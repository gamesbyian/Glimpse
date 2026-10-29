package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.RotationRecord

/** Only a positive Android wallpaper ID may acknowledge an image or consume a shuffle reservation. */
internal sealed interface WallpaperAttemptResult {
    data class Acknowledged(val wallpaperId: Int) : WallpaperAttemptResult
    data object MissingAcknowledgment : WallpaperAttemptResult
    data object Interrupted : WallpaperAttemptResult
    data object PlatformError : WallpaperAttemptResult
    data class BeforePlatformError(val exceptionType: String) : WallpaperAttemptResult
}

/** Persist before decode/OS call. The previously acknowledged wallpaper and bag stay untouched. */
internal fun beginWallpaperAttempt(
    previous: RotationRecord, photoId: String, startedAt: Long
): RotationRecord {
    require(photoId.isNotBlank())
    require(previous.pendingPhotoId == null) { "Resolve the previous attempt first." }
    return previous.copy(
        pendingPhotoId = photoId,
        lastAttemptAt = startedAt,
        outcome = "Attempting to apply Home wallpaper"
    )
}

/**
 * Pure durable attempt transition. No Android, Room or clock side effects.
 * A reserved bag is committed only with a confirmed wallpaper ID; direct
 * manual apply supplies null to retain the current shuffle bag.
 *
 * Platform errors, cancellation, or an absent Android ID retain the pending
 * photo and stop automation. Pre-OS errors are safe to clear and defer one
 * cadence without consuming any image. The next deadline is relative to the
 * actual finish time, not an old missed wakeup.
 */
internal fun resolveWallpaperAttempt(
    previous: RotationRecord,
    photoId: String,
    startedAt: Long,
    finishedAt: Long,
    intervalMillis: Long,
    remainingAfterSuccessJson: String?,
    result: WallpaperAttemptResult
): RotationRecord {
    require(photoId.isNotBlank())
    require(previous.pendingPhotoId == null) { "Resolve the previous attempt first." }
    require(intervalMillis > 0L)
    val nextDue = if (previous.active)
        BatterySaverRotationPolicy.deferDue(finishedAt, intervalMillis)
    else previous.nextEligibleAt
    val attempt = previous.copy(lastAttemptAt = startedAt)
    return when (result) {
        is WallpaperAttemptResult.Acknowledged -> {
            require(result.wallpaperId > 0) { "A non-positive wallpaper ID cannot confirm an apply." }
            attempt.copy(
                pendingPhotoId = null,
                lastAcknowledgedPhotoId = photoId,
                lastAcknowledgedAt = finishedAt,
                nextEligibleAt = nextDue,
                remainingJson = remainingAfterSuccessJson ?: previous.remainingJson,
                outcome = "Android acknowledged Home wallpaper update (ID " + result.wallpaperId + ")"
            )
        }
        WallpaperAttemptResult.MissingAcknowledgment -> attempt.copy(
            pendingPhotoId = photoId, active = false,
            outcome = "Wallpaper API returned no confirmed ID; outcome uncertain"
        )
        WallpaperAttemptResult.Interrupted -> attempt.copy(
            pendingPhotoId = photoId, active = false,
            outcome = "Wallpaper attempt interrupted; outcome uncertain. Check Home wallpaper."
        )
        WallpaperAttemptResult.PlatformError -> attempt.copy(
            pendingPhotoId = photoId, active = false,
            outcome = "Wallpaper API error; outcome uncertain. Check Home wallpaper."
        )
        is WallpaperAttemptResult.BeforePlatformError -> attempt.copy(
            pendingPhotoId = null, nextEligibleAt = nextDue,
            outcome = "Apply failed before OS call: " + result.exceptionType
        )
    }
}
