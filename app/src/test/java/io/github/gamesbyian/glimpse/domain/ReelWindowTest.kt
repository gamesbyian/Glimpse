package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReelWindowTest {
    @Test fun noPhotosCreatesFiveEmptyWindows() {
        assertEquals(listOf(null, null, null, null, null), ReelWindow.slots(emptyList(), null))
        assertNull(ReelWindow.next(emptyList(), null, 1))
    }
    @Test fun singletonNeverForgesCopies() {
        assertEquals(listOf(null, null, "p0", null, null), ReelWindow.slots(listOf("p0"), "p0"))
        assertEquals("p0", ReelWindow.next(listOf("p0"), "p0", 1))
    }
    @Test fun threeItemsUseOnlyThreeRealWindows() {
        assertEquals(listOf(null, "a", "b", "c", null), ReelWindow.slots(listOf("a", "b", "c"), "b"))
    }
    @Test fun allTwentyTwoItemsAreReachableWithoutWrapping() {
        val list = (1..22).map { "item-$it" }
        var id: String? = list.first()
        val visited = mutableListOf<String>()
        repeat(27) {
            visited.add(id!!)
            id = ReelWindow.next(list, id, 1)
        }
        assertEquals(list, visited.distinct())
        assertEquals(list.last(), id)
        assertEquals(listOf(list[19], list[20], list[21], null, null), ReelWindow.slots(list, id))
        assertEquals(list[20], ReelWindow.next(list, id, -1))
    }
    @Test fun unknownSelectionIsStableAtFirstRealAsset() {
        assertEquals(0, ReelWindow.focusIndex(listOf("a", "b"), "missing"))
        assertEquals("b", ReelWindow.next(listOf("a", "b"), "missing", 1))
    }
}
