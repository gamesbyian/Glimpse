package io.github.gamesbyian.glimpse.debug

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.GlimpseManualRecoveryPanel

/**
 * Debug-only real Compose host for an isolated Android consent journey.
 * No entry point in production and no background recovery invocation.
 * The test creates only disposable synthetic app-owned PNG/Room fixtures.
 */
class ManualRecoveryProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true)
        ) { "Recovery consent probe must run on an emulator only" }
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    GlimpseManualRecoveryPanel(this@ManualRecoveryProbeActivity)
                }
            }
        }
    }
}
