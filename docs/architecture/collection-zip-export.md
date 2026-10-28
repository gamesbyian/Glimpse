# Portable collection ZIP export (development contract)

This is a user-initiated export of immutable originals associated with exactly one collection, not a complete library backup or restore implementation. Archive contains `manifest.json` (format `glimpse.collection.originals`, version 1, title, SHA-256, MIME, byte size, safe relative path) and byte-identical `originals/<sha>.<extension>` files. File names are constructed from SHA-256 and trusted imported MIME; arbitrary user paths do not enter ZIP entries. A photo can appear in multiple exported collections without duplicating the live stored source.

Sources are all verified before export begins, and rehashed during bounded streaming. A failed destination may still contain a partial archive, which is *not* safe to treat as a backup and must be deleted through its document provider by the user. ZIP's manifest may contain the collection title and per-photo hashes; exporting it deliberately shares that information with the chosen destination provider. Do not add current wallpaper, uncertain OS state, active shuffle bag, credentials or stored provider URI data to this format without a separate explicit versioned contract.

**Acceptance:** automated JVM exact-byte archives, strict ZIP entry names, manifest escaping, corrupt-source preflight; real Android DocumentsProvider Save As and restore flows are separate gates. The writer alone is not a user feature until surfaced through a working Android create-document flow.

## Restore implementation and failure boundaries

The Android OpenDocument chooser provides a temporary content URI. The reader
accepts only a manifest-first v1 ZIP with strictly relative SHA-derived original
paths, matching filenames, MIME, byte lengths and digests, at most 20,000 images,
at most 50 MiB per image and at most 1 GiB of expanded originals. Its JSON manifest
is capped at 8 MiB. Additional entries, unexpected paths, duplicate originals
and incorrect counts fail closed. The Android decoder then checks actual MIME
and pixel budgets before verified files are finalized.

The fully validated image set is registered in a **new, separate collection**
using one Room transaction. Any earlier collection, original, wallpaper
acknowledgment and shuffle intent remains untouched. Staged temporary files
are deleted on completion/failure; crash-recovery prunes aged import-named
partials. Files finalized before an interrupted database transaction may remain
as verified unindexed originals, never overwritten or silently deleted. A
restored ZIP is not a full backup: wallpaper state and provider grants do not
travel with it.
