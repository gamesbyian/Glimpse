package io.github.gamesbyian.glimpse.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Portable, versioned original-only export. This does NOT implement restore. */
internal data class CollectionZipSummary(val count: Int, val bytes: Long)

/**
 * Does not invent a backup of wallpaper settings, shuffle state or saved edits.
 * All file names are derived from verified SHA/MIME, never user-provided paths.
 * Preflights every source before emitting ZIP bytes and re-hashes while copying.
 */
internal object CollectionZipWriter {
    private const val maxEntries = 20_000
    private const val fixedTimestamp = 315532800000L // 1980-01-01 UTC

    fun defaultName(collectionTitle: String): String {
        val safe = collectionTitle.trim().map { ch ->
            if (ch.isLetterOrDigit() || ch == '-' || ch == '_') ch else '-'
        }.joinToString("").replace(Regex("-+"), "-")
            .trim('-').take(48).ifEmpty { "collection" }
        return "glimpse-" + safe + "-originals.zip"
    }

    fun write(
        destination: OutputStream,
        originalsDirectory: File,
        title: String,
        records: List<PhotoRecord>,
        checkpoint: () -> Unit = {}
    ): CollectionZipSummary {
        require(records.size <= maxEntries) { "Collection too large for this archive format." }
        require(records.map { it.id }.toSet().size == records.size) {
            "Duplicate source identities in collection export."
        }
        val files = records.map { photo ->
            checkpoint()
            val original = verifyPrivateOriginal(originalsDirectory, photo)
            photo to original
        }
        var totalBytes = 0L
        files.forEach { (photo, _) -> totalBytes = Math.addExact(totalBytes, photo.byteLength) }
        val metadata = buildManifest(title, files.map { it.first })
        ZipOutputStream(destination.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json").apply { time = fixedTimestamp })
            zip.write(metadata.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val buffer = ByteArray(64 * 1024)
            files.forEach { (photo, file) ->
                checkpoint()
                val path = "originals/" + expectedPrivateOriginalName(photo.id, photo.sha256, photo.mime)
                zip.putNextEntry(ZipEntry(path).apply { time = fixedTimestamp })
                val hash = MessageDigest.getInstance("SHA-256")
                var written = 0L
                file.inputStream().buffered().use { input: InputStream ->
                    while (true) {
                        checkpoint()
                        val read = input.read(buffer)
                        if (read < 0) break
                        hash.update(buffer, 0, read)
                        zip.write(buffer, 0, read)
                        written += read.toLong()
                    }
                }
                val actual = hash.digest().joinToString("") { "%02x".format(it) }
                require(written == photo.byteLength && actual == photo.sha256) {
                    "Original changed during export; incomplete destination must be discarded."
                }
                zip.closeEntry()
            }
            zip.finish()
        }
        return CollectionZipSummary(files.size, totalBytes)
    }

    private fun buildManifest(title: String, photos: List<PhotoRecord>): String {
        val entries = photos.joinToString(",") { photo ->
            val entry = "originals/" +
                expectedPrivateOriginalName(photo.id, photo.sha256, photo.mime)
            """{"path":${quote(entry)},"sha256":${quote(photo.sha256)},"mime":${quote(photo.mime)},"bytes":${photo.byteLength}}"""
        }
        return """{"format":"glimpse.collection.originals","version":1,"collectionTitle":${quote(title)},"photoCount":${photos.size},"photos":[${entries}]}"""
    }

    private fun quote(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else append(ch)
            }
        }
        append('"')
    }
}
