package io.github.gamesbyian.glimpse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Focused photo-reel room. Home remains a usable collection/shuffle overview,
 * while the physical disc gets an immediately reachable primary surface.
 *
 * This uses exactly the same PhotoRecord IDs, BrowseCursor and WallpaperCommands
 * as the Home Reel/Grid. Entering, moving, leaving and returning is preview-only.
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
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
        if (collectionId == null || state.photos.isEmpty()) {
            Text("This collection has no photographs yet. Return to Home to add some.")
        } else {
            // Stationary hero preview. The user explicitly applies wallpaper;
            // reel selection and either navigation button never do it.
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
            CircularPhotoReel(
                records = state.photos,
                selectedId = selected?.id,
                onSelect = { id -> model.browsePhotoInCollection(collectionId, id) },
                showHeading = false
            )
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
}
