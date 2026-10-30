package io.github.gamesbyian.glimpse.domain

/**
 * A one-shot Home request has an independently classifiable result.
 * A queued worker is not an OS acknowledgment, and an Android-side exception
 * after entering WallpaperManager can never be treated as safe to retry.
 */
internal enum class HomeChangeStatus {
    ACKNOWLEDGED,
    NOT_APPLIED,
    UNCERTAIN
}

internal data class HomeChangeOutcome(
    val status: HomeChangeStatus,
    val message: String
)

/** Pure contract used to verify that only Android's positive wallpaper ID
 * can qualify a manual side effect as completed.
 */
internal fun homeChangeStatus(result: WallpaperAttemptResult): HomeChangeStatus =
    when (result) {
        is WallpaperAttemptResult.Acknowledged ->
            if (result.wallpaperId > 0) HomeChangeStatus.ACKNOWLEDGED
            else HomeChangeStatus.UNCERTAIN
        WallpaperAttemptResult.MissingAcknowledgment,
        WallpaperAttemptResult.Interrupted,
        WallpaperAttemptResult.PlatformError -> HomeChangeStatus.UNCERTAIN
        is WallpaperAttemptResult.BeforePlatformError -> HomeChangeStatus.NOT_APPLIED
    }
