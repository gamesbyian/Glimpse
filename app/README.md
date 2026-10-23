# Native Android prototype

Development began from [issue #1](https://github.com/gamesbyian/Glimpse/issues/1). This is a deliberately bounded Compose/Room implementation, **a buildable debug prototype, not an Android-device-validated app or finished visual design**.

With JDK 17 and Android SDK 37 installed:

    ./gradlew :app:assembleDebug :app:testDebugUnitTest

[New native reel CI run 39924084187](https://github.com/gamesbyian/Glimpse/actions/runs/39924084187) completed successfully with `:app:assembleDebug` and `:app:testDebugUnitTest` and uploaded the **glimpse-debug-test-only** artifact. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. A path-filtered workflow also builds/tests native changes and allows manual dispatch.

The early app can create collections, import selected images from Android Photo Picker into verified offline copies, browse with a provisional circular five-aperture paper reel (swipe/tap across a finite collection, Previous/Next accessibility fallback) or Grid, explicitly set Home wallpaper, and start/pause/advance a no-repeat shuffle while the Activity is visible. Reel motion/texture/layout and physical touch behavior are still open; the current code uses immediate selected-ID changes rather than final physical wheel rotation; Android's Doze may stop foreground-only eligibility while the app is not visible.

Code: `data/GlimpseDatabase.kt` (Room), `data/PhotoImporter.kt` (private original copies), `domain/ShuffleBag.kt` (pure ordering), `domain/WallpaperCommands.kt` (single system mutation gateway), `GlimpseModel.kt` (UI state), `domain/ReelWindow.kt` (finite slot/tap math), `CircularPhotoReel.kt` (native wheel drawing and gestures), and `MainActivity.kt` (Compose layout).

Not implemented: entire Google Photos album sync, exact background five-minute scheduling, screenshot crop, Sharesheet, Lock wallpaper, widgets, and provider-specific import retries. Review [evidence and predictions](../docs/quality/first-slice-evidence.md) before claiming any acceptance.
