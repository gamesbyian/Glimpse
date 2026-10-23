package io.github.gamesbyian.glimpse

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.gamesbyian.glimpse.data.PhotoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Early real interaction prototype, not a validated final Glimpse visual design.
 * All wallpaper changes are dispatched through WallpaperCommands, never browsing.
 */
class MainActivity : ComponentActivity() {
    private val model: GlimpseModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1D5960),
                    secondary = Color(0xFFE8876C),
                    background = Color(0xFFF6F0E6),
                    surface = Color(0xFFFFFCF6)
                )
            ) {
                GlimpseScreen(model)
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Foreground eligibility checking only. No periodic WorkManager
                // or alarm assertion is made and no missed intervals replay.
                while (isActive) {
                    model.tickVisible()
                    delay(15_000)
                }
            }
        }
    }
}

@Composable
private fun GlimpseScreen(model: GlimpseModel) {
    val state by model.ui.collectAsStateWithLifecycle()
    val activeCollection by model.activeCollectionId.collectAsStateWithLifecycle()
    var newCollection by remember { mutableStateOf("Rainbows") }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris -> model.importSelected(uris) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Color(0xFFF6F0E6))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Glimpse", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Your photographs, your wallpaper.", style = MaterialTheme.typography.bodyMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCollection,
                    onValueChange = { newCollection = it },
                    label = { Text("Collection name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { model.createCollection(newCollection) }, enabled = !state.busy) {
                    Text("Create")
                }
            }
            if (state.collections.isNotEmpty()) {
                Text("Collections", fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.collections, key = { it.id }) { group ->
                        FilterChip(
                            selected = activeCollection == group.id,
                            onClick = { model.browseCollection(group.id) },
                            label = { Text(group.title) }
                        )
                    }
                }
                Button(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !state.busy && activeCollection != null
                ) {
                    Text("Add photos")
                }
            }
            if (activeCollection != null) {
                Text(
                    "Photos: " + state.photos.size + " • Offline copies",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.grid,
                        onClick = { if (state.grid) model.toggleGrid() },
                        label = { Text("Reel") }
                    )
                    FilterChip(
                        selected = state.grid,
                        onClick = { if (!state.grid) model.toggleGrid() },
                        label = { Text("View all · Grid") }
                    )
                }
                if (state.photos.isEmpty()) {
                    Text("No images yet. Add photos to this collection.")
                } else {
                    if (state.grid) {
                        PhotoGrid(
                            records = state.photos,
                            selectedId = state.current?.id,
                            onSelect = model::browsePhoto
                        )
                    }
                    val selected = state.current
                    val selectedIndex = state.photos.indexOfFirst { it.id == selected?.id }
                    Text(
                        "Previewing " + (selectedIndex + 1) + " of " + state.photos.size +
                            " · This does not change your wallpaper",
                        style = MaterialTheme.typography.labelMedium
                    )
                    PhotoPreview(
                        photo = selected,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        label = "Selected photo preview"
                    )
                    if (!state.grid) {
                        CircularPhotoReel(
                            records = state.photos,
                            selectedId = selected?.id,
                            onSelect = model::browsePhoto
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { model.stepPhoto(-1) },
                            enabled = selectedIndex > 0
                        ) { Text("Previous photo") }
                        OutlinedButton(
                            onClick = { model.stepPhoto(1) },
                            enabled = selectedIndex < state.photos.lastIndex
                        ) { Text("Next photo") }
                    }
                    Button(onClick = model::applyPreview, enabled = !state.busy) {
                        Text("Set Home wallpaper now")
                    }
                    Text(
                        "Only this explicit action attempts to change the Home wallpaper.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            RotationPanel(state, model)
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Prototype: the paper reel geometry and Android device behavior are still under evaluation.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    records: List<PhotoRecord>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 95.dp),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(300.dp)
    ) {
        gridItems(records, key = { it.id }) { photo ->
            Box(
                modifier = Modifier.height(112.dp)
                    .border(
                        if (photo.id == selectedId) 3.dp else 1.dp,
                        if (photo.id == selectedId) Color(0xFF1D5960) else Color(0xFFD9D0C0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(photo.id) }
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                PhotoPreview(photo, Modifier.fillMaxSize(), "Choose photo " + photo.id.take(8))
            }
        }
    }
}

@Composable
private fun PhotoPreview(photo: PhotoRecord?, modifier: Modifier, label: String) {
    val path = photo?.localPath
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sample = 1
                while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                BitmapFactory.decodeFile(
                    path,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = modifier.background(Color(0xFFE1D9CC), RoundedCornerShape(12.dp))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (image == null) {
            Text(if (photo == null) "No photo" else "Image unavailable")
        } else {
            Image(
                bitmap = image!!,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RotationPanel(state: GlimpseUiState, model: GlimpseModel) {
    val rotation = state.rotation
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Home wallpaper rotation", style = MaterialTheme.typography.titleMedium)
            Text("Every five minutes while Glimpse is in use. No exact background alarm.")
            Text("Status: " + (rotation?.outcome ?: "Not started"))
            Text("Enabled: " + (if (rotation?.active == true) "Yes" else "No"))
            Text("Last OS acknowledgment: " + displayTime(rotation?.lastAcknowledgedAt))
            Text("Next eligible: " + displayTime(rotation?.nextEligibleAt))
            if (rotation?.pendingPhotoId != null) {
                Text("Outcome uncertain. Inspect the phone wallpaper before proceeding.")
                OutlinedButton(onClick = model::resolveUncertain, enabled = !state.busy) {
                    Text("Acknowledge uncertainty and pause")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = model::startRotation,
                        enabled = !state.busy && state.photos.isNotEmpty()
                    ) { Text("Start shuffle") }
                    OutlinedButton(
                        onClick = model::pauseRotation,
                        enabled = !state.busy && rotation?.active == true
                    ) { Text("Pause") }
                }
                TextButton(
                    onClick = model::changeWallpaperNow,
                    enabled = !state.busy && rotation?.collectionId != null
                ) {
                    Text("Change now from shuffle")
                }
            }
        }
    }
}

private fun displayTime(epoch: Long?): String =
    if (epoch == null) "Not yet" else DateFormat.getDateTimeInstance().format(Date(epoch))
