package io.github.gamesbyian.glimpse

import io.github.gamesbyian.glimpse.data.PhotoRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalPhotoSaveTest {
    private val sha = "ba".repeat(32)
    private fun photo(mime: String = "image/jpeg", id: String = sha) =
        PhotoRecord(id=id,sha256=sha,localPath="/private/fixture",mime=mime,
            width=1,height=1,byteLength=5,importedAt=1)

    @Test fun filenameUsesOnlyCanonicalSourceAndActualMIME() {
        assertEquals("Glimpse-original-" + sha.take(12) + ".jpg",
            OriginalPhotoSave.defaultName(photo()))
        assertEquals("Glimpse-original-" + sha.take(12) + ".png",
            OriginalPhotoSave.defaultName(photo("image/png")))
        assertEquals("Glimpse-original-" + sha.take(12) + ".webp",
            OriginalPhotoSave.defaultName(photo("image/webp")))
    }

    @Test fun identityAndFormatErrorsFailBeforeDocumentChooser() {
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSave.defaultName(photo(id="different"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            OriginalPhotoSave.defaultName(photo(mime="application/octet-stream"))
        }
    }

    @Test fun onlyContentDocumentSchemeIsAccepted() {
        assertTrue(OriginalPhotoSave.acceptsDocumentScheme("content"))
        assertFalse(OriginalPhotoSave.acceptsDocumentScheme("file"))
        assertFalse(OriginalPhotoSave.acceptsDocumentScheme("https"))
        assertFalse(OriginalPhotoSave.acceptsDocumentScheme(null))
    }
}
