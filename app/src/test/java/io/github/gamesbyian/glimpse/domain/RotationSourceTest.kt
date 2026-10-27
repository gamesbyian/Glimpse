package io.github.gamesbyian.glimpse.domain

import io.github.gamesbyian.glimpse.data.CollectionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationSourceTest {
    private val morning = CollectionRecord("morning", "Morning light", 10)
    private val evening = CollectionRecord("evening", "Evening colours", 20)
    private val collections = listOf(morning, evening)

    @Test fun noRotationCannotInventASource() {
        assertNull(describeRotationSource(null, morning.id, collections))
    }

    @Test fun browsingDifferentCollectionCannotRewriteRotationSource() {
        val original = describeRotationSource(morning.id, evening.id, collections)!!
        assertEquals("Morning light", original.title)
        assertTrue(original.available)
        assertTrue(original.differsFromBrowsing)
    }

    @Test fun choosingExistingSourceReportsNoDisagreement() {
        val source = describeRotationSource(evening.id, evening.id, collections)!!
        assertEquals("Evening colours", source.title)
        assertTrue(source.available)
        assertFalse(source.differsFromBrowsing)
    }

    @Test fun removedSourceIsExplicitlyUnavailable() {
        val missing = describeRotationSource("deleted", morning.id, collections)!!
        assertEquals("Unavailable collection", missing.title)
        assertFalse(missing.available)
        assertTrue(missing.differsFromBrowsing)
    }
}
