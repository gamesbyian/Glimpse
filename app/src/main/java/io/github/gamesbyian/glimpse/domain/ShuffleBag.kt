package io.github.gamesbyian.glimpse.domain

import kotlin.random.Random

/**
 * Pure, testable no-repeat rotation policy. New arrivals join at the next
 * cycle boundary; removing unavailable photos does not skip a valid entry.
 */
object ShuffleBag {
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
