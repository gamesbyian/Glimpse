package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files

/**
 * Verify a Room photo maps to an exact intact app-private source before
 * exporting or handing its pixels to Android WallpaperManager.
 *
 * The file is immutable after import. Recheck SHA at the side-effect boundary
 * so a damaged/replaced source cannot silently be applied/shared as another.
 */
internal fun expectedPrivateOriginalName(id: String, sha: String, mime: String): String {
    require(id == sha && sha.matches(Regex("[0-9a-f]{64}"))) {
        "The private original identity is invalid."
    }
    val suffix = when (mime) {
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        else -> throw IllegalArgumentException("Unsupported original format.")
    }
    return sha + suffix
}

internal fun verifyPrivateOriginal(rootDir: File, photo: PhotoRecord): File {
    val name = expectedPrivateOriginalName(photo.id, photo.sha256, photo.mime)
    require(photo.byteLength in 1..(50L * 1024L * 1024L)) {
        "Original size outside the supported import range."
    }
    require(rootDir.isDirectory && !Files.isSymbolicLink(rootDir.toPath())) {
        "Private originals directory is unavailable."
    }
    val root = rootDir.canonicalFile
    val candidate = File(photo.localPath)
    require(candidate.isFile && !Files.isSymbolicLink(candidate.toPath()) &&
        candidate.canonicalFile.parentFile == root && candidate.name == name
    ) { "Private original is missing or outside its verified directory." }
    require(existingOriginalMatches(candidate, photo.sha256, photo.byteLength)) {
        "The private original failed exact-byte integrity verification."
    }
    return candidate
}
