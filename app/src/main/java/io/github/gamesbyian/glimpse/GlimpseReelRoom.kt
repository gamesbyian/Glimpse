package io.github.gamesbyian.glimpse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Editorial photo browser: the *selected* collection image supplies the
 * full-screen photographic scene. It remains a PREVIEW and never claims to
 * represent the current Android wallpaper. The reel is a true viewport-fixed
 * physical object; action copy can scroll behind it without shifting it.
 *
 * The dark gradient is a readability treatment for arbitrary user-supplied
 * photos, not a baked sample photograph. The second screen layer contains
 * explicit Previous/Next/Grid and Apply controls for non-gesture navigation.
 */
@Composable
internal fun GlimpseReelRoom(
    state: GlimpseUiState,
    model: GlimpseModel,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onGrid: () -> Unit,
    modifier: Modifier = Modifier,
    contentInsets: PaddingValues = PaddingValues(0.dp)
) {
    val collectionId = state.collectionId
    val collectionName = state.collections.firstOrNull { it.id == collectionId }?.title
        ?: "Your collection"
    val selected = state.current
    val currentIndex = state.photos.indexOfFirst { it.id == selected?.id }
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val hasReel = collectionId != null && state.photos.isNotEmpty()
    val paleControls = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFF7EBD8),
        contentColor = Color(0xFF233C3B),
        // Endpoints must remain visibly readable over arbitrary user photos.
        // Disabled state has a muted *but opaque* ink/paper pair.
        disabledContainerColor = Color(0xFFE8DBCC),
        disabledContentColor = Color(0xFF655A50)
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))) {
        if (hasReel) {
            PhotoPreview(
                photo = selected,
                modifier = Modifier.fillMaxSize(),
                label = "Full-screen selected photo preview. Wallpaper unchanged.",
                contentScale = ContentScale.Crop
            )
            // Keep text legible over dark, light and high-contrast photographs
            // without putting a full-width generic footer below the reel.
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .54f),
                        .42f to Color.Black.copy(alpha = .08f),
                        .73f to Color.Black.copy(alpha = .16f),
                        1f to Color.Black.copy(alpha = .42f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(contentInsets)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp,
                    bottom = if (hasReel) 330.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    if (hasReel) Color.Black.copy(alpha = .37f)
                    else Color(0xFFE8DFD0),
                    RoundedCornerShape(22.dp)
                )
            ) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Back to Home",
                        color = if (hasReel) Color.White else Color(0xFF193B3F))
                }
            }
            Text(
                "THE PHOTO REEL",
                color = if (hasReel) Color(0xFFF6DFC1) else Color(0xFF986950),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Turn a new page.",
                color = if (hasReel) Color.White else Color(0xFF193B3F),
                fontFamily = GlimpseType.editorial,
                fontSize = if (largeType) 27.sp else 36.sp
            )
            Text(
                collectionName,
                style = MaterialTheme.typography.titleSmall,
                color = if (hasReel) Color(0xFFFFF1DB) else Color(0xFF665B51)
            )
            if (!hasReel) {
                Text("This collection has no photographs yet. Return to Home to add some.")
            } else {
                Text(
                    "Previewing ${currentIndex + 1} of ${state.photos.size} · Wallpaper unchanged",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
                // These important controls remain available near the top,
                // outside the overlapping bottom-corner paper silhouette.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { model.stepPhoto(-1) },
                        enabled = currentIndex > 0,
                        colors = paleControls
                    ) { Text("Previous photo") }
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { model.stepPhoto(1) },
                        enabled = currentIndex >= 0 && currentIndex < state.photos.lastIndex,
                        colors = paleControls
                    ) { Text("Next photo") }
                    Button(onClick = onGrid, modifier = Modifier.heightIn(min = 48.dp), colors = paleControls) {
                        Text("View all · Grid")
                    }
                }
                // The primary image receives the lion's share of the screen.
                // Secondary actions deliberately scroll up behind the reel.
                Spacer(modifier = Modifier.height(if (largeType) 120.dp else 250.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8EFE0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("IN FOCUS",
                            color = Color(0xFF986950),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(onClick = onDetails, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Open photo details")
                            }
                            Button(
                                modifier = Modifier.heightIn(min = 48.dp),
                                onClick = model::applyPreview,
                                enabled = !state.busy && selected != null
                            ) { Text("Set Home wallpaper now") }
                        }
                        Text(
                            "Only Set Home wallpaper requests an Android change. " +
                                "The actual system wallpaper is not independently verified.",
                            color = Color(0xFF665B51),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(state.message,
                            color = Color(0xFF665B51),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
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
