package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

/**
 * Device-private, provider-URI-free recovery evidence for a *verified* import
 * stage before attempting publication of a SHA-named immutable original.
 *
 * A receipt is not permission to modify or remove either file. It records
 * enough information for a future read-only validation and user-directed,
 * provenance-checked recovery (#143). No automatic retry, movement, deletion,
 * quarantine or Room attachment is performed here.
 */
internal data class PrivatePublicationReceipt(
    val stageName: String,
    val sha256: String,
    val length: Long,
    val extension: String,
    val collectionId: String,
    val createdAt: Long
) {
    init {
        require(stageName.matches(Regex("import-[a-zA-Z0-9_-]{1,80}\\.partial")))
        require(sha256.matches(Regex("[0-9a-f]{64}")))
        require(length in 1..(50L * 1024L * 1024L))
        require(extension in setOf(".jpg", ".png", ".webp"))
        require(collectionId.isNotBlank() && collectionId.length <= 256)
        require(createdAt > 0)
    }
}

internal object PrivatePublicationReceipts {
    private const val RECEIPT_VERSION = "1"
    private val receiptName = Regex("publish-[a-f0-9-]{36}\\.receipt")
    private val encoding = Charsets.UTF_8

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun lineFor(receipt: PrivatePublicationReceipt): String {
        val collection = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(receipt.collectionId.toByteArray(encoding))
        val fields = listOf(
            RECEIPT_VERSION, receipt.stageName, receipt.sha256,
            receipt.length.toString(), receipt.extension,
            collection, receipt.createdAt.toString()
        ).joinToString("\t")
        return fields + "\t" + sha256(fields.toByteArray(encoding)) + "\n"
    }

    /**
     * Exclusive, fsynced creation under a non-symlinked app-private directory.
     * The source and sha target remain untouched even if this write fails.
     * Never put provider URI, arbitrary source filename, EXIF or image bytes
     * into a receipt.
     */
    fun record(root: File, receipt: PrivatePublicationReceipt): File {
        require((root.isDirectory || root.mkdirs()) &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile == root.absoluteFile.canonicalFile
        ) { "Private publication receipt directory unavailable" }
        val path = File(root, "publish-${UUID.randomUUID()}.receipt")
        val data = lineFor(receipt).toByteArray(encoding)
        require(data.size <= 1024)
        Files.newByteChannel(
            path.toPath(),
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
        ).use { channel ->
            val bytes = ByteBuffer.wrap(data)
            while (bytes.hasRemaining()) channel.write(bytes)
            // SeekableByteChannel lacks force; open a fresh exact-name channel
            // with WRITE (never CREATE/TRUNCATE) to flush the receipt payload.
        }
        java.nio.channels.FileChannel.open(
            path.toPath(), StandardOpenOption.WRITE
        ).use { it.force(true) }
        return path
    }

    /** Validate a private receipt; malformed, untrusted or incomplete data
     * never authorize a recovery. Exactly one bounded line, no paths exposed.
     */
    fun read(file: File): PrivatePublicationReceipt? {
        if (!file.isFile || Files.isSymbolicLink(file.toPath()) ||
            !receiptName.matches(file.name) || file.length() !in 1..1024) return null
        return try {
            val text = file.readText(encoding)
            if (!text.endsWith("\n") || text.count { it == '\n' } != 1) return null
            val columns = text.dropLast(1).split("\t")
            if (columns.size != 8 || columns[0] != RECEIPT_VERSION) return null
            val signed = columns.dropLast(1).joinToString("\t")
            if (sha256(signed.toByteArray(encoding)) != columns.last()) return null
            val collection = String(
                Base64.getUrlDecoder().decode(columns[5]), encoding
            )
            PrivatePublicationReceipt(
                stageName = columns[1],
                sha256 = columns[2],
                length = columns[3].toLong(),
                extension = columns[4],
                collectionId = collection,
                createdAt = columns[6].toLong()
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Read-only, bounded inventory. No file or Room modifications. */
    fun inspect(root: File, maxFiles: Int = 2048): Pair<Int, Int> {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath())) return 0 to 0
        val candidates = root.listFiles()?.filter { it.name.startsWith("publish-") }
            ?.take(maxFiles) ?: return 0 to 0
        val valid = candidates.count { read(it) != null }
        return valid to (candidates.size - valid)
    }
}
