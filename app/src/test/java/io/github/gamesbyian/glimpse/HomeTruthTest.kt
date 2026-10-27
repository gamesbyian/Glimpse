package io.github.gamesbyian.glimpse

import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTruthTest {
    private fun photo(id: String) = PhotoRecord(
        id = id,
        sha256 = id,
        localPath = "/private/originals/" + id + ".jpg",
        mime = "image/jpeg",
        width = 480,
        height = 720,
        byteLength = 4096,
        importedAt = 100L
    )

    @Test fun browsingAnotherCollectionDoesNotOverwriteAcknowledgedHomePhoto() {
        val applied = photo("acknowledged-photo")
        val previewed = photo("browse-only-photo")
        val state = GlimpseUiState(
            photos = listOf(previewed),
            selectedPhotoId = previewed.id,
            acknowledgedPhoto = applied,
            rotation = RotationRecord(lastAcknowledgedPhotoId = applied.id)
        )
        assertEquals(previewed.id, state.current?.id)
        assertEquals(applied.id, state.acknowledgedPhoto?.id)
    }

    @Test fun neverExposeStalePhotoForAnotherAcknowledgment() {
        val old = photo("old-photo")
        val current = photo("new-photo")
        assertNull(matchedAcknowledgedPhoto(null, old))
        assertNull(matchedAcknowledgedPhoto(RotationRecord(), old))
        assertNull(matchedAcknowledgedPhoto(
            RotationRecord(lastAcknowledgedPhotoId = current.id), old
        ))
        assertEquals(current, matchedAcknowledgedPhoto(
            RotationRecord(lastAcknowledgedPhotoId = current.id), current
        ))
    }
}
