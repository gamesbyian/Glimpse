package io.github.gamesbyian.glimpse.data

import android.system.Os
import android.system.OsConstants
import java.io.File
import java.nio.file.Files

/**
 * Read-only Android inode snapshot; never a recovery or deletion capability.
 *
 * A matching (st_dev, st_ino) can corroborate a descriptor/path pair at the
 * instant sampled. It cannot prove which attempt created the inode, survive
 * arbitrary deletion/inode reuse, authenticate a historical receipt, or
 * authorize moving/quarantining a pre-existing SHA-named original.
 */
internal data class PrivateOriginalFileIdentityEvidence(
    val device: Long,
    val inode: Long,
    val ownerUid: Int,
    val size: Long,
    val hardLinks: Long
)

/**
 * Never read a file reached through a symlink. Open a no-follow descriptor,
 * then compare fstat(fd) against lstat(path) while the fd remains open.
 * Any error or identity disagreement returns no evidence (fail-closed).
 * This does not mutate a byte, lock, membership or receipt.
 */
internal fun readPrivateOriginalFileIdentityEvidence(
    originals: File,
    target: File
): PrivateOriginalFileIdentityEvidence? {
    val digestFile = Regex("^[0-9a-f]{64}\\.(jpg|png|webp)$")
    if (!originals.isDirectory || Files.isSymbolicLink(originals.toPath()) ||
        !digestFile.matches(target.name)
    ) return null
    return try {
        val root = originals.canonicalFile
        if (target.canonicalFile.parentFile != root) return null
        val fd = Os.open(
            target.absolutePath,
            OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC,
            0
        )
        try {
            val opened = Os.fstat(fd)
            val observedPath = Os.lstat(target.absolutePath)
            if (!OsConstants.S_ISREG(opened.st_mode) ||
                !OsConstants.S_ISREG(observedPath.st_mode) ||
                opened.st_dev != observedPath.st_dev ||
                opened.st_ino != observedPath.st_ino ||
                opened.st_size != observedPath.st_size ||
                opened.st_ino <= 0L
            ) null else PrivateOriginalFileIdentityEvidence(
                device = opened.st_dev,
                inode = opened.st_ino,
                ownerUid = opened.st_uid,
                size = opened.st_size,
                hardLinks = opened.st_nlink
            )
        } finally {
            Os.close(fd)
        }
    } catch (_: Exception) {
        null
    }
}
