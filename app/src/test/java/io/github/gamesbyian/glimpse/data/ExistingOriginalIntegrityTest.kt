package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ExistingOriginalIntegrityTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun matchingByteIdenticalExistingOriginalIsReusable() {
        val bytes = byteArrayOf(0, 1, 2, 3, -1, -2, 7, 42)
        val original = temporary.newFile("imported.jpg")
        original.writeBytes(bytes)
        assertTrue(existingOriginalMatches(original, digest(bytes), bytes.size.toLong()))
    }

    @Test fun byteChangeWithSameLengthMustNotPass() {
        val bytes = ByteArray(8192) { it.toByte() }
        val original = temporary.newFile("imported.jpg")
        original.writeBytes(bytes)
        val expected = digest(bytes)
        original.writeBytes(bytes.also { it[4000] = 0 })
        assertFalse(existingOriginalMatches(original, expected, original.length()))
    }

    @Test fun wrongLengthMissingAndNonCanonicalHashFailClosed() {
        val original = temporary.newFile("imported.png")
        val bytes = "a verified original".toByteArray()
        original.writeBytes(bytes)
        val hash = digest(bytes)
        assertFalse(existingOriginalMatches(original, hash, bytes.size.toLong() - 1))
        assertFalse(existingOriginalMatches(original, hash, 0))
        assertFalse(existingOriginalMatches(File(original.parentFile, "missing.jpg"), hash, bytes.size.toLong()))
        assertFalse(existingOriginalMatches(original, hash.uppercase(), bytes.size.toLong()))
    }

    @Test fun aSymbolicLinkCannotStandInForAPrivateOriginal() {
        val bytes = "original kept elsewhere".toByteArray()
        val elsewhere = temporary.newFile("real.jpg").apply { writeBytes(bytes) }
        val alias = File(temporary.root, "link.jpg")
        java.nio.file.Files.createSymbolicLink(alias.toPath(), elsewhere.toPath())
        assertFalse(existingOriginalMatches(alias, digest(bytes), bytes.size.toLong()))
    }
}
