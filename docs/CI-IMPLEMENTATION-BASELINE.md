# Glimpse CI modernization baseline

Recorded 2026-10-31 against the latest 100 GitHub Actions runs before CI changes.
Source: `GET /repos/gamesbyian/Glimpse/actions/runs?per_page=100`.
The measured duration is `updated_at - created_at`, including queuing and artifact transfer; it is **not** billed runner time, nor a controlled warm-cache benchmark.

| Workflow | Successful / failed / cancelled in sample | Successful elapsed median |
| --- | ---: | ---: |
| Native Android slice | 17 / 0 / 11 | 2.9 minutes |
| Native visual atlas | 5 / 1 / 0 | 27.4 minutes |
| API29 private originals | 9 / 0 / 2 | 3.3 minutes |
| Installed-data upgrade | 9 / 1 / 9 | 4.0 minutes |
| Room instrumentation | 11 / 0 / 2 | 3.3 minutes |
| Real OS single-photo picker | 4 / 0 / 2 | 4.8 minutes |
| Home widget | 3 / 1 / 8 | 5.4 minutes |
| Static implementation coverage | 5 / 0 / 0 | 0.1 minutes |

## Changes and acceptance boundaries

The governing policy is [CI-ENGINEERING-STANDARD.md](CI-ENGINEERING-STANDARD.md). The focused implementation PR is #159.

1. Dispatch safety: the previous visual-reference workflow did not check **pull requests** at all, only pushes to main. It now checks relevant design PRs. Its global concurrency group could cancel distinct PRs; now it is scoped by PR/ref. Incoming Share, Clipboard and batch-picker emulator jobs now have the same per-PR cancellation discipline.
2. Expensive, independent assurance: Room, API29 and populated-upgrade verification are triggered by alternate immutable-original writers, recovery code and shared Gradle/build inputs, rather than only a narrow list of historic entry points. The visual atlas checks Android resources, manifest and app build configuration; batch-picker tests react to real import logic.
3. Remove demonstrable duplicate task *requests*: specialized emulator jobs no longer request the JVM suite when any app change already triggers the native JVM suite. The native job requests packaging, JVM tests and lint in one Gradle invocation, avoiding a second independent startup. Emulator runs still build/install real Android APKs and retain independent OS evidence. There is **no claim of cross-run compilation reuse**.
4. Selection logic regression suite: `scripts/test_ci_dispatch.py` examines actual YAML triggers and checks docs-only negative controls, rename unions, cross-boundary publication paths, build/Gradle fallback, visual changes, event privilege, branch isolation and merge-group native gate. An always-on lightweight workflow runs this without Android/Gradle, including for documentation-only PRs.
5. Failure intelligence: affected Android workflows emit versioned JSON and a compact job summary if a step fails. JUnit metadata can identify the first observed failed test; absent JUnit it reports `unclassified` instead of fabricating an error. Raw media, full logs, screenshots and fixture bytes are never copied into the receipt; run links preserve complete diagnostic access.

## Remaining optimization work

- Compare exact-head run counts, outcomes, first actionable failures, times and any flake retries after PR #159 settles. **No speedup measured yet.**
- Profile duplication of SDK installation, Gradle configuration and APK compilation across separate emulator workflows. Sharing artifacts across workflows requires explicit source SHA/signature provenance checks, fork isolation and retained independent failure domains. Do not consolidate without measured benefit.
- Treat the visual atlas's approximately 27-minute median separately; optimize capture coverage, emulator startup and image/video transfer without relaxing owner optical or physical-device acceptance.
- Preserve separate oldest-API publication, populated Room upgrade, native data recovery, real Photo Picker and widget evidence. Mock and screenshot checks are not substitutes for real device/owner validation.
- Verify required check contexts and branch-protection/merge-queue rules against actual repository settings before declaring CI fully compliant. No observed permission to edit those rules is implied by this PR.


## 2026-11-02 exact-head job-time measurement (candidate #185)

Source SHA: `91a56bd2791a05748d59f00a587d789d66d0f418`.
GitHub REST `/actions/runs?head_sha=...` plus each run's `/jobs` timestamps,
measured independently of log messages and without assuming billable-minute
rounding. The following seven completed **real** workflows overlap in time.
The native visual atlas was still running when this snapshot was taken, so the
aggregate is deliberately a lower bound.

