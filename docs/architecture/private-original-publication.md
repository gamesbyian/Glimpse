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

## Read-only interrupted-publication evidence (not recovery)

A bounded `PrivatePublicationReceiptJournal.inspectFileEvidence()` can corroborate
each structurally valid receipt with the **current** SHA-256 and length of
its staged source and digest-named target. It reports only aggregate
exact/missing/unverified counts, malformed receipt counts, omitted receipts
and whether the cumulative hash budget ran out. Names, images, collection IDs
and original paths are never returned.

The scan runs only when explicitly invoked by a caller on IO, with a
cancellation checkpoint at receipt traversal and throughout each 64-KiB
original-digest read. Cancelling midway through hashing propagates rather
than returning a misleading unverified file count. It follows no symlink, hashes no more than its
configured byte budget and **never** deletes, renames, quarantines, truncates,
reattaches or overwrites a file. It cannot prove which writer created an
inode or whether another process is writing now. Matching bytes establish
integrity at the moment of inspection, not ownership or repair authority.

This is a read-only JVM-tested diagnostic primitive for issue #143, not
crash recovery. Actual Android kill/interruption tests, cross-process
coordination, proven inode provenance and reversible owner-visible quarantine
are still required before any recovery mutation.

**Optional Help inspection:** Glimpse now exposes the existing bounded
receipt checker through an explicit "Check interrupted import evidence"
control in Help & Privacy. It executes on the IO dispatcher, checks
cancellation, and shows only aggregate exact, missing, unverified, omitted
and untrusted counts. Its labels explicitly deny repair authority and do not
claim that zero receipts proves every older import succeeded. Rendering is
tested both in JVM presentation tests and through the native Android Help
journey. No unattended scan, automatic quarantine or recovery is added.

## Cross-process publication admission (candidate, pending Android acceptance)

An optional implementation now wraps every private original digest publication
in a stable app-private `.glimpse-publication-<SHA>.lock` FileChannel
exclusive lock, **in addition** to a SHA-scoped in-process JVM monitor. Each distinct SHA has an independent lock; the same-hash lock spans the digest
path reservation, entire fallback write, force, and final SHA validation. A second Android process following this publisher
contract must wait for an active writer to complete before inspecting its SHA
path; unrelated operations never truncate, unlink or overwrite the sentinel.
The lock opens with `NOFOLLOW_LINKS` and fails closed on symlink or missing
staging. A process kill releases the OS lock but does **not** justify deleting,
quarantining or reusing its possibly incomplete digest path.

This is cross-process **cooperative serialization only**, not proof that a
preexisting target was created by the current writer, not an authenticated
reservation receipt, and not recovery. First acquire runtime evidence on
Android API29 and modern Android (including an actual two-process, same-hash
live-writer test), and verify interruption/restart regression against issue
#143 before accepting a production merge. Each persistent SHA-scoped sentinel is not an
original and is excluded by the existing SHA/image and staging inventories.
Different SHAs must continue publishing independently even while a
same-SHA writer is paused; the focused JVM regression proves this invariant.
All private-original writers must continue using the shared publication
gateway; Android advisory file locks do not constrain unrelated code that
ignores the lock.

## Conservative preflight: intact stage and *missing* target (read-only)

The opt-in recovery lane may someday use the typed
`PrivatePublicationReceiptJournal.inspectMissingTargetStages()` preflight.
It scans only a bounded number of exact-format local receipts, hashes at most
its configured aggregate byte budget, refuses symlinked roots/stages, checks
the complete stage's length and SHA, decodes the original collection identity
with strict UTF-8, and accepts only a digest path that is **absent even under
NOFOLLOW_LINKS**. An existing digest path (including an exact complete
original, a foreign fragment, or a dangling symlink) is excluded entirely.

Candidates and receipt handles stay within the data layer. The user-facing
Help/Privacy view still exposes only aggregates. This preflight never
publishes a digest, inserts Room membership, modifies a receipt, deletes a
file, decides writer ownership, or claims that a missing target is guaranteed
to remain missing after the scan. An ongoing writer may change the evidence
while it runs. The only safe future mutation route must revalidate under the
same SHA-scoped process lock, prove target absence and collection identity,
ask for explicit owner consent, and pass real Android interruption/restart
tests before a data-modifying recovery action can ship.

