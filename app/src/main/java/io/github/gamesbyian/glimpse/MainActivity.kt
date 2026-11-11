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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.gamesbyian.glimpse.data.CollectionZipWriter
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
                        // Release transient share UI only after the model has
                        // synchronously accepted this operation. A busy action
                        // must not consume the only accessible URI grant.
                        if (model.importSelected(uris, destination)) {
                            incomingShare = null
                            setIntent(Intent(Intent.ACTION_MAIN))
                        }
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
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    var sharingCollection by remember { mutableStateOf(false) }
    var shareCollectionError by remember { mutableStateOf<String?>(null) }
    var clipboardFeedback by remember { mutableStateOf<String?>(null) }
    var newCollection by remember { mutableStateOf("Rainbows") }
    var showNewCollection by remember { mutableStateOf(false) }
    var renameTargetCollection by remember { mutableStateOf<String?>(null) }
    var removeTargetCollection by remember { mutableStateOf<String?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var showingDetail by rememberSaveable { mutableStateOf(false) }
    var detailFromGrid by rememberSaveable { mutableStateOf(false) }
    var showingBrowse by rememberSaveable { mutableStateOf(false) }
    var showingSupport by rememberSaveable { mutableStateOf(false) }
    val closeDetails: () -> Unit = {
        showingDetail = false
        // Restore the user's originating contact sheet, not Home, after
        // temporarily materializing the finite collection for Photo Details.
        if (detailFromGrid) model.showGrid()
        detailFromGrid = false
    }
    BackHandler(enabled = showingDetail) { closeDetails() }
    BackHandler(enabled = showingSupport && !showingDetail) { showingSupport = false }
    BackHandler(enabled = showingBrowse && !showingDetail && !showingSupport) {
        showingBrowse = false
    }
    BackHandler(enabled = state.grid && incomingShare == null &&
        !showingBrowse && !showingDetail && !showingSupport) {
        model.toggleGrid()
    }
    // An external Photo Picker may survive Activity recreation. Retain the
    // launch-time collection identity, never an ephemeral provider URI.
    var pickerTargetCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingArchiveCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var archivePickerError by remember { mutableStateOf<String?>(null) }
    val archivePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        val sourceCollection = pendingArchiveCollection
        pendingArchiveCollection = null
        if (destination != null) {
            model.exportCollectionZip(destination, sourceCollection)
        }
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        val target = pickerTargetCollection
        pickerTargetCollection = null
        if (!model.importSelected(uris, target)) {
            model.retainBusyPickerReturn(uris, target)
        }
    }

    Scaffold { padding ->
        if (showingDetail) {
            GlimpsePhotoDetail(
                state = state,
                model = model,
                onBack = closeDetails,
                modifier = Modifier.padding(padding)
            )
        } else if (showingSupport) {
            GlimpseSupportScreen(
                model = model,
                onBack = { showingSupport = false },
                modifier = Modifier.padding(padding)
            )
        } else if (showingBrowse) {
            GlimpseReelRoom(
                state = state,
                model = model,
                onBack = { showingBrowse = false },
                onDetails = { detailFromGrid = false; showingDetail = true },
                onGrid = {
                    if (!state.grid) model.toggleGrid()
                    showingBrowse = false
                },
                modifier = Modifier,
                contentInsets = padding
            )
        } else if (state.grid && incomingShare == null) {
            GlimpseGridRoom(
                state = state,
                model = model,
                onBack = { model.toggleGrid() },
                onReel = {
                    model.toggleGrid()
                    showingBrowse = true
                },
                onDetails = {
                    // Photo Details and Reel need the full finite collection
                    // until their own cursor virtualization is implemented.
                    // Preserve the paged Grid's selected stable ID first.
                    model.hideGrid()
                    detailFromGrid = true
                    showingDetail = true
                },
                modifier = Modifier.padding(padding)
            )
        } else {
        // The reel occupies fixed screen coordinates, independent of the page
        // scroll. Content scrolls behind the anchored paper disc.
        Box(modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF6F0E6))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = 0.dp,
                    bottom = if (activeCollection != null && !state.grid &&
                        state.photos.isNotEmpty()) 330.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // An Android Sharesheet handoff needs immediate consent: do not
            // bury its import/dismiss choices beneath the photographic hero.
            if (incomingShare == null) {
                GlimpseHomeHero(
                    state = state,
                    onBrowse = { showingBrowse = true },
                    onSupport = { showingSupport = true },
                    modifier = if (state.current == null)
                        Modifier.padding(horizontal = 16.dp) else Modifier
                )
            }
            // Management content is inset as a distinct scroll-flow region.
            // Only the photographic opening is edge-to-edge; the reel remains
            // a viewport-anchored sibling of this entire scrolling Column.
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                    "The shared original stays unchanged. After importing, " +
                                        "open Photo Detail for non-destructive crop and rotation.",
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
            GlimpseWallpaperSummary(state)
            // Until a collection exists there is no Add photos section to own
            // action feedback. Keep the initial onboarding/errors visible.
            if (state.collections.isEmpty()) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
            }
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
                    TextButton(
                        onClick = {
                            val selected = state.collections.firstOrNull {
                                it.id == activeCollection
                            }
                            if (selected != null) {
                                renameTargetCollection = selected.id
                                renameTitle = selected.title
                            }
                        },
                        enabled = activeCollection != null && !state.busy
                    ) { Text("Rename collection") }
                    TextButton(
                        enabled = activeCollection != null && !state.busy,
                        onClick = { removeTargetCollection = activeCollection }
                    ) { Text("Remove collection") }
                    if (state.photos.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val chosen = state.collections.firstOrNull {
                                it.id == activeCollection
                            } ?: return@TextButton
                            try {
                                pendingArchiveCollection = chosen.id
                                archivePickerError = null
                                archivePicker.launch(CollectionZipWriter.defaultName(chosen.title))
                            } catch (_: Exception) {
                                pendingArchiveCollection = null
                                archivePickerError = "Android Save As could not open. Try again."
                            }
                        },
                        enabled = activeCollection != null && !state.busy
                    ) { Text("Export collection ZIP") }
                    }
                    if (state.photos.size in 1..OriginalPhotoSharing.MAX_BATCH) {
                        TextButton(
                            enabled = !state.busy && !sharingCollection,
                            onClick = {
                                val selected = state.photos.toList()
                                sharingCollection = true
                                shareCollectionError = null
                                shareScope.launch {
                                    try {
                                        val chooser = OriginalPhotoSharing.batchChooser(context, selected)
                                        context.startActivity(chooser)
                                    } catch (_: Exception) {
                                        shareCollectionError =
                                            "Unable to share this collection. Check that each " +
                                            "original is intact, or share individual photos."
                                    } finally {
                                        sharingCollection = false
                                    }
                                }
                            }
                        ) { Text(if (sharingCollection) "Verifying originals…" else
                            "Share " + state.photos.size + " originals") }
                    }
                }
                if (shareCollectionError != null) {
                    Text(shareCollectionError!!, color = MaterialTheme.colorScheme.error)
                }
                if (state.photos.size > OriginalPhotoSharing.MAX_BATCH) {
                    Text(
                        "Batch sharing currently supports up to 25 originals. " +
                            "You can share individual photos from their detail screen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.photos.size in 1..OriginalPhotoSharing.MAX_BATCH) {
                    Text(
                        "Batch sharing sends unchanged originals, including any " +
                            "location or camera metadata, through Android's chooser. " +
                            "Receiving apps may have their own size limits.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (archivePickerError != null) {
                    Text(archivePickerError!!, color = MaterialTheme.colorScheme.error)
                }
                if (state.photos.isNotEmpty()) {
                    Text(
                        "Export uses Android's Save As destination and contains exact " +
                            "unedited originals with camera/location metadata. It may be " +
                            "large. Restore collection ZIP in Help & privacy imports " +
                            "photos into a new collection, not wallpaper settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val removeTarget = state.collections.firstOrNull {
                    it.id == removeTargetCollection
                }
                if (removeTarget != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("REMOVE COLLECTION · " + removeTarget.title,
                                style = MaterialTheme.typography.titleSmall)
                            Text(
                                "This removes the collection name and its memberships " +
                                    "only when every photo also belongs to another collection, " +
                                    "or when the collection is empty. No image bytes are " +
                                    "deleted. A saved shuffle source cannot be removed. " +
                                    "This collection arrangement cannot be undone.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { removeTargetCollection = null }) {
                                    Text("Keep collection")
                                }
                                OutlinedButton(enabled = !state.busy, onClick = {
                                    val chosenId = removeTarget.id
                                    removeTargetCollection = null
                                    model.removeCollectionOnly(chosenId)
                                }) { Text("Confirm remove collection only") }
                            }
                        }
                    }
                }
                val renameTarget = state.collections.firstOrNull {
                    it.id == renameTargetCollection
                }
                if (renameTarget != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Rename: " + renameTarget.title,
                                style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = renameTitle,
                                onValueChange = { renameTitle = it.take(80) },
                                label = { Text("New collection name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        model.renameCollection(renameTargetCollection, renameTitle)
                                        renameTargetCollection = null
                                    },
                                    enabled = !state.busy && renameTitle.isNotBlank()
                                ) { Text("Save name") }
                                TextButton(onClick = { renameTargetCollection = null }) {
                                    Text("Cancel rename")
                                }
                            }
                            Text(
                                "Only the title changes. Photos, shuffle collection and " +
                                    "the current wallpaper are unaffected.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardFeedback = null
                            pickerTargetCollection = activeCollection
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !state.busy && activeCollection != null,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Add photos") }
                    OutlinedButton(
                        onClick = {
                            when (val result = ClipboardImageIntake.readOnUserTap(context)) {
                                is ClipboardImageResult.Ready -> {
                                    clipboardFeedback = "Clipboard image import requested; " +
                                        "check the result below. No wallpaper was changed."
                                    model.importSelected(result.uris, activeCollection)
                                }
                                is ClipboardImageResult.Unavailable ->
                                    clipboardFeedback = result.explanation
                            }
                        },
                        enabled = !state.busy && activeCollection != null,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Paste image") }
                }
                if (clipboardFeedback != null) {
                    Text(clipboardFeedback!!, style = MaterialTheme.typography.bodySmall)
                }
                // Keep import/duplicate/error results directly beside Add photos.
                // The previously bottom-of-page status sat behind the fixed
                // paper Reel on 320dp and normal portrait screens, making
                // successful duplicate detection effectively invisible.
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.deferredPickerCount > 0) {
                    Text(
                        "Android returned ${state.deferredPickerCount} selected image(s) " +
                            "while another operation was running. Nothing was imported. " +
                            "Retry now or use Add photos to reselect if the grant expired.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = model::retryDeferredPicker,
                            enabled = !state.busy
                        ) { Text("Retry pending picker photos") }
                        TextButton(onClick = model::dismissDeferredPicker) {
                            Text("Dismiss pending selection")
                        }
                    }
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
                    OutlinedButton(onClick = { detailFromGrid = false; showingDetail = true }) {
                        Text("Open photo details")
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
            Text(
                "Paper-reel finish and motion are still being evaluated on physical Android devices.",
                style = MaterialTheme.typography.bodySmall
            )
            } // End inset management region.
        }
        // This overlay is a sibling of, rather than a child of, the scrollable
        // dashboard. No footer or dashboard card can shift the reel position.
        if (activeCollection != null && !state.grid && state.photos.isNotEmpty()) {
            CircularPhotoReel(
                records = state.photos,
                selectedId = state.current?.id,
                onSelect = { id -> model.browsePhotoInCollection(activeCollection, id) },
                modifier = Modifier.align(Alignment.BottomStart),
                showHeading = false
            )
        }
        // A second physical reel represents ACTUAL saved collections.
        // The first bottom-left reel still represents photographs. Neither
        // is a scrolling dashboard card or a decorative wheel; this corner
        // control only appears when there is a real choice to make and enough
        // screen/type space to preserve reachable primary actions.
        if (incomingShare == null && activeCollection != null &&
            state.collections.size >= 2 &&
            LocalConfiguration.current.screenWidthDp >= 390 &&
            LocalDensity.current.fontScale < 1.5f
        ) {
            GlimpseCollectionReel(
                collections = state.collections,
                selectedId = activeCollection,
                onSelect = model::browseCollection,
                modifier = Modifier.align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = 32.dp)
            )
        }
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
    pathOverride: String? = null,
    decodeMaxEdge: Int = 1024
) {
    val path = pathOverride ?: photo?.localPath
    // Large photographic surfaces benefit from additional detail; keep a
    // strict bound so decoded bitmap memory cannot explode on huge originals.
    val boundedEdge = decodeMaxEdge.coerceIn(256, 2048)
    // key(path) restarts the load state rather than retaining the previous
    // bitmap while the next selection decodes. The explicit source identity
    // check is a second guard against a delayed previous result.
    val loaded by key(path, boundedEdge) {
        produceState(
            initialValue = PreviewLoadState<ImageBitmap>(
                sourcePath = path, complete = path == null
            ),
            key1 = path,
            key2 = boundedEdge
        ) {
            value = if (path == null) {
                PreviewLoadState(sourcePath = null, complete = true)
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        // Same EXIF orientation and bounded decode as Reel/Home
                        // wallpaper. Original file bytes are untouched.
                        val image = BoundedPhotoDecode.decode(
                            File(path), maxEdge = boundedEdge
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
                if (rotation?.active == true && state.backgroundChecksEnabled)
                    "Shuffling · 5-minute visible checks; OS-delayed background checks enabled"
                else if (rotation?.active == true) "Shuffling · while Glimpse is in use"
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = state.backgroundChecksEnabled,
                        onCheckedChange = model::setBackgroundChecks,
                        enabled = !state.busy,
                        modifier = Modifier.semantics {
                            contentDescription = "Enable best-effort background wallpaper checks"
                        }
                    )
                    Text(
                        if (state.backgroundChecksEnabled)
                            "Background checks enabled (best effort)"
                        else "Allow background wallpaper checks",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    "Optional, off by default. Foreground rotation checks about every " +
                        "five minutes while Glimpse is visible. When you enable " +
                        "background checks, Android schedules work no more frequently " +
                        "than every 15 minutes and may defer it much longer in Doze " +
                        "or due to manufacturer restrictions. No exact background " +
                        "change time, wakeup or catch-up is promised.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Outcome: " + (rotation?.outcome ?: "Not started"))
                Text("Last Android acknowledgment: " + displayTime(rotation?.lastAcknowledgedAt))
                Text("Next eligible: " + displayTime(rotation?.nextEligibleAt))
                // Preview is opt-in within Details and never invokes the
                // wallpaper side-effect gateway or reserves a shuffle entry.
                GlimpseShuffleFilmstrip(state)
            }
        }
    }
}

private fun displayTime(epoch: Long?): String =
    if (epoch == null) "Not yet" else DateFormat.getDateTimeInstance().format(Date(epoch))
