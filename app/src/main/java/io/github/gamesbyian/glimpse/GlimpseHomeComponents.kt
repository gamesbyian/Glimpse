package io.github.gamesbyian.glimpse

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gamesbyian.glimpse.data.CollectionOverview

/**
 * Home truth remains stable as browsing changes. Android's wallpaper is not
 * readable as a trustworthy state acknowledgment across OEMs and other apps.
 */
@Composable
internal fun GlimpseWallpaperSummary(state: GlimpseUiState) {
    val acknowledged = state.acknowledgedPhoto
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E4040))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (acknowledged != null) {
                PhotoPreview(
                    photo = acknowledged,
                    modifier = Modifier.width(84.dp).height(104.dp),
                    label = "Photograph from last Glimpse wallpaper acknowledgment"
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "HOME WALLPAPER",
                    color = Color(0xFFE0B991),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.3.sp
                )
                Text(
                    if (acknowledged != null) "Last confirmed." else "A fresh perspective.",
                    color = Color(0xFFFFF7E8),
                    fontFamily = GlimpseType.editorial,
                    fontSize = 23.sp
                )
                Text(
                    when {
                        acknowledged != null ->
                            "Glimpse last received Android's acknowledgment for this photo. " +
                                "The current system wallpaper has not been read back."
                        state.rotation?.lastAcknowledgedPhotoId != null ->
                            "An earlier wallpaper acknowledgment exists, but its original is unavailable."
                        else ->
                            "No wallpaper change has been acknowledged yet. " +
                                "Browse below, then choose Set Home wallpaper."
                    },
                    color = Color(0xFFF0E4D4),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Named collection and real membership count, using a genuine local cover. */
@Composable
internal fun GlimpseCollectionTile(
    overview: CollectionOverview,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier.width(214.dp)
            .heightIn(min = 124.dp)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color(0xFF24605B) else Color(0xFFD4C3AE),
                shape
            )
            .semantics {
                contentDescription = overview.title + ", " + overview.photoCount +
                    " photos" + if (selected) ", selected" else ", open collection"
            }
            .clickable(onClick = onSelect),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE8E9D9) else Color(0xFFFFF9EF)
        )
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoPreview(
                photo = null,
                pathOverride = overview.coverPath,
                modifier = Modifier.width(76.dp).height(106.dp),
                label = "Cover image for " + overview.title
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    overview.title,
                    fontFamily = GlimpseType.editorial,
                    color = Color(0xFF244344),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (overview.photoCount == 1) "1 photograph"
                    else "${overview.photoCount} photographs",
                    color = Color(0xFF695C4F),
                    style = MaterialTheme.typography.labelMedium
                )
                if (selected) {
                    Text(
                        "IN VIEW",
                        color = Color(0xFF23635C),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
