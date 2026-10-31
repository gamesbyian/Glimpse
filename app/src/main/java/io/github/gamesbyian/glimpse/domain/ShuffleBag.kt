package io.github.gamesbyian.glimpse.domain

import kotlin.random.Random

/**
 * Pure, testable no-repeat rotation policy. New arrivals join at the next
 * cycle boundary; removing unavailable photos does not skip a valid entry.
 */
object ShuffleBag {
    /**
     * Explicitly move a known next preview to the tail of the saved cycle.
     * The skipped photo is retained rather than silently excluded forever;
     * Home wallpaper and timing are untouched. No random new candidate is
     * invented and no pending Android acknowledgment is consumed.
     *
     * Returning null means there is no *known different* next candidate.
     */
    fun skipKnownNext(
        available: Collection<String>,
        remaining: List<String>,
        lastAcknowledged: String?,
        expectedHead: String
    ): List<String>? {
        val valid = available.toSet()
        val retained = remaining.filter { it in valid }.distinct()
        // A tap can arrive after the displayed queue has changed.
        if (retained.size < 2 || retained.first() != expectedHead) return null
        if (retained[1] == lastAcknowledged) return null
        return retained.drop(1) + retained.first()
    }

    fun replenish(
        available: Collection<String>,
        remaining: List<String>,
        lastAcknowledged: String?,
        random: Random = Random.Default
    ): List<String> {
        val valid = available.toSet()
        val retained = remaining.filter { it in valid }.distinct()
        if (retained.isNotEmpty()) return retained
        val shuffled = valid.shuffled(random)
        if (shuffled.size < 2 || shuffled.first() != lastAcknowledged) return shuffled
        val swap = shuffled.indexOfFirst { it != lastAcknowledged }
        return shuffled.toMutableList().apply {
            val first = this[0]
            this[0] = this[swap]
            this[swap] = first
        }
    }
}
