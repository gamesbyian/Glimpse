package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CollectionZipReaderTest {
    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()
    private fun fixture(root: File, content: ByteArray): PhotoRecord {
        val sha = MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }
        val file = File(root, sha + ".jpg")
        file.writeBytes(content)
        return PhotoRecord(sha, sha, file.absolutePath, "image/jpeg", 1, 1,
            content.size.toLong(), 1)
    }
    private fun archive(root: File, photos: List<PhotoRecord>): ByteArray {
        val output = ByteArrayOutputStream()
        CollectionZipWriter.write(output, root, "Restored photographs", photos)
        return output.toByteArray()
    }
    private fun crafted(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
    @Test fun roundTripWriterReaderPreservesAllSourceBytes() {
        val root = Files.createTempDirectory("glimpse-restore-").toFile()
        try {
            val a = bytes(1,2,3)
            val b = bytes(5,6,7,8,9)
            val photos = listOf(fixture(root, a), fixture(root, b))
            val decoded = CollectionZipReader.stage(
                ByteArrayInputStream(archive(root, photos)), root
            )
            assertEquals("Restored photographs", decoded.title)
            assertEquals(2, decoded.photos.size)
            assertArrayEquals(a, decoded.photos[0].temporaryFile.readBytes())
            assertArrayEquals(b, decoded.photos[1].temporaryFile.readBytes())
            decoded.discardStaging()
            assertTrue(root.listFiles()!!.none { it.name.endsWith(".partial") })
        } finally { root.deleteRecursively() }
    }
    @Test fun maliciousPathAndExtraEntryAreRejectedWithoutStagingResidue() {
        val root = Files.createTempDirectory("glimpse-zip-badpath").toFile()
        try {
            val payload = """{"format":"glimpse.collection.originals","version":1,"collectionTitle":"Test","photoCount":1,"photos":[{"path":"../escape.jpg","sha256":"abababababababababababababababababababababababababababababababab","mime":"image/jpeg","bytes":3}]}"""
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipReader.stage(
                    ByteArrayInputStream(crafted("manifest.json" to payload.toByteArray())),
                    root
                )
            }
            val pic = fixture(root, bytes(1,3,5))
            val legitimate = archive(root, listOf(pic))
            val appended = crafted("manifest.json" to """{"format":"glimpse.collection.originals","version":1,"collectionTitle":"Test","photoCount":0,"photos":[]}""".toByteArray(), "originals/extra.jpg" to bytes(1))
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipReader.stage(ByteArrayInputStream(appended), root)
            }
            assertFalse(legitimate.isEmpty())
            assertTrue(root.listFiles()!!.none { it.name.endsWith(".partial") })
        } finally { root.deleteRecursively() }
    }
    @Test fun corruptedPayloadDoesNotPublishPartiallyValidatedCopies() {
        val root = Files.createTempDirectory("glimpse-zip-corrupt").toFile()
        try {
            val first = bytes(10,11,12)
            val second = bytes(20,21,22)
            val records = listOf(fixture(root, first), fixture(root, second))
            val zipBytes = archive(root, records)
            // Tamper with the bytes of the second member via a freshly written ZIP.
            val manifest = java.util.zip.ZipInputStream(zipBytes.inputStream()).use { zin ->
                zin.nextEntry
                zin.readBytes()
            }
            val altered = crafted(
                "manifest.json" to manifest,
                "originals/" + records[0].sha256 + ".jpg" to first,
                "originals/" + records[1].sha256 + ".jpg" to bytes(1,2,3)
            )
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipReader.stage(ByteArrayInputStream(altered), root)
            }
            assertTrue(root.listFiles()!!.none { it.name.endsWith(".partial") })
        } finally { root.deleteRecursively() }
    }
    @Test fun neverFollowsSymlinkedPrivateOriginalsRoot() {
        val parent = Files.createTempDirectory("glimpse-zip-symlink")
        try {
            val real = Files.createDirectory(parent.resolve("real"))
            val link = Files.createSymbolicLink(parent.resolve("link"), real)
            assertThrows(IllegalArgumentException::class.java) {
                CollectionZipReader.stage(
                    ByteArrayInputStream(byteArrayOf()), link.toFile()
                )
            }
        } finally { parent.toFile().deleteRecursively() }
    }
}
