package io.github.gamesbyian.glimpse.data

import com.google.gson.JsonParser
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal data class StagedArchivePhoto(
    val sha256: String,
    val mime: String,
    val bytes: Long,
    val temporaryFile: File
)
internal data class StagedCollectionArchive(
    val title: String,
    val photos: List<StagedArchivePhoto>
) {
    fun discardStaging() { photos.forEach { it.temporaryFile.delete() } }
}

/**
 * Only reads Glimpse's manifest-first, SHA-identified v1 format. No extraction
 * using ZIP-controlled paths. Validation ends before the caller publishes files
 * or mutates Room, so corrupt archives cannot import a partial collection.
 */
internal object CollectionZipReader {
    private const val maxEntries = 20_000
    private const val maxManifestBytes = 8 * 1024 * 1024
    private const val maxOriginalBytes = 50L * 1024 * 1024
    private const val maxArchiveExpandedBytes = 1024L * 1024 * 1024

    fun stage(
        source: InputStream,
        originalsDirectory: File,
        checkpoint: () -> Unit = {}
    ): StagedCollectionArchive {
        require(originalsDirectory.isDirectory &&
            !Files.isSymbolicLink(originalsDirectory.toPath())) {
            "The app-private originals directory is unavailable."
        }
        val staged = mutableListOf<StagedArchivePhoto>()
        try {
            ZipInputStream(source.buffered()).use { archive ->
                val manifestEntry = archive.nextEntry
                    ?: error("The archive is empty.")
                require(manifestEntry.name == "manifest.json" && !manifestEntry.isDirectory) {
                    "A Glimpse v1 manifest must be the first ZIP entry."
                }
                val manifestBytes = readLimited(archive, maxManifestBytes.toLong(), checkpoint)
                archive.closeEntry()
                val json = JsonParser.parseReader(
                    InputStreamReader(manifestBytes.inputStream(), Charsets.UTF_8)
                ).asJsonObject
                require(json.keySet() == setOf(
                    "format", "version", "collectionTitle", "photoCount", "photos"
                )) { "Unexpected manifest fields." }
                require(json.get("format").asString == "glimpse.collection.originals" &&
                    json.get("version").asInt == 1
                ) { "Unsupported Glimpse archive format or version." }
                val title = json.get("collectionTitle").asString
                require(title.length in 1..80 &&
                    title.none { it.isISOControl() }) { "Invalid archive collection title." }
                val manifestPhotos = json.getAsJsonArray("photos")
                val count = json.get("photoCount").asInt
                require(count in 0..maxEntries && manifestPhotos.size() == count) {
                    "Invalid archive count."
                }
                val seen = HashSet<String>()
                var cumulative = 0L
                for (record in manifestPhotos) {
                    checkpoint()
                    val descriptor = record.asJsonObject
                    require(descriptor.keySet() == setOf("path", "sha256", "mime", "bytes")) {
                        "Unexpected archive photo fields."
                    }
                    val sha = descriptor.get("sha256").asString
                    val mime = descriptor.get("mime").asString
                    val name = "originals/" + expectedPrivateOriginalName(sha, sha, mime)
                    require(descriptor.get("path").asString == name && seen.add(name)) {
                        "Duplicate or unsafe archive path."
                    }
                    val expectedBytes = descriptor.get("bytes").asLong
                    require(expectedBytes in 1..maxOriginalBytes) {
                        "Archive image exceeds Glimpse's import size limit."
                    }
                    cumulative += expectedBytes
                    require(cumulative <= maxArchiveExpandedBytes) {
                        "Archive exceeds the bounded restore budget."
                    }
                    val next = archive.nextEntry ?: error("Missing archived original.")
                    require(!next.isDirectory && next.name == name) {
                        "Archived originals differ from the manifest."
                    }
                    // A generated temp name, not a ZIP-controlled filesystem path.
                    val temp = File.createTempFile("import-", ".partial", originalsDirectory)
                    // Record the temp immediately so failures never abandon it.
                    staged += StagedArchivePhoto(sha, mime, expectedBytes, temp)
                    val sha256 = MessageDigest.getInstance("SHA-256")
                    var length = 0L
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkpoint()
                            val read = archive.read(buffer)
                            if (read == -1) break
                            length += read
                            require(length <= expectedBytes) {
                                "Archive original exceeds its declared size."
                            }
                            sha256.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                        // The ZIP publisher now retains validated images on
                        // downstream failure. Stage bytes must be durable
                        // before they can enter the exclusive SHA gateway.
                        output.fd.sync()
                    }
                    archive.closeEntry() // verifies ZIP CRC when available
                    require(length == expectedBytes &&
                        sha256.digest().joinToString("") { "%02x".format(it) } == sha
                    ) { "Archive original failed exact-byte validation." }
                }
                require(archive.nextEntry == null) {
                    "Archive contains unmanifested or extra entries."
                }
                return StagedCollectionArchive(title, staged.toList())
            }
        } catch (failure: Throwable) {
            staged.forEach { it.temporaryFile.delete() }
            throw failure
        }
    }

    private fun readLimited(
        source: InputStream, max: Long, checkpoint: () -> Unit
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            checkpoint()
            val n = source.read(chunk)
            if (n < 0) break
            require(out.size().toLong() + n <= max) {
                "Manifest exceeds bounded size."
            }
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }
}
