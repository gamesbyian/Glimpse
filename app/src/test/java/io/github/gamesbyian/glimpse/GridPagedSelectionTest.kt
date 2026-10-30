package io.github.gamesbyian.glimpse

import io.github.gamesbyian.glimpse.data.PhotoRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Photo focus remains an original ID, never an in-memory Grid position. */
class GridPagedSelectionTest {
    private fun photo(id: String) = PhotoRecord(
        id = id, sha256 = id, localPath = "$id.jpg",
        mime = "image/jpeg", width = 128, height = 128,
        byteLength = 200, importedAt = 1L
    )

    @Test fun pagedGridCanPreviewSelectionWithoutCollectingEntireCollection() {
        val chosen = photo("selected-from-page-9000")
        val state = GlimpseUiState(
            grid = true, photos = emptyList(),
            gridPreview = chosen,
            selectedPhotoId = chosen.id
        )
        assertEquals(chosen.id, state.current?.id)
    }

    @Test fun reelUsesActualLoadedOrderAndSelectedPhotoOverLastGridFocus() {
        val a = photo("a")
        val b = photo("b")
        val priorFocus = photo("stale")
        val state = GlimpseUiState(
            grid = false, photos = listOf(a, b),
            gridPreview = priorFocus, selectedPhotoId = b.id
        )
        assertEquals(b.id, state.current?.id)
    }

    @Test fun emptyCollectionHasNoPhantomPhoto() {
        assertNull(GlimpseUiState(grid = true).current)
        assertNull(GlimpseUiState(grid = false).current)
    }

    @Test fun loadedReelCanRecoverFromStaleFocusedIdWithoutInventingItem() {
        val a = photo("only")
        val state = GlimpseUiState(
            grid = false, photos = listOf(a), selectedPhotoId = "removed"
        )
        assertEquals(a.id, state.current?.id)
    }
}
