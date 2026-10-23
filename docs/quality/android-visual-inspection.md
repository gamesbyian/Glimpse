# Glimpse: actual Android visual-inspection atlas

Adapted from the useful, lean parts of ROYGBIV's UI atlas: **fixed fixture identity, multiple meaningful real screen states, native screenshots, accessibility hierarchy, provenance and a browsable gallery**.

## Run

GitHub Actions → [Glimpse native visual atlas](https://github.com/gamesbyian/Glimpse/actions/workflows/android-visual-atlas.yml) → Run workflow. Choose **smoke** for initial 22-image Reel and a scrolled hero; choose **full** for those plus scrolled controls/status, a 3-item reel, Grid and 200% Android text scaling. This specialized emulator run does *not* run on every app push or PR. The first workflow-file commit triggers an initial proof run.

Download the **glimpse-android-visual-atlas** workflow artifact, unzip it and open **index.html**. The contact sheet links the raw PNG screenshots, UIAutomator accessibility XML for each state and **manifest.json** with source SHA, emulator dimensions, scenario labels and clickable-control geometry warnings (under 44dp). Screenshots are native captures of the actual debug APK's MainActivity, not recreated sketches or browser approximations.

**2026-10-23 reliability note:** The first capture run compiled but failed while waiting for the old UIAutomator-based fixture check. The second capture run could not pass the emulator runner's post-boot initialization: its Ubuntu user lacked permission for `/dev/kvm`, forcing a 13-minute unaccelerated startup before Android settings commands failed. The workflow now grants temporary KVM access, uses a smaller emulator boot profile, and waits for an explicit debug fixture marker. **Until a complete run succeeds, no native screenshot gallery is verified.** Screenshot assertions remain separate from real handset acceptance.

## Truth and privacy boundaries

- The debug-only VisualFixtureLauncher seeds real Room records for 0, 3, 22 or 60 images, then launches the **same production Compose screen**. It decorates the six already published source art images with distinct numeric IDs. These are *fixture derivatives*, never your imported personal photographs.
- The launcher is excluded from release APKs. The script resets **all Glimpse app data** to build a clean scenario, and adjusts screen size/density/font scale. **Run it only on a disposable emulator or dedicated test device, never on your personal phone**.
- CI uses an emulator configured to Pixel 6 Pro-like 1440 × 3120 / 560 dpi with an Android API 34 emulator on a smaller boot profile and an app compiled against SDK 37. Emulator appearance is actual software execution, **not physical Pixel hardware performance or user acceptance**.
- The screenshots provided by the owner on October 23 remain in the conversation. We have not republished them or personal image thumbnails in this public repository.
- Screenshots are **implementation evidence**. [Approved design references](../../design/reference/five-window-reel.svg), [critical interaction audit](../design/reel-ux-audit.md), and [human/device observations](first-slice-evidence.md) remain separate evidence types. Do not pretend a screenshot passes a human study or a static picture proves drag/interrupt gestures.

## Inspection checklist

1. Compare shape: genuinely round/cropped paper reel with five **large** apertures, or a straight thumbnail strip?
2. Are real photos and a stationary hero visually dominant? Does collection creation/status copy consume the first viewport?
3. Are all 22 photos accessible through meaningful reel rotation? No fake repeated IDs, clipped windows or missing entries.
4. Check a short collection, first and last photo, Grid ↔ Reel state, and 200% text; inspect click-target bounds and clipping in XML.
5. Keep outcome labels honest: browsing never applies wallpaper, and an Android API acknowledgment is not a continuous readback guarantee.

## Local command

With the Android SDK, JDK 17, adb, Python 3 and a **disposable** emulator running:

    ./gradlew :app:assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    python3 scripts/capture-android-atlas.py --mode full

See [reference acceptance](reference-acceptance.md) and the [implementation queue](../planning/implementation-queue.md) for which screenshots should be compared and why. The gallery is observational rather than an automatic image-difference release gate.
