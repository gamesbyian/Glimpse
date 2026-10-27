package io.github.gamesbyian.glimpse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Real independent Grid mode, not a 300dp sub-scroller buried in Home.
 * It selects the same PhotoRecord IDs as the focused Reel and Detail.
 */
@Composable
internal fun GlimpseGridRoom(
    state: GlimpseUiState,
    model: GlimpseModel,
    onReel: () -> Unit,
    onHome: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionId = state.collectionId
    val name = state.collections.firstOrNull { it.id == collectionId }?.title
        ?: "Your collection"
    val selected = state.current
    val selectedIndex = state.photos.indexOfFirst { it.id == selected?.id }
    val gridState = rememberLazyGridState()

    // On first arrival in a new collection, reveal the previously selected
    // photo. Do not scroll/jump after every tap as the user explores the Grid.
    LaunchedEffect(collectionId) {
        if (selectedIndex >= 0) gridState.scrollToItem(selectedIndex)
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onReel) { Text("Back to Reel") }
            TextButton(onClick = onHome) { Text("Home") }
        }
        Text(
            "THE COLLECTION",
            color = Color(0xFF97684D),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp
        )
        Text(
            "Every photograph.",
            fontFamily = GlimpseType.editorial,
            color = Color(0xFF193B3F),
            fontSize = 29.sp
        )
        Text(
            "$name · ${state.photos.size} photos",
            color = Color(0xFF65584F),
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.photos.isEmpty() || collectionId == null) {
            Text("No photographs yet. Return Home to import some.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                state = gridState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.photos, key = { _, photo -> photo.id }) { index, photo ->
                    val chosen = photo.id == selected?.id
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(132.dp)
                            .border(
                                if (chosen) 3.dp else 1.dp,
                                if (chosen) Color(0xFF236760) else Color(0xFFB9AC9A),
                                shape
                            )
                            .semantics {
                                contentDescription = "Photograph ${index + 1} of " +
                                    "${state.photos.size}" +
                                    if (chosen) ", selected for preview" else ", choose to preview"
                            }
                            .clickable {
                                model.browsePhotoInCollection(collectionId, photo.id)
                            }.padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PhotoPreview(
                            photo = photo,
                            modifier = Modifier.fillMaxSize(),
                            label = "Photograph ${index + 1} of ${state.photos.size}"
                        )
                    }
                }
            }
            Text(
                "Previewing ${selectedIndex + 1} of ${state.photos.size} · Wallpaper unchanged",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF494A43)
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
        }
    }
}
