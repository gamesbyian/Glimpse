package io.github.gamesbyian.glimpse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home's introductory photograph is the *current browsing preview*, not an
 * unverified claim about the actual Android wallpaper. It deliberately scrolls
 * behind the fixed lower-corner reel, while the physical reel never scrolls.
 * A text-only empty-library variant avoids fake stock/sample photos.
 */
@Composable
internal fun GlimpseHomeHero(
    state: GlimpseUiState,
    onBrowse: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = state.current
    val hasPhoto = preview != null
    val largeType = LocalDensity.current.fontScale >= 1.5f
    // The opening image must extend beneath the fixed corner reel on actual
    // devices; a short rectangular card above a dashboard defeats the design.
    val viewportHeight = LocalConfiguration.current.screenHeightDp
    // Empty-library onboarding must not hide Create/Add photos below a
    // nearly full-screen empty illustration. Only genuine photography earns
    // the immersive viewport-height treatment.
    val heroHeight = if (hasPhoto) {
        (viewportHeight - 28).coerceAtLeast(500).dp
    } else {
        (if (largeType) 340 else 248).dp
    }
    val ink = if (hasPhoto) Color.White else Color(0xFF193B3F)
    val paper = Color(0xFFF8EDE1)
    val surface = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .height(heroHeight)
            .clip(surface)
            .background(if (hasPhoto) Color(0xFF2B3A39) else paper)
    ) {
        if (hasPhoto) {
            PhotoPreview(
                photo = preview,
                modifier = Modifier.fillMaxSize(),
                label = "Home selected photograph, preview only; Android wallpaper unchanged by browsing",
                contentScale = ContentScale.Crop,
                decodeMaxEdge = 2048
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .54f),
                        .42f to Color.Black.copy(alpha = .18f),
                        .70f to Color.Black.copy(alpha = .36f),
                        1f to Color.Black.copy(alpha = .73f)
                    )
                )
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Glimpse",
                    color = ink,
                    fontFamily = GlimpseType.editorial,
                    fontSize = if (largeType) 38.sp else 44.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.4).sp
                )
                Text(
                    "YOUR PHOTOS, NEW PERSPECTIVES",
                    color = if (hasPhoto) Color(0xFFFFE6CE) else Color(0xFF986950),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp
                )
                if (state.collectionId != null) {
                    Text(
                        "Photos: ${state.photos.size} • Offline copies",
                        color = ink,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasPhoto) {
                    Text(
                        "Selected photograph · Preview only",
                        color = Color(0xFFFFE6CE),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onBrowse,
                        enabled = state.collectionId != null && state.photos.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF9EBD7),
                            contentColor = Color(0xFF254240),
                            disabledContainerColor = Color(0xFFD9CDC0),
                            disabledContentColor = Color(0xFF6F6256)
                        )
                    ) { Text("Browse photo reel") }
                    TextButton(onClick = onSupport) {
                        Text("Help & privacy", color = ink)
                    }
                }
                Text(
                    when {
                        state.acknowledgedPhoto != null ->
                            "Last wallpaper application acknowledged · Previewing does not apply."
                        state.rotation?.lastAcknowledgedPhotoId != null ->
                            "Earlier wallpaper acknowledgment exists; its original is unavailable."
                        else -> "No wallpaper change has been acknowledged yet."
                    },
                    color = ink.copy(alpha = .88f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
