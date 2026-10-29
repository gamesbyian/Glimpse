package io.github.gamesbyian.glimpse.domain

import kotlin.random.Random

/**
 * Read-only rehearsal of the *same* no-repeat policy used by WallpaperCommands.
 * Never creates a reservation, saves Room state, decodes image bytes, or sends
 * an Android wallpaper request. This is a policy engine, not a clock or alarm.
 */
internal object ShuffleProjection {
    const val MAX_STEPS = 100

    enum class Confidence {
        /** Still in the previously persisted, unused no-repeat sequence. */
        SAVED_QUEUE,
        /** A seeded illustrative draw after the saved queue is exhausted. */
        ILLUSTRATIVE
    }

    data class Frame(
        val photoId: String,
        val position: Int,
        val confidence: Confidence
    )

    data class Result(
        val frames: List<Frame>,
        val requestedSteps: Int,
        val stoppedBecauseOnePhoto: Boolean,
        val hasIllustrativeFrames: Boolean
    )

    /**
     * Actual random order after an exhausted bag is not predictable because
     * foreground/background wallpaper calls use Random.Default. A fixed seed
     * here makes previews reproducible *without influencing real rotation*.
     * The saved unused bag is not edited; missing IDs are filtered precisely
     * as they are by the production engine.
     */
    fun sample(
        available: Collection<String>,
        remaining: List<String>,
        lastAcknowledged: String?,
        requestedSteps: Int,
        seed: Int
    ): Result {
        require(requestedSteps in 0..MAX_STEPS) {
            "Preview must request between 0 and $MAX_STEPS frames."
        }

        val identities = available.toSet()
        if (requestedSteps == 0 || identities.isEmpty()) {
            return Result(emptyList(), requestedSteps,
                stoppedBecauseOnePhoto = false, hasIllustrativeFrames = false)
        }

        var unused = remaining.filter { it in identities }.distinct()
        var last = lastAcknowledged
        var illustrative = false
        var stoppedSingleton = false
        val generator = Random(seed)
        val sequence = ArrayList<Frame>(requestedSteps)

        for (index in 0 until requestedSteps) {
            // This is the same singleton optimization as WallpaperCommands:
            // don't claim a new change when it would reapply the same photo.
            if (identities.size == 1 && identities.first() == last) {
                stoppedSingleton = true
                break
            }
            val certainty = if (unused.isNotEmpty())
                Confidence.SAVED_QUEUE else Confidence.ILLUSTRATIVE
            if (certainty == Confidence.ILLUSTRATIVE) illustrative = true
            val bag = ShuffleBag.replenish(identities, unused, last, generator)
            val chosen = bag.firstOrNull() ?: break
            sequence.add(Frame(chosen, index + 1, certainty))
            unused = bag.drop(1)
            last = chosen
        }
        return Result(sequence, requestedSteps, stoppedSingleton, illustrative)
    }
}
