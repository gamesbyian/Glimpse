# Room relationship hardening without losing installed data

**Status:** staged future design; **no schema v3 migration is authorized or implemented**.
Source of truth: `GlimpseDatabase.kt`, exported Room v1/v2 JSON, audit
[GL-AUD-02](../quality/ARCHITECTURE-AUDIT-2026-10-29.md), and [#120](https://github.com/gamesbyian/Glimpse/issues/120).
This is not a claim that historical data is already consistent.

## Data invariant

Every imported original is user-owned and remains byte-identical until the
user explicitly exports, moves, or removes it through a designed operation.
A dangling Room row may be the *only remaining pointer* to an intact private
original. A broken relationship, absent collection or uncertain wallpaper
operation is **never** authority to delete a file, favorite, photo row,
collection record, shuffle bag, or pending wallpaper token. No migration
may use `fallbackToDestructiveMigration` or silently drop offending rows.

Room v2 presently has no foreign keys on `memberships` or `photo_favorites`.
The accepted prior importer correction checks collection existence in the
same transaction; a newly built read-only integrity feature [#132](https://github.com/gamesbyian/Glimpse/pull/132)
is awaiting native/Room/UI acceptance. It detects invalid references and
registered originals without a collection, but cannot automatically repair
their meaning. This precedes any tightening of constraints.

## Dependency-gated execution

1. **Protect installed source data first.** Resolve stable signing [#72](https://github.com/gamesbyian/Glimpse/issues/72):
   two distinct `main` builds must have the same public certificate and
   `adb install -r` must preserve a populated v1/v2 database, private SHA
   originals, user recipes, favorite flags, and pending wallpaper state.
   Never advise uninstalling a working debug install to evade signer mismatch.
2. **Observe, without modification.** Land the source-exact, transactional
   referential audit and its negative Android Room tests. Counts are:
   membership→missing photo, membership→missing collection,
   favorite→missing photo, photo→no membership, and missing references in the
   saved rotation source, pending photo, and acknowledged photo. Aggregate
   user-facing counts only; no photo names, hashes, paths or URIs in logs.
   Maintain the independent exact-byte and physical orphan-file audits.
3. **Characterize legacy states.** Create three disposable real Room fixtures:
   pristine v1, pristine v2, and v2 with deliberately planted dangling rows,
   unsorted/invalid shuffle references and a pending unresolved OS operation.
   Compare before/after exact row counts and field values at each migration
   step. The current populated v1→v2 test is a foundation, not a v3 test.
4. **Offer explicit recovery, not magical guessing.** Before enforcing FKs,
   provide a user-controlled path to attach *SHA-verified* unassigned originals
   to an existing/new collection or retain records unchanged. The UI must
   explain ambiguous memberships and never auto-select an unexpected
   destination or consume the shuffle bag. No automatic file garbage collection.
   Failed verification leaves bytes and registration untouched.
5. **Enforce constraints only once losslessness is proven.** Candidate v3:
   membership collection/photo FKs with `NO ACTION` or `RESTRICT` (never
   `CASCADE`); favorite→photo with `NO ACTION`/`RESTRICT`. Optional
   rotation FKs require a separate decision because unknown/pending tokens
   must remain durable through failed platform operations. Avoid requiring
   impossible historical constraints without a preservation strategy.
6. **Escrow incompatible historical rows if necessary.** If a row cannot
   satisfy the new constraints, a migration may copy its *complete original
   fields* into a dedicated typed legacy/escrow table, and exclude it from
   enforced child relationships only if the union of live + escrow preserves
   **every** original relationship exactly. Such escrow is not automatic
   deletion or silent repair: retain user-visible counts and a deliberate
   reconciliation path. Do not invent a repair to make `foreign_key_check`
   pass. This option must be reviewed and explicitly tested before selection.
7. **Verify the actual device and generated schema.** Commit KSP-generated
   v3 JSON and exercise v1→v2→v3, v2→v3, v3 reopening, no-op repeat opens,
   process cancellation, transaction rollback, injected invalid rows,
   `PRAGMA foreign_key_check`, and original file SHA identity. Preserve
   Room known schema history, expected indices and all photo/rotation
   invariants. Never declare progress from SQLite-only synthetic ordering tests.

## Proposed acceptance table

| Failure case | Required outcome |
|---|---|
| Picker returns after destination removal | Transaction refuses attachment; no dangling new membership; finished SHA copy preserved |
| Same image in two collections | Two memberships, one immutable original, favorites shared globally |
| v2 legacy dangling membership | Preserved in live or explicitly retained escrow row, never dropped silently |
| Registered photo with no collection | Counted; SHA original retained; no arbitrary reassignment |
| Favorite refers to missing photo | Detected and retained for explicit user review |
| Rotation source missing / pending photo invalid | Rotation state remains conservative; no automatic wallpaper replay |
| Migration canceled halfway through | SQLite transaction rollback restores pre-migration bytes and rows |
| FK-constrained table removal | Restricts unsafe deletion; no unintentional photo/collection cascades |
| Old installed APK replaced | Same signature and populated Room/private data intact, including pending unknown |

## Implementation boundaries and ownership

- **DAO/Room lane:** queries, migrations, generated schema, true Room test
  fixtures. Exclusive ownership of `GlimpseDatabase.kt` and schema exports.
- **UI/UX lane:** aggregate audit, explicit opt-in recovery and 320dp/200%
  controls. Do not mix unsafe file deletion into Help & Privacy.
- **Wallpaper lane:** pending/uncertain state and no-replay contracts.
  Never introduce a second OS wallpaper writer in recovery code.
- **Release/CI lane:** independent data-preserving install upgrades,
  schema-drift gate and non-destructive migration smoke tests.
- **Integration:** stage these in order, retain owner aesthetic/device gates,
  keep issue [#120](https://github.com/gamesbyian/Glimpse/issues/120) updated,
  and report only exact-head passing evidence. If no robust preservation
  design is ready, **ship v2 longer rather than making v3 destructive**.

**No automatic deletion, silent FK repair, forced migration, or access to
a person's device is authorized by this planning document.**
