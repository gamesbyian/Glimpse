# Audit optical review: current R01/R02/R03 candidates

**Reviewed against GitHub Actions run [40033741630](https://github.com/gamesbyian/Glimpse/actions/runs/40033741630)**, source commit `d41d9ef7d51cff370e5d1d339a1f6caab78ec81e`. Successful validation and source-hashed raster generation: 203 baseline images, 36 core references, 23 connected journeys, two R03, six R01 and two R02. **Actual contact-sheet raster inspected for R01 and R02**. This is optical reconnaissance, not final native, accessibility or owner approval.

## R01: six-state contact sheet, confirmed deficiencies

1. **Photo windows too small.** A wide and cropped portion of the circular paper disc is visible, but the five apertures render small within that disc, leaving a disproportionate blank paper area. The result looks like an interaction schematic rather than a product-ready photographic reel. **High.**
2. **Different photo IDs use visually repeated rainbow artwork.** The captions and finite indexing are correct, but p12, p13, p14 etc look identical. This defeats a core audit requirement: a user must visually verify identity continuity from hero to focused aperture to Grid. **High.**
3. **Hero/focused-aperture pixels are not matched to a real photographic source**. Both are vector rainbows, not the intended collection images. A source-ID assertion cannot prove content identity. **High.**
4. **Circular-window versus photographic wedge aesthetic conflict:** newer round chips must not silently replace the stronger X05 large photographic aperture language. Need a genuine side-by-side owner comparison with native device crop, not a choice made by validator. **High.**
5. **Previous and Next reachability** is displayed but state boundary ergonomics and disabled affordances remain only illustrative. At N=0 the Previous area turns into an Add photos prompt, which is a different control contract and needs review. **Medium.**
6. **Some type and footer labels are tiny** at full 401×890 crop, including audit disclaimer, bottom navigation and action hint text. Actual 200% font and target measurements are still absent. **High accessibility evidence gap.**

**Positive source observations:** fixed top pointer, explicit N and index, no duplicated photos for N=0/1, disabled Next at the finite last item, snapped selection p12→p13 and visible drag variant. These are *schematic* strengths, not approval.

**Required R01 revision:** keep the exact finite mapping but replace all placeholder rainbow chips with distinct, source-proven photographs, enlarge apertures and compare against X05 photo-backed disc. Keep motion/gesture state and selected-hero relationship independently testable.

## R02: crop-pending vs copy-saved contact sheet

- **Improvement:** clear not-saved/save-acknowledged labels and buttons, independent original p12 and saved edited p12c, and visually distinct 150% crop in the copy-saved image.
- **Remaining defects:** both scenes show stylized vector rather than the X07 original photographic pixels, so true byte-preservation is only asserted and not proven. Tiny secondary information and compact bottom navigation need mobile scale review. The flow still lacks a linked Save failure, source/copy choice, system share handoff and Return/Dismiss, with native semantics and focus not verified.
- **Disposition:** retain as **source-level illustrative proof** of a critical lifecycle transition, not a visual acceptance of the complete Edit/Share journey.

## R03 status

Both R03 phone crops were inspected in the same source-hashed output. The preview p12 versus last-confirmed Home p04 separation is legible, and the unknown request uses a different warning presentation and an Inspect system wallpaper action. The difference is semantically clear, but the same stylized rainbow image is used in both candidates; tiny explanatory microcopy and compact bottom navigation need 200% text/device review. No OS readback or wallpaper-state trace was observed. **Source-level visual distinction present; native result truth and owner comprehension remain unverified.**

## Audit scoring

No GUX result is upgraded by render success or clearer proposals alone. Preserve first cycle **18 PASS / 8 ISSUE / 34 UNVERIFIED** until the exact source and native evidence or owner-visible interpretation task supports rescoring. This report provides the concrete redesign inputs and traceable defect descriptions.


## Revisions made in response to the above optical findings (source only, pending newest raster)

**R01 v2:** The small circular photo-chip layout from the six first-pass R01 boards is superseded for *visual proposal review* by six source-editable, **X05 paper-wedge photographic reels**, generated deterministically from the six checked-in public reference images. [Mapping, source labels and state inventory](../../design/proposals/audit-revisions/R01-PHOTOGRAPHIC-CONTINUITY.md). The photo near the pointer is now source-identical across rest/drag and correctly changes to p13's different source after snap. Empty and single-item boards likewise use the physical paper wheel with empty apertures. These changes address the earlier *source composition* defects, but do not close the native reel or independent 401×890 optical acceptance gates.

**R02 v2:** All four proposed before/after/save/share/return illustrations now embed the exact X07 original p12 JPEG, and p12c uses a visibly different crop of those exact bytes. The Share-choice proposal now has two actual photographic thumbnails, and its edited thumbnail crop origin was corrected to stay inside its clipping rectangle. The validator checks source bytes against X07, crop transforms, and non-acknowledged share recipient outcomes. The latest proof must visually inspect this four-state revision, not rely on the earlier two-vector contact sheet.

**Review tool:** [12-scene mobile-first comparison atlas](../../design/atlas/audit-visual-comparisons.html) holds the earlier/revised phone-side drawing pairs without relying on annotation panels. [Native QA matrix](R01-R02-NATIVE-EVIDENCE-MATRIX.md) defines the next separate Android proof gate. This section records source deltas, not a new counted GUX PASS or owner approval.
