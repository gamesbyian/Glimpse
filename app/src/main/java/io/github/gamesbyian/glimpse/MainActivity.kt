package io.github.gamesbyian.glimpse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.domain.describeRotationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Early real interaction prototype, not a validated final Glimpse visual design.
 * All wallpaper changes are dispatched through WallpaperCommands, never browsing.
 */
class MainActivity : ComponentActivity() {
    private val model: GlimpseModel by viewModels()
    // The incoming grant is intentionally Activity RAM, never saved into a Bundle.
    // During configuration changes, Android re-delivers the launch Intent until
    // the user accepts/dismisses it. Process death may require sharing again.
    private var incomingShare by mutableStateOf<IncomingPhotoShare?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShare = IncomingPhotoShareReceiver.parse(intent)
        setContent {
            MaterialTheme(
                typography = GlimpseType.theme,
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1D4A4D),
                    onPrimary = Color(0xFFFFFCF3),
                    primaryContainer = Color(0xFFE2EEE9),
                    onPrimaryContainer = Color(0xFF243F3C),
                    secondary = Color(0xFF8B5A3C),
                    onSecondary = Color(0xFFFFF9F0),
                    secondaryContainer = Color(0xFFF3E0D0),
                    onSecondaryContainer = Color(0xFF603D29),
                    tertiary = Color(0xFF536454),
                    onTertiary = Color(0xFFFFFCF3),
                    tertiaryContainer = Color(0xFFDFE8DF),
                    onTertiaryContainer = Color(0xFF334538),
                    background = Color(0xFFF6F0E6),
                    onBackground = Color(0xFF263A37),
                    surface = Color(0xFFFFFCF3),
                    onSurface = Color(0xFF273A36),
                    surfaceVariant = Color(0xFFECE2D5),
                    onSurfaceVariant = Color(0xFF5D5247),
                    surfaceContainerLowest = Color(0xFFFFFCF6),
                    surfaceContainerLow = Color(0xFFF8F1E6),
                    surfaceContainer = Color(0xFFF1E8DC),
                    surfaceContainerHigh = Color(0xFFEDE1D2),
                    surfaceContainerHighest = Color(0xFFE6D7C6),
                    outline = Color(0xFF9B8E7E),
                    outlineVariant = Color(0xFFD6C8B8)
                )
            ) {
                GlimpseScreen(
                    model = model,
                    incomingShare = incomingShare,
                    onAcceptShared = { uris, destination ->
                        // Import never changes Home wallpaper or starts rotation.
                        // Clear the incoming launch intent to avoid replay on rotate.
                        incomingShare = null
                        setIntent(Intent(Intent.ACTION_MAIN))
                        model.importSelected(uris, destination)
                    },
                    onDismissShared = {
                        incomingShare = null
                        setIntent(Intent(Intent.ACTION_MAIN))
                    }
                )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Replaces an unaccepted previous share; nothing imported implicitly.
        incomingShare = IncomingPhotoShareReceiver.parse(intent)
    }
}

