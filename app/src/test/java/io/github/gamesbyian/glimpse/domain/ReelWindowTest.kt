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
        val placement = ReelWindow.placement(w, h)
        val cx = placement.centerX
        val cy = placement.centerY
        val radius = placement.radius * .62f
        fun x(deg: Double) = cx + kotlin.math.cos(Math.toRadians(deg)).toFloat() * radius
        fun y(deg: Double) = cy + kotlin.math.sin(Math.toRadians(deg)).toFloat() * radius
        assertEquals(0, ReelWindow.tapOffset(x(-90.0), y(-90.0), w, h))
        assertEquals(1, ReelWindow.tapOffset(x(-18.0), y(-18.0), w, h))
        assertEquals(-1, ReelWindow.tapOffset(x(-162.0), y(-162.0), w, h))
        assertNull(ReelWindow.tapOffset(cx, cy, w, h))
    }
    @Test fun paperRimHubAndFrameGuttersCannotSelectAPhoto() {
        val width = 400f
        val height = 316f
        val p = ReelWindow.placement(width, height)
        fun hit(radius: Float, degrees: Double): Int? {
            val a = Math.toRadians(degrees)
            val x = p.centerX + radius * kotlin.math.cos(a).toFloat()
            val y = p.centerY + radius * kotlin.math.sin(a).toFloat()
            return ReelWindow.tapOffset(x, y, width, height)
        }
        // Actual image windows are inset from a substantial physical paper rim.
        assertNull(hit(p.radius * .34f, -90.0))
        assertNull(hit(p.radius * .91f, -90.0))
        // Half-way between aperture centres is solid cardstock.
        assertNull(hit(p.radius * .62f, -54.0))
        assertEquals(0, hit(p.radius * .62f, -90.0))
        assertEquals(1, hit(p.radius * .62f, -18.0))
        assertEquals(-1, hit(p.radius * .62f, -162.0))
        assertEquals(23f, ReelWindow.FACE_HOLE_HALF_ANGLE)
        assert(ReelWindow.WINDOW_INNER_RADIUS < ReelWindow.FACE_HOLE_INNER_RADIUS)
        assert(ReelWindow.WINDOW_OUTER_RADIUS > ReelWindow.FACE_HOLE_OUTER_RADIUS)
        assert(ReelWindow.WINDOW_HALF_ANGLE > ReelWindow.FACE_HOLE_HALF_ANGLE)
    }

    @Test fun bottomCornerObjectHasVisiblePerimeterAndRealHitArea() {
        val p = ReelWindow.placement(400f, 316f)
        assert(p.centerX > 0f && p.centerX < 400f * .40f)
        assert(p.radius < 400f * .73f)
        assert(p.centerY - p.radius > -12f)
        assert(p.centerX + p.radius < 400f)
        assertEquals(0, ReelWindow.tapOffset(p.centerX,
            p.centerY - p.radius * .62f, 400f, 316f))
    }
    @Test fun unknownSelectionIsStableAtFirstRealAsset() {
        assertEquals(0, ReelWindow.focusIndex(listOf("a", "b"), "missing"))
        assertEquals("b", ReelWindow.next(listOf("a", "b"), "missing", 1))
    }
    @Test fun everyApertureAndPaperSectorCanStartReelRotation() {
        val w = 400f
        val h = 316f
        val p = ReelWindow.placement(w, h)
        for (degree in listOf(-162.0, -90.0, -18.0, 54.0, 126.0)) {
            val a = Math.toRadians(degree)
            val x = p.centerX + p.radius * .62f * kotlin.math.cos(a).toFloat()
            val y = p.centerY + p.radius * .62f * kotlin.math.sin(a).toFloat()
            assert(ReelWindow.acceptsRotationStart(x, y, w, h))
        }
        assert(ReelWindow.acceptsRotationStart(p.centerX, p.centerY, w, h))
        assert(!ReelWindow.acceptsRotationStart(p.centerX + p.radius * 1.2f,
            p.centerY, w, h))
        assert(!ReelWindow.acceptsRotationStart(Float.NaN, p.centerY, w, h))
    }


}
