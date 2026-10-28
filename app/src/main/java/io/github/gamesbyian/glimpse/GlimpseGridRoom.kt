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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-height contact sheet is a genuine alternative to the physical reel,
 * not a 300dp nested grid buried in the scrolling Home dashboard.
 *
 * The grid scrolls independently beneath a compact, persistent identity and
 * action header. Browsing updates only the selected PhotoRecord. Wallpaper
 * changes require the explicit Home apply action.
 */
@Composable
internal fun GlimpseGridRoom(
    state: GlimpseUiState,
    model: GlimpseModel,
    onBack: () -> Unit,
    onReel: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionId = state.collectionId
    val photos = state.photos
    val selected = state.current
    val index = photos.indexOfFirst { it.id == selected?.id }
    val collection = state.collections.firstOrNull { it.id == collectionId }?.title
        ?: "Your collection"
    val paper = Color(0xFFF7F0E6)
    val ink = Color(0xFF25423F)

    Column(
        modifier = modifier.fillMaxSize().background(paper).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Back to Home")
            }
            OutlinedButton(
                onClick = onReel,
                enabled = photos.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Browse photo reel") }
        }
        Text("THE CONTACT SHEET", color = Color(0xFF986950),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium, letterSpacing = 1.3.sp)
        Text(collection, color = ink, fontFamily = GlimpseType.editorial,
            fontSize = 28.sp)
        Text(
            "${photos.size} photographs · Selection only; wallpaper unchanged",
            color = Color(0xFF695D52),
            style = MaterialTheme.typography.labelMedium
        )
        if (photos.isEmpty()) {
            Text("No photos in this collection yet. Add photos from Home.",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("IN FOCUS · ${index + 1} OF ${photos.size}",
                        color = ink, style = MaterialTheme.typography.labelLarge)
                    PhotoPreview(
                        photo = selected,
                        modifier = Modifier.fillMaxWidth().height(126.dp),
                        label = "Current contact sheet selection, preview only"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDetails,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Photo details") }
                        Button(
                            onClick = model::applyPreview,
                            enabled = !state.busy && selected != null,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Set Home wallpaper now") }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 102.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    val active = photo.id == selected?.id
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(128.dp)
                            .border(if (active) 3.dp else 1.dp,
                                if (active) Color(0xFF24665E)
                                else Color(0xFFC6B7A4), shape)
                            .clickable {
                                model.browsePhotoInCollection(collectionId, photo.id)
                            }.padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PhotoPreview(
                            photo = photo,
                            modifier = Modifier.fillMaxSize(),
                            label = "Choose photo in collection: " + photo.id.take(8)
                        )
                    }
                }
            }
        }
    }
}