@Composable
private fun GlimpseScreen(
    model: GlimpseModel,
    incomingShare: IncomingPhotoShare?,
    onAcceptShared: (List<Uri>, String) -> Unit,
    onDismissShared: () -> Unit
) {
    val state by model.ui.collectAsStateWithLifecycle()
    val activeCollection = state.collectionId
    var newCollection by remember { mutableStateOf("Rainbows") }
    var showNewCollection by remember { mutableStateOf(false) }
    var showingDetail by rememberSaveable { mutableStateOf(false) }
    var showingBrowse by rememberSaveable { mutableStateOf(false) }
    var showingSupport by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showingDetail) { showingDetail = false }
    BackHandler(enabled = showingSupport && !showingDetail) { showingSupport = false }
    BackHandler(enabled = showingBrowse && !showingDetail && !showingSupport) {
        showingBrowse = false
    }
    // An external Photo Picker may survive Activity recreation. Retain the
    // launch-time collection identity, never an ephemeral provider URI.
    var pickerTargetCollection by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        val target = pickerTargetCollection
        pickerTargetCollection = null
        model.importSelected(uris, target)
    }

    Scaffold { padding ->
        if (showingDetail) {
            GlimpsePhotoDetail(
                state = state,
                model = model,
                onBack = { showingDetail = false },
                modifier = Modifier.padding(padding)
            )
        } else if (showingSupport) {
            GlimpseSupportScreen(
                onBack = { showingSupport = false },
                modifier = Modifier.padding(padding)
            )
        } else if (showingBrowse) {
            GlimpseReelRoom(
                state = state,
                model = model,
                onBack = { showingBrowse = false },
                onDetails = { showingDetail = true },
                onGrid = {
                    if (!state.grid) model.toggleGrid()
                    showingBrowse = false
                },
                modifier = Modifier.padding(padding)
            )
        } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Color(0xFFF6F0E6))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Glimpse", color = Color(0xFF193B3F),
                fontFamily = GlimpseType.editorial, fontSize = 44.sp,
                fontWeight = FontWeight.Normal, letterSpacing = (-1.4).sp)
            Text("YOUR PHOTOS, NEW PERSPECTIVES",
                color = Color(0xFF986950), fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall, letterSpacing = 1.3.sp)
            if (incomingShare != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("SHARED WITH GLIMPSE",
                            style = MaterialTheme.typography.labelLarge)
                        when (incomingShare) {
                            is IncomingPhotoShare.Ready -> {
                                val count = incomingShare.uris.size
                                val targetName = state.collections.firstOrNull {
                                    it.id == activeCollection
                                }?.title
                                Text(
                                    "$count image" + (if (count == 1) " is" else "s are") +
                                        " ready for import. Originals will be saved as " +
                                        "verified private copies. No wallpaper will change."
                                )
                                Text(
                                    if (targetName != null)
                                        "Destination: " + targetName
                                    else "Create or select a collection before importing.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val destination = activeCollection
                                            if (destination != null) {
                                                onAcceptShared(incomingShare.uris, destination)
                                            }
                                        },
                                        enabled = !state.busy && targetName != null
                                    ) { Text("Import shared images") }
                                    TextButton(onClick = onDismissShared) { Text("Dismiss") }
                                }
                                Text(
                                    "Crop and rotate are not yet available in this build. " +
                                        "The shared original remains unchanged.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is IncomingPhotoShare.Rejected -> {
                                Text(incomingShare.message)
                                TextButton(onClick = onDismissShared) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
            // Direct route to the physical control: no need to scroll through
            // the dashboard/collection management merely to turn the reel.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { showingBrowse = true },
                    enabled = activeCollection != null && state.photos.isNotEmpty()
                ) { Text("Browse photo reel") }
                TextButton(onClick = { showingSupport = true }) {
                    Text("Help & privacy")
                }
            }
            GlimpseWallpaperSummary(state)
            if (state.collections.isEmpty() || showNewCollection) {
                Text(
                    if (state.collections.isEmpty()) "Start with a collection of photographs."
                    else "Create a collection",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCollection,
                        onValueChange = { newCollection = it },
                        label = { Text("Collection name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            model.createCollection(newCollection)
                            showNewCollection = false
                        },
                        enabled = !state.busy && newCollection.isNotBlank()
                    ) { Text("Create") }
                }
            }
            if (state.collections.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Collections", color = Color(0xFF263A37),
                        fontFamily = GlimpseType.editorial, fontSize = 24.sp)
                    TextButton(onClick = { showNewCollection = !showNewCollection }) {
                        Text(if (showNewCollection) "Cancel" else "New collection")
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.collectionOverviews, key = { it.id }) { group ->
                        GlimpseCollectionTile(
                            overview = group,
                            selected = activeCollection == group.id,
                            onSelect = { model.browseCollection(group.id) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        pickerTargetCollection = activeCollection
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !state.busy && activeCollection != null
                ) {
                    Text("Add photos")
                }
                if (state.retryableFailures > 0) {
                    OutlinedButton(
                        onClick = model::retryFailedImports,
                        enabled = !state.busy
                    ) {
                        Text("Retry " + state.retryableFailures + " failed photo" +
                            if (state.retryableFailures == 1) "" else "s")
                    }
                    Text(
                        "Retry uses temporary Photo Picker access. If it has expired, " +
                            "choose Add photos to reselect missing images.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (activeCollection != null) {
                Text(
                    "Photos: " + state.photos.size + " • Offline copies",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                            onSelect = { photoId -> model.browsePhotoInCollection(activeCollection, photoId) }
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
                    OutlinedButton(onClick = { showingDetail = true }) {
                        Text("Open photo details")
                    }
                    if (!state.grid) {
                        CircularPhotoReel(
                            records = state.photos,
                            selectedId = selected?.id,
                            onSelect = { photoId -> model.browsePhotoInCollection(activeCollection, photoId) }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                "Paper-reel finish and motion are still being evaluated on physical Android devices.",
                style = MaterialTheme.typography.bodySmall
            )
        }
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
internal fun PhotoPreview(
    photo: PhotoRecord?,
    modifier: Modifier,
    label: String,
    contentScale: ContentScale = ContentScale.Crop,
    pathOverride: String? = null
) {
    val path = pathOverride ?: photo?.localPath
    // key(path) restarts the load state rather than retaining the previous
    // bitmap while the next selection decodes. The explicit source identity
    // check is a second guard against a delayed previous result.
    val loaded by key(path) {
        produceState(
            initialValue = PreviewLoadState<ImageBitmap>(
                sourcePath = path, complete = path == null
            ),
            key1 = path
        ) {
            value = if (path == null) {
                PreviewLoadState(sourcePath = null, complete = true)
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        // Same EXIF orientation and bounded decode as Reel/Home
                        // wallpaper. Original file bytes are untouched.
                        val image = BoundedPhotoDecode.decode(
                            File(path), maxEdge = 1024
                        ).asImageBitmap()
                        PreviewLoadState(sourcePath = path, image = image, complete = true)
                    } catch (_: Exception) {
                        PreviewLoadState(sourcePath = path, complete = true)
                    }
                }
            }
        }
    }
    val image = loaded.imageFor(path)
    Box(
        modifier = modifier.background(Color(0xFFE1D9CC), RoundedCornerShape(12.dp))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        when {
            path == null -> Text("No photo")
            loaded.isLoading(path) -> Text("Loading photo…")
            image == null -> Text("Image unavailable")
            else -> Image(
                bitmap = image,
                contentDescription = label,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RotationPanel(state: GlimpseUiState, model: GlimpseModel) {
    val rotation = state.rotation
    val source = describeRotationSource(
        rotation?.collectionId, state.collectionId, state.collections
    )
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Home rotation", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Less detail" else "Details")
                }
            }
            Text(
                if (rotation?.active == true) "Shuffling · while Glimpse is in use"
                else "Rotation is paused or not started",
                style = MaterialTheme.typography.bodyMedium
            )
            if (source != null) {
                Text(
                    "Shuffle collection: " + source.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!source.available) {
                    Text(
                        "This saved shuffle source no longer exists. Choose a collection and " +
                            "start a new shuffle.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (source.differsFromBrowsing) {
                    Text(
                        if (rotation?.active == true)
                            "Browsing a different collection does not switch the shuffle. " +
                                "Change Home now still uses " + source.title +
                                ". Pause first, then start shuffle in the collection you want."
                        else
                            "Change Home now still uses " + source.title +
                                ". Start shuffle while viewing another collection to switch sources.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (rotation?.pendingPhotoId != null) {
                Text("Previous wallpaper attempt is uncertain; automatic changes are paused.")
                OutlinedButton(onClick = model::resolveUncertain, enabled = !state.busy) {
                    Text("Review uncertainty and pause")
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (rotation?.active == true) {
                        OutlinedButton(onClick = model::pauseRotation, enabled = !state.busy) {
                            Text("Pause")
                        }
                    } else {
                        Button(
                            onClick = model::startRotation,
                            enabled = !state.busy && state.photos.isNotEmpty()
                        ) { Text("Start shuffle") }
                    }
                    TextButton(
                        onClick = model::changeWallpaperNow,
                        enabled = !state.busy && source?.available == true
                    ) {
                        Text("Change Home now")
                    }
                }
            }
            if (expanded) {
                Text(
                    "Five-minute eligibility is checked while this app is visible. " +
                        "Android may defer changes when Glimpse is not in use.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Outcome: " + (rotation?.outcome ?: "Not started"))
                Text("Last Android acknowledgment: " + displayTime(rotation?.lastAcknowledgedAt))
                Text("Next eligible: " + displayTime(rotation?.nextEligibleAt))
            }
        }
    }
}

private fun displayTime(epoch: Long?): String =
    if (epoch == null) "Not yet" else DateFormat.getDateTimeInstance().format(Date(epoch))
