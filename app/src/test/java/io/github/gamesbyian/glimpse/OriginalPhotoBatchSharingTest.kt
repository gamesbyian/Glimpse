package io.github.gamesbyian.glimpse

import io.github.gamesbyian.glimpse.data.PhotoRecord
import org.junit.Assert.assertThrows
import org.junit.Test

class OriginalPhotoBatchSharingTest {
    private fun photo(id: String) = PhotoRecord(
        id, id, "/private/originals/" + id + ".jpg", "image/jpeg",
        1, 1, 100L, 1L
    )

    @Test fun rejectsEmptyAndOversizeBatchesBeforeAndroidIntent() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.requireValidBatch(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.requireValidBatch(
                (1..26).map { photo(it.toString()) }
            )
        }
    }

    @Test fun rejectsRepeatedSourceIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.requireValidBatch(listOf(photo("a"), photo("a")))
        }
    }

    @Test fun includesTwentyFiveUniqueSources() {
        OriginalPhotoSharing.requireValidBatch(
            (1..25).map { photo(it.toString()) }
        )
    }
}