| Exact-head workflow | Runner occupancy | Workflow elapsed |
| --- | ---: | ---: |
| Always-on CI contract | 0.08 min | 0.13 min |
| Native build, JVM tests and lint | 1.77 min | 1.82 min |
| API29 private-original publication | 2.43 min | 2.48 min |
| Actual Room/schema instrumentation | 3.03 min | 3.08 min |
| Real OS two-photo picker | 4.23 min | 4.30 min |
| Installed-data old-to-new APK upgrade | 3.83 min | 3.87 min |
| Real OS single-photo picker | 4.80 min | 5.02 min |

**Total observed runner occupancy: 20.2 minutes, excluding atlas.** This
is the sum of GitHub job start-to-completion durations, not an assertion about
GitHub invoice rounding. The critical-path time for these seven concurrent jobs
was approximately 5 minutes, not their summed occupancy. Queuing affects
real elapsed time; this is a single SHA, not a statistically controlled
comparison against the older 2026-10-31 median sample.

Candidate build duplication worth measuring experimentally: 1.40 min native
Gradle test/lint, 1.18 min Room test compile, 1.10 min single-picker APK build,
1.17 min two-photo APK build, and 1.08 min each for the separately signed
old and candidate APKs in the actual populated upgrade. A shared artifact
must have exact source SHA, provenance/digest verification and fork isolation;
the old-version upgrade build and independent Android OS proofs must remain.
**No compilation reuse optimization or runner-minute saving is claimed yet.**
See issue #161 for the live evidence ledger and next experiment.

## 2026-11-03 real native atlas optimization (merged #209)

| Genuine API34 visual-atlas job | Observed elapsed | Actual capture inventory |
| --- | ---: | --- |
| [Before #41562319210](https://github.com/gamesbyian/Glimpse/actions/runs/41562319210) (PR source `814dba8a`) | ~27m 58s | 46 real Android PNGs + 46 UIAutomator XML |
| [After #41583239867](https://github.com/gamesbyian/Glimpse/actions/runs/41583239867) (PR source `0fcac291`, synthetic merge `7518b6f8`) | ~20m 12s | 46 real Android PNGs + 46 UIAutomator XML |

**Difference on these comparable successful runs: approximately 7m46s / 27.7% elapsed reduction.** This is one before/after observation, not a controlled distribution or guaranteed percentage. Both ran a real API34 KVM emulator, debug APK, 46 screenshot states, and complete original Room/Photo Picker/Save As/large-text journeys. New output retained 94 files (46 PNG, 46 XML, one HTML, one JSON), valid PNG/XML, zero manifest errors and valid artifact ZIP integrity. The [merged #209](https://github.com/gamesbyian/Glimpse/pull/209) eliminated redundant consecutive UIAutomator dumps in normal click/capture paths, repaired ANR bounds and preserved fresh ANR recapture on actual system obstructions. Six fast harness tests and the exact-head CI contract passed.

The new manifest reported **319 hierarchy dumps totaling 636.219 seconds**, max single 2.186 seconds, making UIAutomator interaction the dominant measured remaining hotspot. The 46 `capture()` calls themselves consumed ~133s, while capture/navigation altogether occupied more than 17 minutes. Smaller AVD boot savings (around 25 seconds observed) alone would not materially improve this critical path.

Subsequent draft [#210](https://github.com/gamesbyian/Glimpse/pull/210) experiments with shorter post-input settling and a cheap automatic real-PNG/XML/SHA artifact gate. Its final-head Android outcome is **pending**, so no further measured speedup or acceptance is claimed here.

### Superseded-commit waste during #210 preparation

Draft #210's intermediate source commits launched full atlas CI, then later commits canceled the obsolete same-ref jobs. For example [run #41586367764](https://github.com/gamesbyian/Glimpse/actions/runs/41586367764) was canceled after more than five minutes had elapsed, and [#41586825061](https://github.com/gamesbyian/Glimpse/actions/runs/41586825061) after about 25 seconds. Cancellation correctly prevents stale results from being used for merge, but does not refund runner work already performed. This is a concrete reason to batch all coherent harness/test/workflow changes **before** opening or updating an expensive PR, then run a single required final-head acceptance. Never skip that final acceptance to reduce cancellation costs.

See [issue #161](https://github.com/gamesbyian/Glimpse/issues/161) for the separate, measured Gradle compilation fan-out and test-topology decisions.
