package io.github.gamesbyian.glimpse

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.gamesbyian.glimpse.data.PhotoRecord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/**
 * Source-faithful detail, built from the same selected photo ID and the same
 * explicit wallpaper command as Reel/Grid. No second source of wallpaper state.
 */
@Composable
internal fun GlimpsePhotoDetail(
    state: GlimpseUiState,
    model: GlimpseModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photo = state.current
    val index = state.photos.indexOfFirst { it.id == photo?.id }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember(photo?.id) { mutableStateOf(false) }
    var shareError by remember(photo?.id) { mutableStateOf<String?>(null) }
    var choosingCopyDestination by remember(photo?.id) { mutableStateOf(false) }
    var confirmingMembershipRemoval by remember(photo?.id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveNotice by remember { mutableStateOf<String?>(null) }
    // Hold the exact selected image across the external picker. Not persisted.
    var pendingSave by remember { mutableStateOf<PhotoRecord?>(null) }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/*")
    ) { destination ->
        val original = pendingSave
        pendingSave = null
        if (destination != null) {
            if (original == null) {
                saveNotice = "The source selection expired. Choose the photo and Save again."
            } else {
                saving = true
                saveNotice = null
                scope.launch {
                    try {
                        saveNotice = OriginalPhotoSave.copy(context, original, destination)
                    } catch (error: Exception) {
                        saveNotice = "Save failed: " +
                            (error.message ?: "Android could not write the selected document.")
                    } finally {
                        saving = false
                    }
                }
            }
        }
    }
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to Reel / Grid") }
        Text(
            "A CLOSER LOOK",
            color = Color(0xFF966C4F),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "In focus.",
            fontFamily = GlimpseType.editorial,
            color = Color(0xFF193B3F),
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp
        )
        Text(
            if (index >= 0) "Photograph ${index + 1} of ${state.photos.size}"
            else "No photograph selected",
            color = Color(0xFF5F554B),
            style = MaterialTheme.typography.titleSmall
        )

        // Fit preserves the entire image and its aspect ratio. This is distinct
        // from the deliberate cover crops in the stationery/reel composition.
        PhotoPreview(
            photo = photo,
            modifier = Modifier.fillMaxWidth().height(390.dp),
            label = "Full original photograph, preview only",
            contentScale = ContentScale.Fit
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { model.stepPhoto(-1) },
                enabled = index > 0
            ) { Text("Previous photo") }
            OutlinedButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { model.stepPhoto(1) },
                enabled = index >= 0 && index < state.photos.lastIndex
            ) { Text("Next photo") }
        }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            onClick = {
                val source = photo ?: return@OutlinedButton
                if (!sharing) {
                    sharing = true
                    shareError = null
                    scope.launch {
                        try {
                            // File integrity verification runs off the main thread.
                            val chooser = OriginalPhotoSharing.chooser(context, source)
                            context.startActivity(chooser)
                        } catch (error: Exception) {
                            shareError = error.message ?: "Android could not open sharing."
                        } finally {
                            sharing = false
                        }
                    }
                }
            },
            enabled = photo != null && !sharing
        ) { Text(if (sharing) "Verifying original…" else "Share original photo") }
        Text(
            "Sharing sends the unedited original. Camera or location metadata may " +
                "be included; Android lets you choose the receiving app. " +
                "Glimpse does not upload photos itself.",
            style = MaterialTheme.typography.bodySmall
        )
        if (shareError != null) {
            Text("Sharing unavailable: " + shareError, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            onClick = {
                val source = photo ?: return@OutlinedButton
                try {
                    val filename = OriginalPhotoSave.defaultName(source)
                    pendingSave = source
                    saveNotice = null
                    documentPicker.launch(filename)
                } catch (error: Exception) {
                    pendingSave = null
                    saveNotice = "Could not open Android Save As: " +
                        (error.message ?: "Unsupported destination.")
                }
            },
            enabled = photo != null && !sharing && !saving
        ) {
            Text(if (saving) "Saving original copy…" else "Save original copy…")
        }
        Text(
            "Choose a file destination through Android. The exact imported " +
                "original, including camera/location metadata, is copied without " +
                "editing or deleting Glimpse's private file. This is not a full library backup.",
            style = MaterialTheme.typography.bodySmall
        )
        if (saveNotice != null) {
            Text(saveNotice!!, style = MaterialTheme.typography.bodySmall)
        }
        if (photo != null) {
            GlimpsePhotoEditor(photo, model, state.collectionId, state.busy)
        }
        Button(
            modifier = Modifier.heightIn(min = 48.dp),
            onClick = model::applyPreview,
            enabled = photo != null && !state.busy
        ) { Text("Set Home wallpaper now") }
        Text(
            "Browsing and inspecting photos never changes the device wallpaper. " +
                "Only the button above requests a Home wallpaper change.",
            style = MaterialTheme.typography.bodySmall
        )

        if (photo != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("THE ORIGINAL", fontFamily = GlimpseType.editorial,
                        style = MaterialTheme.typography.titleLarge)
                    Text("${photo.width} × ${photo.height} pixels")
                    Text("${NumberFormat.getIntegerInstance().format(photo.byteLength)} bytes · ${photo.mime}")
                    Text("Imported ${DateFormat.getDateTimeInstance().format(Date(photo.importedAt))}")
                    Text(
                        "Verified private offline copy. Sharing the untouched original is available; " +
                            "The basic crop/quarter-turn editor saves EXIF-free, bounded PNG copies; durable edit recipes and edited wallpaper application are not yet implemented.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.rotation?.lastAcknowledgedPhotoId == photo.id) {
                        Text(
                            "Glimpse last received Android acknowledgment for this photo. " +
                                "Current system wallpaper has not been read back.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        val otherCollections = state.collections.filter { it.id != state.collectionId }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            enabled = photo != null && state.collectionId != null &&
                otherCollections.isNotEmpty() && !state.busy,
            onClick = { choosingCopyDestination = !choosingCopyDestination }
        ) { Text(if (choosingCopyDestination) "Close collection chooser"
                 else "Add this photo to another collection") }
        if (choosingCopyDestination && photo != null && state.collectionId != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Reuses this verified original; it will appear in both " +
                            "collections. No wallpaper or shuffle change.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    for (target in otherCollections) {
                        OutlinedButton(
                            modifier = Modifier.heightIn(min = 48.dp),
                            enabled = !state.busy,
                            onClick = {
                                val sourceId = state.collectionId
                                val selectedId = photo.id
                                choosingCopyDestination = false
                                model.addPhotoToCollection(selectedId, sourceId, target.id)
                            }
                        ) { Text("Add to " + target.title) }
                    }
                }
            }
        } else if (otherCollections.isEmpty()) {
            Text(
                "Create another collection on Home to organize this photo in more than one place.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            enabled = photo != null && state.collectionId != null && !state.busy,
            onClick = { confirmingMembershipRemoval = true }
        ) { Text("Remove from this collection") }
        if (confirmingMembershipRemoval && photo != null && state.collectionId != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Only a collection membership is removed. This succeeds " +
                            "only if the photo is also in another collection, so " +
                            "its intact original remains accessible. Current " +
                            "wallpaper and shuffle settings do not change.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { confirmingMembershipRemoval = false }
                    ) { Text("Keep photo in this collection") }
                    OutlinedButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !state.busy,
                        onClick = {
                            val source = state.collectionId
                            val selected = photo.id
                            confirmingMembershipRemoval = false
                            model.removePhotoFromCollection(selected, source)
                        }
                    ) { Text("Keep original and remove membership") }
                }
            }
        }
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
    }
}
