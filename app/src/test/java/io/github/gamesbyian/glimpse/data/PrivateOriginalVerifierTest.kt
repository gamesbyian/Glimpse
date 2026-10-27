package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class PrivateOriginalVerifierTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun fixture(root: File, data: ByteArray = "verified bytes".toByteArray()):
        Pair<File, PhotoRecord> {
        val hash = MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }
        val file = File(root, hash + ".jpg").apply { writeBytes(data) }
        return file to PhotoRecord(
            id = hash, sha256 = hash, localPath = file.absolutePath,
            mime = "image/jpeg", width = 10, height = 10,
            byteLength = data.size.toLong(), importedAt = 1000L
        )
    }

    @Test fun validImmutableOriginalCanBeUsedForSharingOrWallpaper() {
        val folder = temporary.newFolder("originals")
        val (file, photo) = fixture(folder)
        assertEquals(file, verifyPrivateOriginal(folder, photo))
    }

    @Test fun sameLengthReplacementMustNotBeAppliedOrShared() {
        val folder = temporary.newFolder("originals")
        val (file, photo) = fixture(folder)
        file.writeBytes(ByteArray(file.length().toInt()) { 55 })
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(folder, photo)
        }
        assertTrue(file.isFile)
    }

    @Test fun mismatchedMetadataAndExternalPathsFailClosed() {
        val folder = temporary.newFolder("originals")
        val (_, photo) = fixture(folder)
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(folder, photo.copy(mime = "image/png"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(folder, photo.copy(id = "unexpected"))
        }
        val outside = temporary.newFolder("other")
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(folder, photo.copy(localPath = File(outside, "photo.jpg").path))
        }
    }

    @Test fun rejectsLinkedRootOrFileEvenIfContentMatches() {
        val folder = temporary.newFolder("originals")
        val (file, photo) = fixture(folder)
        val rootAlias = File(temporary.root, "root-alias")
        Files.createSymbolicLink(rootAlias.toPath(), folder.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(rootAlias, photo)
        }
        val second = temporary.newFolder("with-link")
        val alias = File(second, file.name)
        Files.createSymbolicLink(alias.toPath(), file.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            verifyPrivateOriginal(second, photo.copy(localPath = alias.path))
        }
    }
}
