package io.github.gamesbyian.glimpse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OriginalPhotoSharingTest {
    private val hash = "ab".repeat(32)

    @Test fun onlyImporterNamingContractIsExposed() {
        assertEquals(hash + ".jpg", OriginalPhotoSharing.expectedName(hash, hash, "image/jpeg"))
        assertEquals(hash + ".png", OriginalPhotoSharing.expectedName(hash, hash, "image/png"))
        assertEquals(hash + ".webp", OriginalPhotoSharing.expectedName(hash, hash, "image/webp"))
    }

    @Test fun conflictingOrNonCanonicalIdsFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.expectedName("other", hash, "image/jpeg")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.expectedName("../".repeat(20), "../".repeat(20), "image/png")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.expectedName(hash.uppercase(), hash.uppercase(), "image/webp")
        }
    }

    @Test fun unsupportedFormatsCannotBeShared() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSharing.expectedName(hash, hash, "application/octet-stream")
        }
    }
}
