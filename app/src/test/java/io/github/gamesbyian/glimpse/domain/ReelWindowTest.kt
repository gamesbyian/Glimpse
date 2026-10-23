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
    @Test fun hitTestingMatchesRenderedApertures() {
        val w = 400f
        val h = 316f
        val cx = w / 2f
        val cy = h * .82f
        val radius = w * .84f * .62f
        fun x(deg: Double) = cx + kotlin.math.cos(Math.toRadians(deg)).toFloat() * radius
        fun y(deg: Double) = cy + kotlin.math.sin(Math.toRadians(deg)).toFloat() * radius
        assertEquals(0, ReelWindow.tapOffset(x(-90.0), y(-90.0), w, h))
        assertEquals(1, ReelWindow.tapOffset(x(-18.0), y(-18.0), w, h))
        assertEquals(-1, ReelWindow.tapOffset(x(-162.0), y(-162.0), w, h))
        assertNull(ReelWindow.tapOffset(cx, cy, w, h))
    }
    @Test fun unknownSelectionIsStableAtFirstRealAsset() {
        assertEquals(0, ReelWindow.focusIndex(listOf("a", "b"), "missing"))
        assertEquals("b", ReelWindow.next(listOf("a", "b"), "missing", 1))
    }
}
