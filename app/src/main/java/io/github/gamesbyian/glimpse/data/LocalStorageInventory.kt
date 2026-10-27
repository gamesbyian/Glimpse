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
