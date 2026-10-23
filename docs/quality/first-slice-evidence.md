# Issue #1: first native slice predictions and evidence

**Status: Android source, CI build and JVM unit task verified; handset/human acceptance unverified.** These hypotheses were documented before an uncoached user observation. This is not a simulated usability study.

## Scope and provenance

Native Android Photo Picker snapshot → local private image copies with SHA-256 verification → Room collections → finite Reel/Grid selector → explicit Home wallpaper application → foreground active-use shuffle. The app checks eligibility while the Activity is STARTED every 15 seconds. It does not promise waking from Doze, an exact five-minute background alarm, automatic whole Google Photos album synchronization, or lock-screen wallpaper support. Reel visuals remain provisional.

The local development container had no Android SDK. GitHub Actions installed the published Android 37.0 package and built successfully. [Run 39904252446](https://github.com/gamesbyian/Glimpse/actions/runs/39904252446), source SHA `73f09984431de22c31993105d7fc0eef8bd1d3c3`, reports `BUILD SUCCESSFUL`, `:app:compileDebugKotlin`, `:app:assembleDebug`, and `:app:testDebugUnitTest` all succeeded. Artifact `glimpse-debug-test-only`, ID `12480286986`, approximately 10.6 MB compressed. No Android runtime/emulator/human test has been performed, and an APK building is not proof of feature correctness. The source code or a static design drawing is not a successful device test.

## Prediction → observation → discrepancy → decision

| Journey | Anticipated first action and result | Observed human/device behavior | Discrepancy / decision |
|---|---|---|---|
| W01: switch collection | Tap named collection chip; preview content changes, wallpaper remains unchanged | Pending uncoached owner | Pending |
| W57: browse known photo | Tap reel slot or Previous/Next; stable selected photo and finite position indicator | Pending uncoached owner | Pending |
| F071: import screenshot | Tap Add photos; native picker; photo appears as private offline snapshot without changing original | Pending real device | Pending |
| F073: share original/crop | User expects Android Sharesheet; not yet implemented in initial slice | Not testable in current prototype | Schedule after first slice |
| W60: immediate/set vs rotate | Distinguish Set Home wallpaper now from Start shuffle; no implicit apply on browse | Pending uncoached owner | Pending |
| Reel vs Grid, 3/60 photos | Grid preserves selected photo; 3-item reel has blank end slots rather than fabricated photos | Pending real device and owner | Pending |
| Interruption and uncertainty | OS unacknowledged/pending apply never advances shuffle bag automatically | Pending fault injection | Pending |
| TalkBack / 200% text | Conventional buttons expose browse/apply without gestures; no clipping or focus traps | Pending accessibility device test | Pending |

## Implementation facts requiring independent validation

- Import limits 50 MB / 120 MP and JPEG/PNG/WebP decoding; copy to app-private storage; dedupe by content hash, memberships committed in Room transaction.
- Browsing only updates UI state. The serialized WallpaperManager command gateway records pending attempts and only consumes an automatic shuffle bag on acknowledged success. Uncertainty blocks automatic retry.
- Only the HOME wallpaper target is used; acknowledged wallpaper ID does not prove later system read-back state.
- Manual Change now resolves a bag entry; Start shuffle schedules first eligibility about five minutes into a foreground-visible session, not an immediate automatic change.

## Next verification gates

1. **Completed source-build gate:** Gradle/Android compilation and basic JVM tests passed and produced a debug APK artifact. Next verify install/startup and behavior on a device.
2. Phone exercise of A001, A003, A004, A008, A011 and A014 (Photo Picker, airplane mode, duplicates, process death, Home-only, accessibility).
3. Compare reel/grid task completion and preview-versus-applied understanding. Include reduced motion, 200% text, three images and large collections.
4. Complete F073 Sharesheet and resumable cloud import only after bounded basic path is stable.

Do not substitute agent speculation for observed user actions or physical-device results.
