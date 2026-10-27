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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back to Reel / Grid") }
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
                onClick = { model.stepPhoto(-1) },
                enabled = index > 0
            ) { Text("Previous photo") }
            OutlinedButton(
                onClick = { model.stepPhoto(1) },
                enabled = index >= 0 && index < state.photos.lastIndex
            ) { Text("Next photo") }
        }
        Button(
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
                        "Verified private offline copy. Cropping, sharing and editing " +
                            "will appear only when those native features are implemented.",
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
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
    }
}
