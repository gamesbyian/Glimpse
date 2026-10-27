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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gamesbyian.glimpse.data.CollectionOverview

/**
 * One readable column rather than 76dp thumbnail + one-word-wide copy when
 * 320dp or 200% font scaling makes the original horizontal card impossible.
 * Uses Android's actual Compose density/configuration, no emulator special case.
 */
@Composable
private fun needsStackedPhotoCard(): Boolean =
    LocalDensity.current.fontScale >= 1.5f ||
        LocalConfiguration.current.screenWidthDp <= 360

/**
 * Home truth remains stable as browsing changes. Android's wallpaper is not
 * readable as a trustworthy state acknowledgment across OEMs and other apps.
 */
@Composable
internal fun GlimpseWallpaperSummary(state: GlimpseUiState) {
    val acknowledged = state.acknowledgedPhoto
    val stacked = needsStackedPhotoCard()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E4040))
    ) {
        if (stacked && acknowledged != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhotoPreview(
                    photo = acknowledged,
                    modifier = Modifier.fillMaxWidth().height(156.dp),
                    label = "Photograph from last Glimpse wallpaper acknowledgment"
                )
                WallpaperSummaryCopy(state)
            }
        } else {
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
                WallpaperSummaryCopy(state, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WallpaperSummaryCopy(state: GlimpseUiState, modifier: Modifier = Modifier) {
    val acknowledged = state.acknowledgedPhoto
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
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

/** Named collection and real membership count, using a genuine local cover. */
@Composable
internal fun GlimpseCollectionTile(
    overview: CollectionOverview,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val stacked = needsStackedPhotoCard()
    val width = if (stacked) {
        (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(200).dp
    } else 214.dp
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier.width(width)
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
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PhotoPreview(
                    photo = null,
                    pathOverride = overview.coverPath,
                    modifier = Modifier.fillMaxWidth().height(128.dp),
                    label = "Cover image for " + overview.title
                )
                CollectionTileCopy(overview, selected, stacked = true)
            }
        } else {
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
                CollectionTileCopy(overview, selected, stacked = false)
            }
        }
    }
}

@Composable
private fun CollectionTileCopy(
    overview: CollectionOverview,
    selected: Boolean,
    stacked: Boolean
) {
    Column(
        modifier = if (stacked) Modifier.fillMaxWidth() else Modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            overview.title,
            fontFamily = GlimpseType.editorial,
            color = Color(0xFF244344),
            style = MaterialTheme.typography.titleMedium,
            maxLines = if (stacked) 6 else 3,
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
