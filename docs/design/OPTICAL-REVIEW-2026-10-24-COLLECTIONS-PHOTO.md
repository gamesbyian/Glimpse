# Current-generation Collections photo-cover optical review · 2026-10-24

**Evidence class:** independent direct visual inspection of one source-hashed proposed 401×890 phone crop. **Not** real Android screenshot, human acceptance or asset licence verification beyond checked-in fixture provenance.

## Source and proof

- Exact editable SVG: [`collections-photo-covers.svg`](../../design/proposals/whole-product-candidates/collections-photo-covers.svg).
- SVG SHA-256: `e318dbbfa331fde235102430e2ccb750cca8acb340fa2b3e0b6af3ba65e31daa`.
- Phone review: [`collections-photo-cover.jpg`](../../design/reviews/targeted-2026-10-24/collections-photo-cover.jpg) and [exact provenance JSON](../../design/reviews/targeted-2026-10-24/collections-photo-cover-source.json).
- Review JPEG SHA-256: `eb835afe85f3a225a4e216c708f629faeea79ae28df74c63b1ce59ce8f161a21`.
- Successful focused authoring/raster workflow: [#40117807422](https://github.com/gamesbyian/Glimpse/actions/runs/40117807422). Source and hash included in successful full atlas/reviewer update [#40117824560](https://github.com/gamesbyian/Glimpse/actions/runs/40117824560).

## Directly observed phone pixels

Three stacked white cards carry **different original source-backed photographic cover images**, human-readable names, truthful illustrative counts (Rainbows 22, Golden Hour 50, Patterns 3), a visible **Open collection** action and separate search/sort affordances. The active Rainbows card is labeled *Active · local* in words. **Add photographs** and **New collection** remain two distinct full-width-class bottom actions, above the four-tab navigation. The visible photo-paper frames read as static covers, not five-window movable mini-reels. No wallpaper action is suggested by selecting a cover.

First inspection exposed a **missing font glyph box** where the sort-down triangle should have appeared. Source generator repaired it using an explicit SVG vector chevron and similarly replaced the unreliable status/battery glyph with native SVG shapes. I opened the regenerated phone JPEG. **The Sort control now has a legible down-chevron and the tiny status element is vector rendered without missing glyph boxes.** Cards, controls, labels and bottom navigation display without obvious clipping at the one sampled 401×890 raster setting. This specific font-portability issue is resolved at the source SHA above.

## Constraints and unresolved review decisions

- The three cover images are independent public research fixtures checked into the repo. Their use here is **illustrative**, not proof that every sample collection contains the specified image or that provider permissions exist. The collection list does not create an implied Google Photos album sync or apply wallpaper.
- Accessibility at 200% text, actual 48dp touch bounds, filtered/empty/multicollection scrolling, TalkBack, dynamic source grants and Android import/gallery navigation remain unverified.
- The owner must decide whether static photographic paper-frame covers retain enough of Glimpse's identity without introducing a second wheel metaphor. This version deliberately avoids an extra moving reel for a simple list.
- Current-generation source is a **provisional reviewer candidate**, not normative approved visual target. [Candidate set](../../design/atlas/whole-product-review-selection.html) now links it as Collections default, retaining [the old schematic plate](../../design/proposals/plates/collections.svg) as a comparison alternative.

**Audit integrity:** The 60-question Cycle A stays 18 source-level PASS / 8 ISSUE / 34 UNVERIFIED. One clearer draft Collections view and a fixed icon do not prove an app-wide UX result.
