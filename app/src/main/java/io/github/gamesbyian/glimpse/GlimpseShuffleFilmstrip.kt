package io.github.gamesbyian.glimpse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.domain.ShuffleProjection
import org.json.JSONArray

/**
 * Real preview-only four-frame filmstrip for the saved Home shuffle source.
 *
 * Shows persisted bag entries as known next photos. After the known sequence,
 * the seeded example is labelled illustrative because the actual system draw
 * happens only when Android wallpaper delivery becomes eligible. This view
 * cannot reserve a candidate, save a bag or call WallpaperManager.
 */
@Composable
internal fun GlimpseShuffleFilmstrip(state: GlimpseUiState) {
    val rotation = state.rotation
    var expanded by rememberSaveable(rotation?.collectionId) {
        mutableStateOf(false)
    }
    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(if (expanded) "Hide next-photo preview" else "Preview next four photos")
    }
    if (!expanded) return

    if (rotation == null) {
        Text(
            "Choose a collection and start shuffle first. " +
                "This preview cannot start or change a rotation.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    if (rotation.pendingPhotoId != null) {
        Text(
            "The previous Android wallpaper operation is uncertain. " +
                "Review that result before previewing another sequence.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    val sourceId = rotation.collectionId
    if (sourceId == null) {
        Text(
            "Choose a collection and start shuffle first. " +
                "This preview cannot start or change a rotation.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    if (sourceId != state.collectionId) {
        Text(
            "This preview belongs to the saved shuffle source, not the " +
                "collection you are browsing. Open the saved collection " +
                "to inspect its images. Browsing never changes the shuffle source.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    val photos = state.photos
    if (photos.isEmpty()) {
        Text(
            "No photographs are available in the saved shuffle collection.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val preview = remember(
        sourceId, photos, rotation.remainingJson,
        rotation.lastAcknowledgedPhotoId, rotation.lastAcknowledgedAt
    ) {
        // Old or malformed JSON must not crash Home or try to load unbounded
        // data. A malformed bag becomes an illustrative preview only.
        val savedQueue = runCatching {
            if (rotation.remainingJson.length > 100_000) emptyList()
            else {
                val array = JSONArray(rotation.remainingJson)
                (0 until minOf(array.length(), 300)).map { array.getString(it) }
            }
        }.getOrDefault(emptyList())
        ShuffleProjection.sample(
            available = photos.map { it.id },
            remaining = savedQueue,
            lastAcknowledged = rotation.lastAcknowledgedPhotoId,
            requestedSteps = 4,
            seed = (rotation.lastAcknowledgedAt ?: 1979L).hashCode()
        )
    }
    val byId = remember(photos) { photos.associateBy { it.id } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "READ-ONLY ROTATION REHEARSAL",
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            "Saved queue positions are real. When the unused queue ends, " +
                "the example photos are illustrations, not a prediction. " +
                "Android timing, failures and user changes can alter the result.",
            style = MaterialTheme.typography.bodySmall
        )
        if (preview.frames.isEmpty()) {
            Text(
                if (preview.stoppedBecauseOnePhoto)
                    "Only one image is available and it is already the last applied Home photo."
                else "No eligible photographs in the saved collection.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (frame in preview.frames) {
                    val photo = byId[frame.photoId] ?: continue
                    Card(modifier = Modifier.width(100.dp)) {
                        Column(
                            modifier = Modifier.padding(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PhotoPreview(
                                photo = photo,
                                modifier = Modifier.fillMaxWidth().height(108.dp),
                                label = if (frame.confidence ==
                                    ShuffleProjection.Confidence.SAVED_QUEUE)
                                    "Saved next image ${frame.position}, preview only"
                                else "Illustrative image ${frame.position}, preview only",
                                decodeMaxEdge = 256
                            )
                            Text(
                                if (frame.confidence ==
                                    ShuffleProjection.Confidence.SAVED_QUEUE)
                                    "NEXT ${frame.position} · SAVED"
                                else "STEP ${frame.position} · EXAMPLE",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Previewing does not apply a wallpaper, consume the no-repeat bag, " +
                "schedule a job or alter your current Home image.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
