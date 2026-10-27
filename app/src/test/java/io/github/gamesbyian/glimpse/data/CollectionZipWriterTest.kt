package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class CollectionZipWriterTest {
    private fun photo(root: java.io.File, bytes: ByteArray): PhotoRecord {
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val file = root.resolve("$sha.jpg")
        file.writeBytes(bytes)
        return PhotoRecord(sha, sha, file.absolutePath, "image/jpeg",
            1, 1, bytes.size.toLong(), 1L)
    }

    private fun entries(bytes: ByteArray): Map<String, ByteArray> {
        val found = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                found[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return found
    }

    @Test fun streamsExactOriginalsWithPathSafeManifest() {
        val root = Files.createTempDirectory("glimpse-zip").toFile()
        try {
            val one = byteArrayOf(1, 2, 3, 4)
            val two = byteArrayOf(8, 6, 4, 2, 0)
            val originals = listOf(photo(root, one), photo(root, two))
            val output = ByteArrayOutputStream()
            val report = CollectionZipWriter.write(output, root, "Quote \" & <title>", originals)
            assertEquals(2, report.count)
            assertEquals(9, report.bytes)
            val files = entries(output.toByteArray())
            assertEquals(3, files.size)
            assertArrayEquals(one, files.getValue("originals/${originals[0].sha256}.jpg"))
            assertArrayEquals(two, files.getValue("originals/${originals[1].sha256}.jpg"))
            val manifest = files.getValue("manifest.json").toString(Charsets.UTF_8)
            assertTrue(manifest.contains("glimpse.collection.originals"))
            assertTrue(manifest.contains("Quote \\\" & <title>"))
            assertFalse(manifest.contains(root.absolutePath))
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsCorruptSourcesBeforeWritingZipPrefix() {
        val root = Files.createTempDirectory("glimpse-corrupt-zip").toFile()
        try {
            val record = photo(root, byteArrayOf(1, 1, 1))
            root.resolve("${record.sha256}.jpg").writeBytes(byteArrayOf(0, 0, 0))
            val output = ByteArrayOutputStream()
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipWriter.write(output, root, "Damaged", listOf(record))
            }
            assertEquals(0, output.size())
        } finally { root.deleteRecursively() }
    }

    @Test fun detectsChangesAfterPreflightDuringStreaming() {
        val root = Files.createTempDirectory("glimpse-racing-zip").toFile()
        try {
            val original = photo(root, byteArrayOf(1, 2, 3, 4))
            var checkpoints = 0
            val output = ByteArrayOutputStream()
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipWriter.write(output, root, "Race", listOf(original)) {
                    checkpoints++
                    if (checkpoints == 3) {
                        root.resolve("${original.sha256}.jpg").writeBytes(
                            byteArrayOf(9, 9, 9, 9)
                        )
                    }
                }
            }
            assertTrue(output.size() > 0) // partial archive: never report success
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesDuplicatePathsAndSanitizesOutputName() {
        val root = Files.createTempDirectory("glimpse-dup-zip").toFile()
        try {
            val record = photo(root, byteArrayOf(5, 5, 5))
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipWriter.write(ByteArrayOutputStream(), root, "X", listOf(record, record))
            }
            assertEquals("glimpse-Rainbows-Room-originals.zip",
                CollectionZipWriter.defaultName("  Rainbows / Room  "))
        } finally { root.deleteRecursively() }
    }
}
