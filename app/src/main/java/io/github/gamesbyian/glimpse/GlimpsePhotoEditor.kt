package io.github.gamesbyian.glimpse

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.domain.NormalizedCrop
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingEditedSave(val original: PhotoRecord,
                                     val geometry: PhotoEditGeometry)

/**
 * Temporary non-destructive crop/rotate session. All controls have ordinary
 * labelled equivalents; no gesture or unrecoverable editing operation.
 */
@Composable
internal fun GlimpsePhotoEditor(photo: PhotoRecord) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by rememberSaveable(photo.id) { mutableStateOf(false) }
    var turns by rememberSaveable(photo.id) { mutableIntStateOf(0) }
    var left by rememberSaveable(photo.id) { mutableFloatStateOf(0f) }
    var top by rememberSaveable(photo.id) { mutableFloatStateOf(0f) }
    var right by rememberSaveable(photo.id) { mutableFloatStateOf(1f) }
    var bottom by rememberSaveable(photo.id) { mutableFloatStateOf(1f) }
    var saving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingEditedSave?>(null) }

    val geometry = PhotoEditGeometry(turns, NormalizedCrop(
        left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble()
    ))
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { destination ->
        val snapshot = pending
        pending = null
        if (destination != null) {
            if (snapshot == null) {
                notice = "The edit selection expired. Choose Save edited PNG again."
            } else {
                saving = true
                notice = null
                scope.launch {
                    try {
                        notice = EditedPhotoOutput.writePng(context,
                            snapshot.original, snapshot.geometry, destination)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        notice = "Edited export failed. Android may have created an " +
                            "incomplete destination file. The original was not changed."
                    } finally {
                        saving = false
                    }
                }
            }
        }
    }

    OutlinedButton(onClick = { visible = !visible }) {
        Text(if (visible) "Close photo editor" else "Edit photo (crop and rotate)")
    }
    if (!visible) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("EDITED PHOTO", style = MaterialTheme.typography.labelLarge)
            Text(
                "This is a reversible preview. The imported original is unchanged. " +
                    "Save edited PNG makes a separate image, up to 3072 pixels on " +
                    "the longest edge, without copying original EXIF metadata. " +
                    "Changes are not saved as a permanent recipe yet.",
                style = MaterialTheme.typography.bodySmall
            )
            val preview by produceState<Bitmap?>(null, photo.id, geometry) {
                value = null
                value = try {
                    withContext(Dispatchers.IO) {
                        EditedPhotoOutput.renderVerified(
                            context, photo, geometry, EditedPhotoOutput.MAX_PREVIEW_EDGE
                        )
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    null
                }
            }
            if (preview != null) {
                Image(
                    bitmap = preview!!.asImageBitmap(),
                    contentDescription = "Preview of the current rotated and cropped photo",
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Preparing edited preview…", style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { turns = (turns + 3) % 4 }) {
                    Text("Rotate 90° left")
                }
                OutlinedButton(onClick = { turns = (turns + 1) % 4 }) {
                    Text("Rotate 90° right")
                }
                OutlinedButton(onClick = {
                    turns = 0
                    left = 0f; top = 0f; right = 1f; bottom = 1f
                }) { Text("Reset crop and rotation") }
            }
            Text("Rotation: " + (90 * turns) + "° clockwise")
            Text(
                "Adjust the crop edges below. Values are fractions of the " +
                    "EXIF-upright image after your rotation. Each edge remains " +
                    "at least two percent from its opposite.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Left edge: " + (left * 100).toInt() + "%")
            Slider(
                value = left,
                onValueChange = { left = it.coerceAtMost(right - 0.02f) },
                valueRange = 0f..0.98f,
                modifier = Modifier.semantics { contentDescription = "Left crop edge" }
            )
            Text("Top edge: " + (top * 100).toInt() + "%")
            Slider(
                value = top,
                onValueChange = { top = it.coerceAtMost(bottom - 0.02f) },
                valueRange = 0f..0.98f,
                modifier = Modifier.semantics { contentDescription = "Top crop edge" }
            )
            Text("Right edge: " + (right * 100).toInt() + "%")
            Slider(
                value = right,
                onValueChange = { right = it.coerceAtLeast(left + 0.02f) },
                valueRange = 0.02f..1f,
                modifier = Modifier.semantics { contentDescription = "Right crop edge" }
            )
            Text("Bottom edge: " + (bottom * 100).toInt() + "%")
            Slider(
                value = bottom,
                onValueChange = { bottom = it.coerceAtLeast(top + 0.02f) },
                valueRange = 0.02f..1f,
                modifier = Modifier.semantics { contentDescription = "Bottom crop edge" }
            )
            OutlinedButton(
                enabled = !saving,
                onClick = {
                    try {
                        pending = PendingEditedSave(photo, geometry)
                        notice = null
                        picker.launch(EditedPhotoOutput.defaultName(photo))
                    } catch (_: Exception) {
                        pending = null
                        notice = "Android Save As could not open."
                    }
                }
            ) { Text(if (saving) "Saving edited PNG…" else "Save edited PNG…") }
            if (notice != null) {
                Text(notice!!, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
