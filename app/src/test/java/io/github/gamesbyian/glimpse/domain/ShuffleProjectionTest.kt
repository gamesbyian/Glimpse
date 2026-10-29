package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleProjectionTest {
    @Test fun persistedQueueIsShownExactlyWithoutReorderingOrConsumingIt() {
        val saved = mutableListOf("p3", "p1")
        val forecast = ShuffleProjection.sample(
            available = listOf("p1", "p2", "p3"),
            remaining = saved, lastAcknowledged = "p2",
            requestedSteps = 2, seed = 17
        )
        assertEquals(listOf("p3", "p1"), forecast.frames.map { it.photoId })
        assertTrue(forecast.frames.all {
            it.confidence == ShuffleProjection.Confidence.SAVED_QUEUE
        })
        assertFalse(forecast.hasIllustrativeFrames)
        assertEquals(listOf("p3", "p1"), saved)
    }

    @Test fun lostOriginalsAreFilteredAndNewArrivalsWaitForNextCycle() {
        val preview = ShuffleProjection.sample(
            available = listOf("p2", "p3", "new"),
            remaining = listOf("removed", "p2", "p2", "p3"),
            lastAcknowledged = "p1", requestedSteps = 3, seed = 43
        )
        assertEquals("p2", preview.frames[0].photoId)
        assertEquals("p3", preview.frames[1].photoId)
        assertEquals(ShuffleProjection.Confidence.SAVED_QUEUE,
            preview.frames[1].confidence)
        assertEquals(ShuffleProjection.Confidence.ILLUSTRATIVE,
            preview.frames[2].confidence)
        assertTrue(preview.hasIllustrativeFrames)
    }

    @Test fun simulatedCyclesAreSeededAndDoNotRepeatAtBoundary() {
        val a = ShuffleProjection.sample(
            listOf("a", "b", "c"), emptyList(), "a", 39, 73
        )
        val b = ShuffleProjection.sample(
            listOf("a", "b", "c"), emptyList(), "a", 39, 73
        )
        assertEquals(a, b)
        assertEquals(39, a.frames.size)
        for ((index, frame) in a.frames.withIndex()) {
            if (index > 0) assertTrue(frame.photoId != a.frames[index - 1].photoId)
            if (index % 3 == 0) {
                val next = a.frames.drop(index).take(3).map { it.photoId }
                assertEquals(3, next.toSet().size)
            }
        }
    }

    @Test fun singletonStopsOnceNoNewWallpaperCouldBeShown() {
        val alreadyApplied = ShuffleProjection.sample(
            listOf("one"), emptyList(), "one", 12, 5
        )
        assertTrue(alreadyApplied.frames.isEmpty())
        assertTrue(alreadyApplied.stoppedBecauseOnePhoto)

        val first = ShuffleProjection.sample(
            listOf("one"), emptyList(), null, 12, 5
        )
        assertEquals(listOf("one"), first.frames.map { it.photoId })
        assertTrue(first.stoppedBecauseOnePhoto)
    }

    @Test fun noSourcesZeroStepsAndExcessivePreviewAreBounded() {
        assertTrue(ShuffleProjection.sample(emptyList(), emptyList(),
            null, 10, 1).frames.isEmpty())
        assertTrue(ShuffleProjection.sample(listOf("a"), emptyList(),
            null, 0, 1).frames.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            ShuffleProjection.sample(listOf("a"), emptyList(),
                null, ShuffleProjection.MAX_STEPS + 1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShuffleProjection.sample(listOf("a"), emptyList(),
                null, -1, 1)
        }
    }
}
