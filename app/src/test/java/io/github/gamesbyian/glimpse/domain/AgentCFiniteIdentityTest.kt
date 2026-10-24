package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Production C: deterministic domain proofs. These do not establish actual
 * Compose layout/gestures, 200%-text usability, TalkBack, or OS wallpaper.
 */
class AgentCFiniteIdentityTest {
    @Test fun finiteDatasetMatrixHasNoPhantomSlotsOrWrapping() {
        for (size in listOf(0, 1, 3, 5, 22, 60)) {
            val ids = (1..size).map { "sha256-stable-id-%02d".format(it) }
            if (size == 0) {
                assertEquals(List<String?>(5) { null }, ReelWindow.slots(ids, null))
                assertNull(ReelWindow.next(ids, null, 1))
                continue
            }
            assertEquals(ids.first(), ReelWindow.next(ids, ids.first(), -1))
            assertEquals(ids.last(), ReelWindow.next(ids, ids.last(), 1))
            val visited = mutableListOf<String>()
            var current: String? = ids.first()
            repeat(size + 4) {
                visited.add(current!!)
                current = ReelWindow.next(ids, current, 1)
            }
            assertEquals("all IDs reachable for size=" + size, ids, visited.distinct())
            for (index in ids.indices) {
                val focused = ids[index]
                val slots = ReelWindow.slots(ids, focused)
                assertEquals("five stable apertures", 5, slots.size)
                assertEquals(focused, slots[2])
                assertEquals(slots.filterNotNull().distinct().size, slots.count { it != null })
                assertEquals((index - 2..index + 2).map(ids::getOrNull), slots)
                assertFalse("invalid photo ID minted", slots.filterNotNull().any { it !in ids })
            }
        }
    }

    @Test fun stepBackwardAfterForwardRestoresExactIdentity() {
        val ids = (1..60).map { "asset-" + (it * 37) }
        for (index in 1 until ids.lastIndex) {
            val after = ReelWindow.next(ids, ids[index], 1)
            val back = ReelWindow.next(ids, after, -1)
            assertEquals("identity changed at index=" + index, ids[index], back)
        }
    }

    @Test fun staleSelectionFallsBackToRealFirstPhotoOnly() {
        val ids = listOf("p10", "p11", "p12")
        assertEquals(ids.first(), ReelWindow.next(ids, "deleted-id", 0))
        assertEquals(listOf(null, null, "p10", "p11", "p12"),
            ReelWindow.slots(ids, "deleted-id"))
        assertTrue(ReelWindow.focusIndex(emptyList(), "deleted") < 0)
    }
}
