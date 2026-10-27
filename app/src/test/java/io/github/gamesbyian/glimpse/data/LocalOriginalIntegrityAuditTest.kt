package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class LocalOriginalIntegrityAuditTest {
    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun registered(root: File, content: ByteArray): PhotoRecord {
        val sha = hash(content)
        val original = File(root, "$sha.png")
        original.writeBytes(content)
        return PhotoRecord(
            id = sha, sha256 = sha, localPath = original.absolutePath,
            mime = "image/png", width = 1, height = 1,
            byteLength = content.size.toLong(), importedAt = 1
        )
    }

    @Test fun checksRegisteredRecordsAndCountsCorruptionWithoutDeleting() {
        val root = Files.createTempDirectory("glimpse-audit").toFile()
        try {
            val good = registered(root, byteArrayOf(1, 3, 5, 7))
            val damaged = registered(root, byteArrayOf(2, 4, 6, 8))
            File(damaged.localPath).writeBytes(byteArrayOf(0, 0, 0, 0))
            val report = inspectRegisteredOriginals(root, listOf(good, damaged))
            assertEquals(2, report.registered)
            assertEquals(1, report.verified)
            assertEquals(1, report.problemCount)
            assertEquals(4, File(damaged.localPath).length())
        } finally { root.deleteRecursively() }
    }

    @Test fun emptyDatabaseNeedsNoOriginalDirectory() {
        val root = File(Files.createTempDirectory("glimpse-empty-audit").toFile(), "absent")
        val report = inspectRegisteredOriginals(root, emptyList())
        assertEquals(0, report.registered)
        assertEquals(0, report.problemCount)
        root.parentFile?.delete()
    }

    @Test fun linkedRootIsUnavailableNotVerified() {
        val parent = Files.createTempDirectory("glimpse-audit-link")
        val actual = Files.createDirectory(parent.resolve("actual"))
        val linked = parent.resolve("linked")
        try {
            val photo = registered(actual.toFile(), byteArrayOf(10, 20))
            Files.createSymbolicLink(linked, actual)
            assertThrows(IllegalArgumentException::class.java) {
                inspectRegisteredOriginals(linked.toFile(), listOf(photo))
            }
        } finally {
            Files.deleteIfExists(linked)
            actual.toFile().deleteRecursively()
            Files.deleteIfExists(parent)
        }
    }

    @Test fun checkCancellationIsNotSwallowed() {
        val root = Files.createTempDirectory("glimpse-audit-stop").toFile()
        try {
            val photo = registered(root, byteArrayOf(11, 22))
            assertThrows(IllegalStateException::class.java) {
                inspectRegisteredOriginals(root, listOf(photo)) {
                    throw IllegalStateException("cancel-check")
                }
            }
        } finally { root.deleteRecursively() }
    }
}
