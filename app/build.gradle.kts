plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.github.gamesbyian.glimpse"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.gamesbyian.glimpse"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // A CI key must live in repository secrets, not public Git history or
    // workflow artifacts. Without the environment hook, AGP's throwaway
    // debug key remains the explicit non-upgradeable test-only fallback.
    val ciSigningPath = System.getenv("GLIMPSE_CI_DEBUG_KEYSTORE_PATH")
    if (!ciSigningPath.isNullOrBlank()) {
        signingConfigs.create("ciDebug") {
            storeFile = file(ciSigningPath)
            storePassword = requireNotNull(
                System.getenv("GLIMPSE_CI_DEBUG_STORE_PASSWORD")
            ) { "Missing CI signing store password" }
            keyAlias = requireNotNull(System.getenv("GLIMPSE_CI_DEBUG_KEY_ALIAS")) {
                "Missing CI signing key alias"
            }
            keyPassword = requireNotNull(
                System.getenv("GLIMPSE_CI_DEBUG_KEY_PASSWORD")
            ) { "Missing CI signing key password" }
            storeType = "pkcs12"
        }
        buildTypes.getByName("debug").signingConfig =
            signingConfigs.getByName("ciDebug")
    }

    buildFeatures {
        compose = true
    }

    // Fixed research artwork in debug APK only, not user photo collections.
    sourceSets.getByName("debug").assets.srcDir(
        "../design/research/reel-layout/fixtures/source"
    )
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core:1.19.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("androidx.room:room-paging:2.8.5")
    implementation("androidx.paging:paging-runtime:3.5.3")
    implementation("androidx.paging:paging-compose:3.5.3")
    // Coil 3.3.0 is compiled against Kotlin 2.2.0 like Glimpse's compiler.\n    // Newer 3.6.x pulls incompatible Kotlin 2.4 metadata. Local files only.
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
    // Real Room/OS instrumentation, scoped to data-integrity and migration
    // workflows. Pure policy tests remain under src/test.
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
}

// Stable, user-facing filename. Keep AGP's internal app-debug.apk untouched so
// emulator and instrumentation workflows do not depend on renamed intermediates.
tasks.register<Copy>("packageGlimpseDebugApk") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(layout.buildDirectory.dir("distributions"))
    rename { "glimpse-debug.apk" }
}
