package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files

/**
 * A user-initiated, read-only verification of DB-registered originals.
 * No filenames, hashes, EXIF, provider URIs or error messages are surfaced.
 * An original in several collections counts once, since photos are unique.
 */
data class LocalOriginalIntegritySummary(val registered: Int, val verified: Int) {
    init {
        require(registered >= 0 && verified in 0..registered)
    }
    val problemCount: Int get() = registered - verified
}

internal fun inspectRegisteredOriginals(
    root: File,
    registered: List<PhotoRecord>,
    checkpoint: () -> Unit = {}
): LocalOriginalIntegritySummary {
    if (registered.isNotEmpty()) {
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
            "Private originals directory is unavailable for inspection."
        }
    }
    var verified = 0
    for (photo in registered) {
        checkpoint()
        try {
            verifyPrivateOriginal(root, photo)
            verified++
        } catch (_: Exception) {
            // A bad record does not stop checks of unrelated originals.
            // We intentionally return only aggregate counts, not details.
        }
    }
    return LocalOriginalIntegritySummary(registered.size, verified)
}