This is strictly a read-only staging plan. **Issue #143 remains open** for
real recovery and quarantine of partially published targets.

## Explicit-confirmation backend for absent digest paths (unaccepted draft)

A proposed `ManualMissingTargetStageResume` backend can process only a bounded
candidate set from the verified-stage/missing-target preflight. Callers must
provide an affirmative confirmation flag. No native UI invokes it, so this is
**not yet an exercised human-consent journey** and should remain a draft
implementation until an explicit, understandable owner-facing confirmation is
built and tested.

On IO, the backend rechecks the exact receipt through a no-follow channel,
the existing staged bytes and SHA, affirmative no-follow target absence, the
existing destination collection and the decoded JPEG/PNG/WebP dimensions.
It rejects a conflicting registered photo, uses the existing SHA-scoped
exclusive publisher, and attempts a **single Room transaction** which rechecks both the destination collection and immutable metadata of any existing PhotoRecord before admitting a membership. A concurrent conflicting photo record is rejected, never silently reused. Return values
contain aggregate counts only. Stages and receipts are always retained.
A raced existing foreign or partial digest fails closed, and a vanished
collection receives no membership. A successful file publication followed by
a failed Room transaction can leave a complete orphan; because the target
now exists, this preflight deliberately will not retry or delete it. The
remaining post-publication/Room crash window needs separate reconciliation
and verification.

Until real API29/modern Android, owner consent/UX and interruption tests are
accepted, this backend must never be automatically called on app startup,
from a worker or widget, or silently presented as a recovery feature.
Ambiguous preexisting digest files still cannot be quarantined or replaced.
Issue #143 remains open.

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

## Deterministic exclusive-copy interruption tests

The exclusive `CREATE_NEW` fallback now exposes an optional internal
post-chunk checkpoint (the production default is a no-op). JVM and real
Android app-private tests can throw a controlled exception **after a
destination write but before force/Room admission**, without touching
external storage or owner photos. The tested outcomes distinguish a
partially written digest target from an exact-byte target whose last
write completed but whose publication did not finish. Both retain the
complete staged copy and the durable attempt receipt; neither test
authorizes unlinking/replacing the existing digest name or auto-attaching
a database membership.

This is deterministic fault injection in disposable files, **not**
physical power-loss proof. API29/modern Android
instrumentation and actual process kill at each receipt/reservation/force/
DAO transition remain required under issue #143. Real symlink, foreign
file and same-hash race refusal remain independent gates.

## Actual Android child-process SIGKILL acceptance

A debug-only, non-exported bound service runs under a distinct process name, inside
a unique instrumentation cache child, and invokes the actual exclusive
`CREATE_NEW` writer against a 128-KiB synthetic staging source. Immediately
after the first 64-KiB write it fsyncs a test-only PID marker and issues
`android.os.Process.killProcess` against **its own** process. The main
instrumentation runner separately verifies that the child PID exited, that the
staged bytes and receipt survived, that the digest path retains the exact
first chunk, and that a reselect cannot overwrite it.

The debug service now accepts only two hard-coded interruption points:
after 64 KiB of a 128 KiB original, and after all 128 KiB are written but
**before** the destination's `FileChannel.force(true)`, final SHA validation,
or any Room membership commit. The latter is a real post-last-write
process-kill scenario; it verifies staged and currently readable target bytes
and the retained receipt, without representing volatile cache contents as
power-loss durability or an admitted library record. A subsequent publisher
can verify and reuse the exact current complete bytes, while an incomplete
digest remains non-replaceable.

This establishes real **mid-copy and post-last-write/pre-force SIGKILL** cases
on the Android API(s) where their exact-head CI executes. It does not claim a power-cut durability
guarantee, a physically tested Pixel, process kill before/after *other*
publication boundaries, cross-process ownership proof, recovery quarantine,
successful reselect or a Room commit. Issue #143 remains open.

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
