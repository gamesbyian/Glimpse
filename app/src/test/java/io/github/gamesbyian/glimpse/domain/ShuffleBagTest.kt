package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleBagTest {
    @Test fun neverRepeatsWithinCycle() {
        val all = listOf("a", "b", "c", "d")
        var bag = ShuffleBag.replenish(all, emptyList(), null, Random(7))
        val seen = mutableSetOf<String>()
        repeat(4) {
            assertTrue(seen.add(bag.first()))
            bag = bag.drop(1)
        }
        assertTrue(bag.isEmpty())
        assertEquals(all.toSet(), seen)
    }

    @Test fun preventsImmediateBoundaryRepeat() {
        val all = listOf("a", "b", "c")
        for (seed in 0..100) {
            val next = ShuffleBag.replenish(all, emptyList(), "b", Random(seed))
            assertFalse(next.first() == "b")
        }
    }

    @Test fun removesMissingAndDoesNotAddNewMidCycle() {
        assertEquals(
            listOf("c"),
            ShuffleBag.replenish(listOf("b", "c", "d"), listOf("a", "c"), "b", Random(3))
        )
    }

    @Test fun emptyAndSingletonDoNotInventExtraPhotos() {
        assertTrue(ShuffleBag.replenish(emptyList(), emptyList(), null).isEmpty())
        assertEquals(listOf("a"), ShuffleBag.replenish(listOf("a"), emptyList(), "a"))
    }
}
