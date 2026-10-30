package io.github.gamesbyian.glimpse.domain

/** Bounded SQL viewport for a physical five-aperture Reel, zero-indexed. */
internal data class ReelWindowRequest(val offset: Int, val limit: Int)

/**
 * Pure viewport policy. The selected photo's 1-based SQL ordinal is the
 * authority, not a guessed in-memory adapter index. At the first/last
 * image the real window shifts inward instead of inventing repeated photos.
 *
 * This is the query foundation only: switching live Reel rendering from
 * the finite list requires its own full visual/touch acceptance.
 */
internal object ReelWindowPolicy {
    const val MAX_WINDOW = 9

    fun forFocus(
        positionOneBased: Int,
        total: Int,
        requestedWidth: Int = 5
    ): ReelWindowRequest {
        require(total >= 0) { "Collection size cannot be negative" }
        require(requestedWidth in 1..MAX_WINDOW) {
            "Window width must be in 1..$MAX_WINDOW"
        }
        if (total == 0) {
            require(positionOneBased == 0)
            return ReelWindowRequest(offset = 0, limit = 0)
        }
        require(positionOneBased in 1..total) {
            "Focus ordinal must belong to the collection"
        }
        val limit = minOf(requestedWidth, total)
        val centered = (positionOneBased - 1) - (limit / 2)
        return ReelWindowRequest(
            offset = centered.coerceIn(0, total - limit),
            limit = limit
        )
    }
}
