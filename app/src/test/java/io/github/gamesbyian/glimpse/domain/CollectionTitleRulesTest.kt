package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CollectionTitleRulesTest {
    @Test fun trimPreservesNormalUnicodeAndVisibleText() {
        assertEquals("Travel & Earth", checkedCollectionTitle("  Travel & Earth \n"))
        assertEquals("雨と虹", checkedCollectionTitle("雨と虹"))
    }

    @Test fun emptyAndWhitespaceTitlesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            checkedCollectionTitle("  \n \t ")
        }
    }

    @Test fun boundAndControlRejectionProtectAgainstNonReadableNames() {
        assertEquals("A".repeat(80), checkedCollectionTitle("A".repeat(80)))
        assertThrows(IllegalArgumentException::class.java) {
            checkedCollectionTitle("A".repeat(81))
        }
        assertThrows(IllegalArgumentException::class.java) {
            checkedCollectionTitle("foo\u0000bar")
        }
    }
}
