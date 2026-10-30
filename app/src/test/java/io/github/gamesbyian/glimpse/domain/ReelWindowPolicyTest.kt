package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReelWindowPolicyTest {
    @Test fun firstAndLastRemainFiniteWithNoFakeRepetition() {
        assertEquals(ReelWindowRequest(0, 5), ReelWindowPolicy.forFocus(1, 22))
        assertEquals(ReelWindowRequest(0, 5), ReelWindowPolicy.forFocus(3, 22))
        assertEquals(ReelWindowRequest(17, 5), ReelWindowPolicy.forFocus(22, 22))
        assertEquals(ReelWindowRequest(17, 5), ReelWindowPolicy.forFocus(20, 22))
    }

    @Test fun middleAndLargeLibraryOnlyRequestBoundedRows() {
        assertEquals(ReelWindowRequest(8, 5), ReelWindowPolicy.forFocus(11, 22))
        assertEquals(ReelWindowRequest(8997, 5),
            ReelWindowPolicy.forFocus(9000, 12_000))
        assertEquals(ReelWindowRequest(11995, 5),
            ReelWindowPolicy.forFocus(12_000, 12_000))
    }

    @Test fun emptyAndTinyCollectionsHaveNoPhantomSlots() {
        assertEquals(ReelWindowRequest(0, 0), ReelWindowPolicy.forFocus(0, 0))
        assertEquals(ReelWindowRequest(0, 1), ReelWindowPolicy.forFocus(1, 1))
        assertEquals(ReelWindowRequest(0, 3), ReelWindowPolicy.forFocus(2, 3))
        assertEquals(ReelWindowRequest(0, 3), ReelWindowPolicy.forFocus(3, 3))
    }

    @Test fun corruptOrStaleOrdinalsAreRefusedInsteadOfSilentlyWrapping() {
        for (invalid in listOf(0, -1, 23)) {
            assertThrows(IllegalArgumentException::class.java) {
                ReelWindowPolicy.forFocus(invalid, 22)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReelWindowPolicy.forFocus(1, -4)
        }
        for (width in listOf(0, 10, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                ReelWindowPolicy.forFocus(1, 22, width)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReelWindowPolicy.forFocus(1, 0)
        }
    }
}
