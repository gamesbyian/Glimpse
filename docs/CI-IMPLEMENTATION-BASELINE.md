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
