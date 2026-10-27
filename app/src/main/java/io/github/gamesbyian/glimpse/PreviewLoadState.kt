package io.github.gamesbyian.glimpse

/**
 * A decoder result must be tied to the exact photo path that requested it.
 * Otherwise a Compose producer restarting on another path can briefly expose
 * the previous photo underneath the new index/actions while loading.
 */
internal data class PreviewLoadState<T>(
    val sourcePath: String?,
    val image: T? = null,
    val complete: Boolean = false
) {
    fun imageFor(requestedPath: String?): T? =
        image?.takeIf { complete && requestedPath == sourcePath }

    fun isLoading(requestedPath: String?): Boolean =
        requestedPath != null && (requestedPath != sourcePath || !complete)
}
