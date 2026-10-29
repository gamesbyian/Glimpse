#!/usr/bin/env python3
"""Exercise the *actual Room SQL query* against a deterministic 12k-row fixture.

Pure stdlib SQLite; tests logical ordering, collection isolation, real paging
and All/Recent/Favorites mode semantics without allocating bitmap pixels.
This is a SQL contract check, NOT Android Room invalidation, Coil memory,
Pixel scrolling, user behavior, or live OS wallpaper acceptance.
"""
import pathlib
import re
import sqlite3

ROOT = pathlib.Path(__file__).resolve().parents[1]
DAO_SOURCE = ROOT / "app/src/main/java/io/github/gamesbyian/glimpse/data/GlimpseDatabase.kt"
TEXT = DAO_SOURCE.read_text(encoding="utf-8")
QUERY_BLOCK = re.search(
    r'@Query\(\s*((?:"[^"]*"\s*\+?\s*)+)\)\s*fun pageContactSheet',
    TEXT,
    re.DOTALL,
)
if not QUERY_BLOCK:
    raise AssertionError("Unable to extract the live Room contact-sheet query")
QUERY = "".join(re.findall(r'"([^"]*)"', QUERY_BLOCK.group(1)))
for expected in ("p.importedAt", "photo_favorites", ":collectionId",
                 ":recentCutoff", ":nowMillis", ":filter"):
    if expected not in QUERY:
        raise AssertionError("Paging SQL lost contract: " + expected)

NOW = 1_850_000_000_000
DAY = 24 * 60 * 60 * 1000
WEEK = 7 * DAY
COUNT = 12_000

db = sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE photos (
  id TEXT PRIMARY KEY,
  importedAt INTEGER NOT NULL
);
CREATE TABLE memberships (
  collectionId TEXT NOT NULL, photoId TEXT NOT NULL,
  PRIMARY KEY(collectionId, photoId)
);
CREATE TABLE photo_favorites (
  photoId TEXT PRIMARY KEY, markedAt INTEGER NOT NULL
);
CREATE INDEX ix_membership_collection ON memberships(collectionId, photoId);
CREATE INDEX ix_photos_imported ON photos(importedAt, id);
""")

rows = []
favorites = set()
memberships = []
for i in range(COUNT):
    photo_id = f"photo-{i:05d}"
    base = (NOW - 9 * DAY, NOW - 2 * DAY, NOW + DAY)[i % 3]
    imported = base + (i % 997)
    rows.append((photo_id, imported))
    memberships.append(("main", photo_id))
    if i % 5 == 0:
        memberships.append(("alternate", photo_id))
    if i % 13 == 0:
        favorites.add(photo_id)
db.executemany("INSERT INTO photos VALUES (?, ?)", rows)
db.executemany("INSERT INTO memberships VALUES (?, ?)", memberships)
db.executemany("INSERT INTO photo_favorites VALUES (?, ?)",
               [(photo_id, NOW) for photo_id in favorites])
db.commit()

def expected_ids(collection, mode):
    member_ids = {id_ for scope, id_ in memberships if scope == collection}
    visible = [(id_, timestamp) for id_, timestamp in rows if id_ in member_ids]
    if mode == 1:
        visible = [(id_, stamp) for id_, stamp in visible
                   if NOW - WEEK <= stamp <= NOW]
    if mode == 2:
        visible = [(id_, stamp) for id_, stamp in visible if id_ in favorites]
    visible.sort(key=lambda pair: (pair[1], pair[0]))
    return [id_ for id_, _ in visible]

def page(collection, mode, offset, limit):
    params = dict(collectionId=collection, filter=mode,
                  recentCutoff=NOW - WEEK, nowMillis=NOW,
                  limit=limit, offset=offset)
    return [row[0] for row in db.execute(
        QUERY + " LIMIT :limit OFFSET :offset", params
    )]

for collection in ("main", "alternate", "missing"):
    for mode in (0, 1, 2):
        expected = expected_ids(collection, mode)
        assert len(expected) == len(set(expected)), "Duplicate original in collection"
        offsets = {0, 1, 48, 47 * 3, 47 * 150,
                   max(0, len(expected) - 48), len(expected)}
        for offset in sorted(offsets):
            observed = page(collection, mode, offset, 48)
            want = expected[offset:offset + 48]
            if observed != want:
                raise AssertionError(
                    f"{collection=} {mode=} {offset=} expected {want[:3]} got {observed[:3]}"
                )
        assert page(collection, mode, len(expected), 48) == []
    print(f"{collection}: all={len(expected_ids(collection,0))} "
          f"recent={len(expected_ids(collection,1))} "
          f"favorites={len(expected_ids(collection,2))}")
assert len(expected_ids("main", 0)) == COUNT
assert len(expected_ids("main", 1)) == COUNT // 3
assert all(not p.startswith("future") for p in expected_ids("main", 1))
# The focused item's ordinal is computed in SQL, not by downloading 12k
# records. Audit the exact Room query as well as the paged rows it counts.
POSITION_BLOCK = re.search(
    r'@Query\(\s*((?:"[^"]*"\s*\+?\s*)+)\)\s*fun watchPhotoPosition',
    TEXT, re.DOTALL,
)
if not POSITION_BLOCK:
    raise AssertionError("Unable to extract the live Room focus-position query")
POSITION_SQL = "".join(re.findall(r'"([^"]*)"', POSITION_BLOCK.group(1)))
for collection in ("main", "alternate"):
    for sample_id in ("photo-00000", "photo-00002", "photo-09000", "photo-11999"):
        row = next((row for row in rows if row[0] == sample_id), None)
        assert row is not None
        actual = db.execute(POSITION_SQL, dict(
            collectionId=collection, selectedImportedAt=row[1], selectedId=sample_id
        )).fetchone()[0]
        expected = expected_ids(collection, 0)
        expected_set = set(expected)
        desired = expected.index(sample_id) + 1 if sample_id in expected else sum(
            1 for photo_id, stamped in rows
            if photo_id in expected_set and
               (stamped < row[1] or (stamped == row[1] and photo_id <= sample_id))
        )
        assert actual == desired, (collection, sample_id, actual, desired)

print("PASS: actual Room contact-sheet SQL, 12000 rows, pagination, collection scope, "
      "stable order, future-date exclusions and filters")
