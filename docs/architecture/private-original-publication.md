# Private original publication and crash-recovery contract

**Authoritative for new importer publication and recovery work.** Cross-reference
[architecture audit #120](https://github.com/gamesbyian/Glimpse/issues/120),
[interrupted-publication recovery #143](https://github.com/gamesbyian/Glimpse/issues/143)
and [installed signing continuity](apk-signing-updates.md). This contract is
about *app-owned filesDir originals*, never files in Android Downloads, Google
Photos, user media, another app's space or SAF documents.

## Current verified baseline (31 October 2026)

Merged [#141](https://github.com/gamesbyian/Glimpse/pull/141) fixes the
check-then-`renameTo` replacement race. `PhotoImporter.copyOne` copies from
a temporary URI into a unique same-directory `import-*.partial` file,
calculates SHA-256 while streaming, checks byte/pixel limits and supported
image identity, then calls `publishPrivateOriginalExclusively` *before*
attaching the PhotoRecord and collection membership in one Room transaction.
The temporary input is fsynced before publication.

Publication first tries a **same-directory hard link**, which atomically
creates the SHA-name destination if absent. Actual Android API34 Photo Picker
acceptance showed this operation is denied under the app sandbox despite
working on hosted Linux. Android therefore falls back to a file-channel
`CREATE_NEW` operation, which **atomically refuses an existing name**,
streams the complete staging bytes to the exclusively reserved name, forces
the file and revalidates full SHA-256 and byte length. A preexisting same-hash
original may be shared only after exact-byte verification; corrupt, partial or
symlinked existing targets are never overwritten, truncated or removed.
The API34 picker, real Room and native suite passed on the final #141 head.
Oldest supported API29 acceptance is independently tracked in
[PR #144](https://github.com/gamesbyian/Glimpse/pull/144).

## Subsequent merged safeguards

PRs #144, #146 and #147 add actual API29 private-filesystem tests,
same-process SHA publication serialization, and retention of validated
nonempty importer staging files after publication failure. Only zero-byte,
aged importer staging placeholders may be pruned automatically.

PR #149 adds a successful populated, same-temporary-signer Android upgrade
test covering five Room tables and three exact-byte originals. This does not
establish stable owner signing.

PR #155 subsequently added durable, fsynced, URI-free per-attempt
publication receipts before the final digest-name reservation, and retires
each exact intact receipt only after a verified original and committed Room
membership. Receipt content is not itself proof of inode ownership and cannot
authorize repair or deletion. Merged PR #156 extends exclusive no-replace
publication to edited PNG originals and collection ZIP restores; PR #157
bounds edited-share cache writes and pruning to newly owned files.

Issue #143 remains open: those changes do **not** establish cross-process
interrupted-publication recovery, authenticated ownership of a damaged
unregistered target, non-destructive quarantine, or the process-kill/device
fault-injection evidence required for completion. Issue #72 likewise remains
open until the owner-controlled persistent signer and two separate main-build
install-over-update checks are independently demonstrated.

## Invariants

1. A provider URI is an ephemeral grant. Never persist it in the import
   journal, Room, photos, diagnostics or a recovery receipt. Never modify the
   provider file.
2. A digest filename is never authority to replace its existing contents.
   All existing targets, including symlinks and registered originals, must be
   preserved byte-for-byte unless the owner explicitly authorizes a
   separately proven, safe recovery. No destructive cleanup is implicit.
3. A successful imported membership must refer to verified, fsynced private
   bytes with the SHA and mime metadata recorded in Room.
4. File and Room commit boundaries are separate. Failure between them may
   create an **orphan original**, which a read-only audit may identify; it
   never gives permission to delete the file.
5. Distinguish **atomic path reservation** (`CREATE_NEW`) from **atomic
   publication visibility** (a fully written, durable file becomes visible in
   one namespace operation). The former does not supply the latter.
6. Two independent importers with the same SHA must not treat a live writer's
   incomplete path as proof of corruption. Coordinate same-hash publication
   across app entrypoints and processes without introducing an overwrite
   opportunity. Different SHAs should remain independently usable.
7. Android API29, API34 and current-target behavior require actual emulator
   evidence; a Linux JVM test alone is never Android filesystem acceptance.

**Receipt status update:** the proposed receipt stage below now exists in
`PrivatePublicationReceiptJournal`; the remaining proposed *recovery*,
ownership proof and quarantine steps have not been implemented or accepted.

## Remaining crash window and recovery plan

A process kill, storage exhaustion, device power loss or cancelled file-channel
write can leave an incomplete **unregistered** SHA-name file. Today the next
import refuses to overwrite it. This protects prior bytes but can make a
legitimate reimport fail. Issue #143 owns the required end-to-end correction.

A proposed recovery must first establish *durable provenance* for the exact
publication attempt (privately named staging identity, digest, length,
creation time, unique attempt ID and target identity), separate from the
ordinary provider-URI-free import journal. Pre-existing Room registrations,
active same-hash writers, mismatched inodes/lengths and ambiguous historical
orphan files must trigger **refusal and non-destructive diagnostics**. Never
infer ownership from a filename alone.

When a crashed write is proven and no concurrent writer or Room registration
can claim the bytes, the app may offer a reversible, owner-visible recovery:
retain the questionable bytes under an independently generated, exclusively
created app-private quarantine identity, verify the reselected source, publish
a new complete immutable original and commit the restored membership. Quarantine
must not use a replace-capable rename and must preserve a meaningful audit
record, with an explicit safe resolution path. The exact method, user consent
and retention lifetime require documented acceptance before deployment.
If provenance is uncertain, leave all bytes untouched.

The complete fault-injection matrix: kill before/after receipt persistence;
after staging force; after path reservation but before any copy; mid-copy;
after final force; before and after Room transaction; two simultaneous
same-hash writers; preexisting valid, truncated, corrupted or symlinked target;
full-disk error; process restart; exact-byte reselect. Verify unchanged
preexisting bytes and no lost memberships in **real Android** tests.

## Platform pitfalls and alternatives

- Android's `Files.copy` documentation explicitly states that its target
  existence check and creation **may not be atomic**. Never revert the
  `CREATE_NEW` fallback to an unchecked copy/rename.
- `Files.move(..., ATOMIC_MOVE)` does **not** promise a non-replacing move:
  Android documentation leaves target replacement behavior
  implementation-specific. It cannot independently satisfy our invariant.
- Bionic added the `renameat2` libc wrapper at **API30**, after our minimum
  API29. A carefully bounded native `RENAME_NOREPLACE` implementation
  might combine atomic no-replace and complete-file visibility, but API29
  would need independently validated syscall/ABI/SELinux behavior or an
  alternative. Do not introduce NDK dependence on speculative compatibility.
- Android app-private root paths should be checked against symlink redirection
  before staging and publication. Native code, if ever added, must constrain
  both source and destination directory capabilities; broad external-storage
  permissions cannot solve this issue.

References:
[Android Files.copy / Files.move contract](https://developer.android.com/reference/java/nio/file/Files),
[Android Bionic API-level changes](https://android.googlesource.com/platform/bionic/+/HEAD/docs/status.md),
[Java StandardOpenOption.CREATE_NEW](https://developer.android.com/reference/java/nio/file/StandardOpenOption).

**Release remains blocked separately** by owner-stable APK signing, actual
data-preserving install-over-update, owner visual acceptance, and measured
Home/Reel scalability. A green synthetic filesystem test is not proof of
whole-product completion.
