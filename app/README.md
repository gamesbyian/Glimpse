# Native Android prototype

Development began from [issue #1](https://github.com/gamesbyian/Glimpse/issues/1). This is a deliberately bounded Compose/Room implementation, **not a tested APK or finished visual design**.

With JDK 17 and Android SDK 37 installed:

    ./gradlew :app:assembleDebug :app:testDebugUnitTest

If successful, the debug APK is `app/build/outputs/apk/debug/app-debug.apk`. A path-filtered workflow also builds/tests native changes and allows manual dispatch.

The early app can create collections, import selected images from Android Photo Picker into verified offline copies, browse with a finite five-window reel or Grid, explicitly set Home wallpaper, and start/pause/advance a no-repeat shuffle while the Activity is visible. The reel geometry is exploratory and has no final paper animation; Android's Doze may stop foreground-only eligibility while the app is not visible.

Code: `data/GlimpseDatabase.kt` (Room), `data/PhotoImporter.kt` (private original copies), `domain/ShuffleBag.kt` (pure ordering), `domain/WallpaperCommands.kt` (single system mutation gateway), `GlimpseModel.kt` (UI state) and `MainActivity.kt` (Compose).

Not implemented: entire Google Photos album sync, exact background five-minute scheduling, screenshot crop, Sharesheet, Lock wallpaper, widgets, and provider-specific import retries. Review [evidence and predictions](../docs/quality/first-slice-evidence.md) before claiming any acceptance.
