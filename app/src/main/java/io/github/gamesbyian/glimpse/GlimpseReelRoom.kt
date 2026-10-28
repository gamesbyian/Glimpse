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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fixed-edge photo reel: the page scrolls BEHIND the reel, while its screen-space
 * centre and bounding box stay stationary. Never put the wheel in a scrollable
 * Column, and never attach a footer underneath the bottom-edge control.
 *
 * The extra scrollable tail lets the last action rise completely above the
 * physical paper disc without moving the disc itself.
 */
@Composable
internal fun GlimpseReelRoom(
    state: GlimpseUiState,
    model: GlimpseModel,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionId = state.collectionId
    val collectionName = state.collections.firstOrNull { it.id == collectionId }?.title
        ?: "Your collection"
    val selected = state.current
    val currentIndex = state.photos.indexOfFirst { it.id == selected?.id }
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val hasReel = collectionId != null && state.photos.isNotEmpty()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp,
                    bottom = if (hasReel) 330.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back to Home") }
            Text(
                "THE PHOTO REEL",
                color = Color(0xFF986950),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Turn a new page.",
                color = Color(0xFF193B3F),
                fontFamily = GlimpseType.editorial,
                fontSize = 30.sp
            )
            Text(
                collectionName,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF665B51)
            )
            if (!hasReel) {
                Text("This collection has no photographs yet. Return to Home to add some.")
            } else {
                PhotoPreview(
                    photo = selected,
                    modifier = Modifier.fillMaxWidth().height(if (largeType) 150.dp else 185.dp),
                    label = "Selected photo preview in dedicated Reel browser"
                )
                Text(
                    "Previewing ${currentIndex + 1} of ${state.photos.size} · Wallpaper unchanged",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { model.stepPhoto(-1) },
                        enabled = currentIndex > 0
                    ) { Text("Previous photo") }
                    OutlinedButton(
                        onClick = { model.stepPhoto(1) },
                        enabled = currentIndex >= 0 && currentIndex < state.photos.lastIndex
                    ) { Text("Next photo") }
                    OutlinedButton(onClick = onGrid) { Text("View all · Grid") }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = onDetails) { Text("Open photo details") }
                    Button(
                        onClick = model::applyPreview,
                        enabled = !state.busy && selected != null
                    ) { Text("Set Home wallpaper now") }
                }
                Text(
                    "Only Set Home wallpaper requests an Android change. Glimpse cannot " +
                        "guarantee that the system wallpaper was not changed externally.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(state.message, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (hasReel) {
            CircularPhotoReel(
                records = state.photos,
                selectedId = selected?.id,
                onSelect = { id -> model.browsePhotoInCollection(collectionId, id) },
                modifier = Modifier.align(Alignment.BottomStart),
                showHeading = false
            )
        }
    }
}
