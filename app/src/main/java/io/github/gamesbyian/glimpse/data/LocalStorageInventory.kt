package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files

/**
 * Metadata-only, read-only physical storage inventory. This deliberately does
 * not imply Room membership, image integrity or total free device capacity.
 */
internal data class LocalStorageInventory(
    val originalFileCount: Int,
    val originalBytes: Long,
    val stagingFileCount: Int,
    val stagingBytes: Long
)

private val originalFilename = Regex("^[0-9a-f]{64}\\.(jpg|png|webp)$")

/**
 * Only count direct, regular, non-symlink entries in the app-private originals
 * directory. Do not follow aliases, inspect EXIF or ever delete anything.
 * Returns null if the directory exists but cannot be enumerated safely.
 */
internal fun inspectLocalStorage(originals: File): LocalStorageInventory? {
    if (!originals.exists()) return LocalStorageInventory(0, 0L, 0, 0L)
    return try {
        if (!originals.isDirectory || Files.isSymbolicLink(originals.toPath())) return null
        val root = originals.canonicalFile
        val children = originals.listFiles() ?: return null
        var originalsCount = 0
        var originalsBytes = 0L
        var stagingCount = 0
        var stagingBytes = 0L
        for (child in children) {
            if (Files.isSymbolicLink(child.toPath()) ||
                !child.isFile || child.canonicalFile.parentFile != root
            ) continue
            val length = child.length().coerceAtLeast(0L)
            fun addBounded(total: Long) =
                if (Long.MAX_VALUE - total < length) Long.MAX_VALUE else total + length
            when {
                originalFilename.matches(child.name) -> {
                    originalsCount++
                    originalsBytes = addBounded(originalsBytes)
                }
                child.name.startsWith("import-") && child.name.endsWith(".partial") -> {
                    stagingCount++
                    stagingBytes = addBounded(stagingBytes)
                }
            }
        }
        LocalStorageInventory(originalsCount, originalsBytes, stagingCount, stagingBytes)
    } catch (_: Exception) {
        null
    }
}

/**
 * Potentially unreferenced completed originals, calculated from physical
 * SHA-named files versus the currently committed Room photo records.
 *
 * An unreferenced file may be a valuable immutable copy written just before
 * a crash or a transaction commit. This is an advisory count, NEVER evidence
 * that it is safe to delete the file. Verification/recovery needs a separate
 * explicit user action and full content validation.
 */
internal data class PotentialOrphanInventory(
    val scannedOriginals: Int,
    val unreferencedOriginals: Int,
    val unreferencedBytes: Long
)

/**
 * Read-only, metadata-only reconciliation. No byte reads, imports, hash
 * verification, logs, files changed or symbolic link traversal. Do not trust
 * a database record whose path escapes the exact private originals directory.
 */
internal fun inspectPotentialOrphans(
    originals: File,
    registered: List<PhotoRecord>,
    checkpoint: () -> Unit = {}
): PotentialOrphanInventory? {
    if (!originals.exists()) return PotentialOrphanInventory(0, 0, 0L)
    return try {
        if (!originals.isDirectory || Files.isSymbolicLink(originals.toPath())) return null
        val root = originals.canonicalFile
        val referenced = registered.mapNotNull { photo ->
            checkpoint()
            val file = File(photo.localPath)
            if (file.name == photo.sha256 + "." + file.extension &&
                originalFilename.matches(file.name) &&
                file.absoluteFile.parentFile == root
            ) file.name else null
        }.toHashSet()
        var scanned = 0
        var orphaned = 0
        var bytes = 0L
        for (child in (originals.listFiles() ?: return null)) {
            checkpoint()
            if (Files.isSymbolicLink(child.toPath()) ||
                !child.isFile || child.canonicalFile.parentFile != root ||
                !originalFilename.matches(child.name)
            ) continue
            scanned++
            if (child.name !in referenced) {
                orphaned++
                val length = child.length().coerceAtLeast(0L)
                bytes = if (Long.MAX_VALUE - bytes < length) Long.MAX_VALUE else bytes + length
            }
        }
        PotentialOrphanInventory(scanned, orphaned, bytes)
    } catch (_: Exception) {
        null
    }
}
