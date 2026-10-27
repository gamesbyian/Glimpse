package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseCursorTest {
    @Test fun collectionSwitchRestoresItsOwnSelection() {
        val albumA = listOf("a0", "a1", "shared")
        val albumB = listOf("b0", "shared", "b2")
        val a = BrowseCursor().open("A", "a1")
        assertEquals("a1", a.resolvedId(albumA))
        val b = a.open("B", "b2")
        assertEquals("b2", b.resolvedId(albumB))
        assertEquals("a1", b.open("A", "a1").resolvedId(albumA))
    }

    @Test fun lateTapCannotCrossCollectionsEvenForSharedPhoto() {
        val b = BrowseCursor("B", "b0")
        assertEquals(b, b.select("A", "shared", listOf("b0", "shared")))
        assertEquals(b, b.select("B", "outside", listOf("b0", "shared")))
        assertEquals("shared", b.select("B", "shared", listOf("b0", "shared")).selectedPhotoId)
    }

    @Test fun missingRememberedPhotoFallsBackToRealFirstImage() {
        assertEquals("first", BrowseCursor("A", "deleted").resolvedId(listOf("first", "second")))
        assertEquals(null, BrowseCursor("A", "deleted").resolvedId(emptyList()))
        assertEquals(null, BrowseCursor().open(null, "old").selectedPhotoId)
    }

    @Test fun restoredCollectionFallsBackWithoutFabricatingAnEntry() {
        val collections = listOf("flowers", "rainbows", "skies")
        assertEquals("rainbows", BrowseCursor.restoredCollection(collections, "rainbows"))
        assertEquals("flowers", BrowseCursor.restoredCollection(collections, "deleted"))
        assertEquals("flowers", BrowseCursor.restoredCollection(collections, null))
        assertEquals(null, BrowseCursor.restoredCollection(emptyList(), "deleted"))
    }

    @Test fun finiteStepsClampAtBothEndsAndHandleLargeDeltas() {
        val ids = (0 until 60).map { "p$it" }
        val atStart = BrowseCursor("A", "p0")
        assertEquals("p0", atStart.step(-1000, ids).selectedPhotoId)
        assertEquals("p59", atStart.step(1000, ids).selectedPhotoId)
        assertEquals("p58", atStart.open("A", "p59").step(-1, ids).selectedPhotoId)
        assertEquals(null, atStart.step(1, emptyList()).selectedPhotoId)
        assertEquals("only", atStart.step(Int.MAX_VALUE, listOf("only")).selectedPhotoId)
        assertEquals("p0", atStart.step(Int.MIN_VALUE, ids).selectedPhotoId)
    }
}
