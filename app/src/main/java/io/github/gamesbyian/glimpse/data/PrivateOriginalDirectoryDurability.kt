package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption

/**
 * Persist the app-private *directory entry* after a newly fsynced receipt
 * or immutable SHA original has been created.
 *
 * For crash durability, syncing a file's bytes is insufficient by itself:
 * the parent directory's new name must be explicitly synchronized too.
 * Open READ + NOFOLLOW_LINKS to refuse a substituted directory symlink,
 * force(true) and propagate all failures. No fallback that silently claims
 * durability, no rename, truncate, delete or foreign-directory traversal.
 *
 * Call only after file content has been forced and before Room admission.
 * This is a filesystem barrier, not proof against a physical power cut,
 * faulty flash controller or an incomplete earlier publication.
 */
internal fun forcePrivateOriginalDirectory(directory: File) {
    require(directory.isDirectory && !Files.isSymbolicLink(directory.toPath())) {
        "Private original publication directory is unavailable."
    }
    FileChannel.open(
        directory.toPath(),
        StandardOpenOption.READ,
        LinkOption.NOFOLLOW_LINKS
    ).use { channel ->
        channel.force(true)
    }
}
