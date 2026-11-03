# Android private-original inode provenance: evidence before recovery

Status: **experimental, read-only**. Parent: [issue #143](https://github.com/gamesbyian/Glimpse/issues/143).
Existing receipt v1 and all original files must remain preserved. This document
does not authorize quarantine, deletion, reselecting an occupied target, or
silently inserting a Room membership.

## Platform facts and verified public APIs

- Android's public `android.system.Os.open`, `fstat`, `lstat`, `fsync`
  (API 21+) and `StructStat.st_dev/st_ino/st_uid/st_size/st_nlink` allow a
  descriptor-based local identity snapshot. Read-only `O_NOFOLLOW` rejects
  symbolic-link final components. Android's documentation:
  https://developer.android.com/reference/android/system/Os and
  https://developer.android.com/reference/android/system/StructStat
  and https://developer.android.com/reference/android/system/OsConstants
- `Files.move(..., ATOMIC_MOVE)` **does not promise no replacement** if the
  target exists: the Android API explicitly permits implementation-specific
  replacement or refusal. It is therefore **not** an acceptable fallback for
  no-clobber publication/quarantine. Source:
  https://developer.android.com/reference/java/nio/file/Files
- Linux `renameat2(RENAME_NOREPLACE)` exists but is not exposed as the same
  guaranteed public Android `Files.move` contract. A native/NDK experiment
  would have to prove filesystem support, no-clobber behavior and API29/modern
  device conditions before use; silent fallback to ordinary rename or atomic
  move would be unsafe. Reference:
  https://man7.org/linux/man-pages/man2/rename.2.html

## What a read-only inode snapshot can establish

Under a checked app-private directory, open the SHA path using
`Os.open(O_RDONLY|O_NOFOLLOW|O_CLOEXEC)`; while the descriptor stays open,
compare `fstat(fd)` and `lstat(path)` for a regular file, equal device/inode
and current byte count. A mismatch, symlink, invalid digest name, unreadable
root or unsupported call results in **no evidence**. The proposed
`readPrivateOriginalFileIdentityEvidence` returns only these fields, never
photo content and never a recovery decision.

An equal `(dev, ino)` says the path referenced that open inode at that
instant. It does **not** prove which process or import created the inode,
identify a historical v1 receipt, prevent an inode being recycled after a
delete, prove fsync/directory durability, or make later pathname operations
safe. UID equality likewise cannot distinguish cooperating writers within
one app. An app-installed read-only result cannot be used to move or delete
the target.

## Candidate future ownership protocol (not yet implemented)

1. Before attempting digest-name creation, durably preserve the complete
   SHA-checked staging bytes and a unique v2 attempt nonce/receipt in a
   separately fsynced app-private entry. Reject malformed/unknown receipts.
2. Use the existing stable SHA-scoped JVM/OS admission lock and an
   **exclusive, no-replace** path reservation. Keep the descriptor open.
   Capture `fstat(fd)` only after successful reservation, never from a
   preexisting target. A crash before the next durable ownership record
   produces an ambiguous orphan and must fail closed.
3. Persist a separate versioned attempt-to-descriptor identity record
   without mutating the existing receipt. Require a full fsync and
   directory durability study on each supported Android filesystem.
4. Write the file, force the destination, reread SHA/length and record
   current target identity again before Room. Source and receipts survive.
   If this stage fails or a process dies, do not infer that a complete
   target was committed to Room.
5. Independently and transactionally validate collection/photo identity.
   Record an acknowledged/unknown state that can be inspected on restart.
   The irreversible OS wallpaper gateway is entirely out of scope.
6. Only after all boundaries are experimentally proven should a user see
   **one identified photo** with a reversible next action. For any occupied
   or mismatched inode, no automatic delete, overwrite, relocate or repair.
   A separately created SHA-verified quarantine *copy* with its original
   retained could be researched, but must handle low space and failure;
   this is not a design approval or permission to garbage collect.

## Required independent Android failures

Test separately: process death before digest reservation, immediately after
CREATE_NEW but before inode-receipt durability, after 64 KiB of copy,
after final byte before force, after force/before Room, after Room transaction,
repeated receipt scan, two processes racing the same SHA, a foreign exact
file, foreign incomplete file, symlink swap, hard-link aliases, low storage,
emulator restart, and actual power interruption (not equivalent to SIGKILL).
Record source, receipt, target and Room byte-for-byte in disposable fixtures.
No test may use owner originals or an unapproved cleanup operation.

A physically tested Pixel and actual storage/fs behavior remain separate
acceptance gates. These experiments cannot close #143 by themselves.
