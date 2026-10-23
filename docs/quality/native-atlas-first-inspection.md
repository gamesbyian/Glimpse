## Native visual inspection: first artifact and correction (2026-10-23)

**Source evidence:** [run 39924384542](https://github.com/gamesbyian/Glimpse/actions/runs/39924384542), **partially successful capture**, not a clean passing visual atlas. This run used Android API 34 in a Pixel 5 AVD profile, displayed through 1440×3120 / 560 dpi screen overrides. [Download its actual capture artifact](https://github.com/gamesbyian/Glimpse/actions/runs/39924384542) while retained.

**What was independently observed in the images and XML:**

- Debug Photo Picker fixture initialization to Room completed for 22 and 3 records. The 22-photo view visibly contains a large stationary image preview and cropped circular paper-wedge image selector beneath it.
- The screen is still vertically long and setup/collection management consumes substantial first-viewport height; this is not the approved final task hierarchy.
- All first four screenshots are **invalid as clean Glimpse UI evidence**: Android raised a system modal reading **“Pixel Launcher isn't responding”**, darkening the underlying app and covering interaction targets. UIAutomator therefore exposed only the modal's **Close app / Wait** actions, preventing the `View all · Grid` step.
- The 3-photo initial screenshot visibly reports **Photos: 0** despite its committed Room fixture. It was taken before the asynchronous UI collection watch had populated; this is a **capture-readiness flaw**, not evidence of failed persistence or missing imported photos.
- Image source was a deterministic numbered public design fixture, not user-imported Google Photos. Do not mix these screenshots with the physical-owner 22-photo report.

**Repairs committed:** Give KVM permission for fast emulator boot (reducing startup from ~13 minutes to ~40 seconds on this run), select a plain AOSP Android system image instead of Google APIs Pixel Launcher, detect and dismiss/forbid Android ANR overlays, verify both fixture completion **and the actual UI photo count** before capture, retain invalid-capture diagnostics.

**Still required before passing:** complete all 22-item Reel/Grid, three-item, and 200% font states without any system overlay, verify the collection index and photo count on each frame, and inspect visual fidelity against the approved proposal direction. Current drawing still lacks animated physical rotation, tested drag interruption and complete large-text/device evidence.

**Evidence labels:** This is observation of actual emulator software, not a final screenshot reference or device/human approval.
