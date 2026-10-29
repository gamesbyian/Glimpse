package io.github.gamesbyian.glimpse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.gamesbyian.glimpse.domain.RecentImportFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Full-height Room-backed contact sheet. Each on-screen page is queried from
 * SQLite without first collecting all photo records into the Home ViewModel.
 * The selected original is observed separately by stable ID.
 *
 * All filters are read-only; Grid taps only move the browse cursor, never the
 * saved rotation source or Android wallpaper. Returning to Reel restores the
 * existing finite ordering with the same stable selected photo.
 */
@Composable
internal fun GlimpseGridRoom(
    state: GlimpseUiState,
    model: GlimpseModel,
    onBack: () -> Unit,
    onReel: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionId = state.collectionId
    val total = state.collectionOverviews.firstOrNull { it.id == collectionId }?.photoCount ?: 0
    var recentOnly by rememberSaveable(collectionId) { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable(collectionId) { mutableStateOf(false) }
    var timeSnapshot by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(recentOnly, collectionId) {
        timeSnapshot = System.currentTimeMillis()
        while (recentOnly && isActive) {
            delay(60_000L)
            timeSnapshot = System.currentTimeMillis()
        }
    }
    val filter = when {
        favoritesOnly -> 2
        recentOnly -> 1
        else -> 0
    }
    val recentCutoff = if (recentOnly)
        (timeSnapshot - RecentImportFilter.WEEK_MILLIS).coerceAtLeast(0L)
    else 0L
    val pageFlow = remember(collectionId, filter, recentCutoff) {
        model.contactSheetPages(collectionId.orEmpty(), filter, recentCutoff, timeSnapshot)
    }
    val paged = pageFlow.collectAsLazyPagingItems()
    val loading = paged.loadState.refresh is LoadState.Loading
    val loadError = paged.loadState.refresh is LoadState.Error
    val selected = state.current
    val gridState = rememberLazyGridState()
    LaunchedEffect(collectionId, filter) { gridState.scrollToItem(0) }

    val collection = state.collections.firstOrNull { it.id == collectionId }?.title
        ?: "Your collection"
    val paper = Color(0xFFF7F0E6)
    val ink = Color(0xFF25423F)

    Column(
        modifier = modifier.fillMaxSize().background(paper).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Back to Home")
            }
            OutlinedButton(
                onClick = onReel,
                enabled = total > 0,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Browse photo reel") }
        }
        Text("THE CONTACT SHEET", color = Color(0xFF986950),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium, letterSpacing = 1.3.sp)
        Text(collection, color = ink, fontFamily = GlimpseType.editorial,
            fontSize = 28.sp)
        Text(
            "$total photographs · Selection only; wallpaper unchanged",
            color = Color(0xFF695D52),
            style = MaterialTheme.typography.labelMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = filter == 0,
                onClick = { recentOnly = false; favoritesOnly = false },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("All photos") }
            )
            FilterChip(
                selected = filter == 1,
                onClick = { recentOnly = true; favoritesOnly = false },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("Imported last 7 days") }
            )
            FilterChip(
                selected = filter == 2,
                onClick = { favoritesOnly = true; recentOnly = false },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text("Favorites") }
            )
        }
        if (recentOnly && !loading) {
            Text(
                "${paged.itemCount} imported in the last seven days · $total total",
                color = Color(0xFF695D52),
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (favoritesOnly && !loading) {
            Text(
                "${paged.itemCount} favorites in this collection · " +
                    "$total total. Favorite flags are global to original photos.",
                color = Color(0xFF695D52),
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (total == 0) {
            Text("No photos in this collection yet. Add photos from Home.",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (selected != null) "IN FOCUS · ${state.gridPosition} OF $total"
                        else "CHOOSE A PHOTOGRAPH",
                        color = ink, style = MaterialTheme.typography.labelLarge)
                    PhotoPreview(
                        photo = selected,
                        modifier = Modifier.fillMaxWidth().height(126.dp),
                        label = "Current contact sheet selection, preview only"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDetails,
                            enabled = selected != null,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Photo details") }
                        Button(
                            onClick = model::applyPreview,
                            enabled = !state.busy && selected != null,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Set Home wallpaper now") }
                    }
                }
            }
            if (loadError) {
                Text("The contact sheet couldn't load. Your original photos are unchanged.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { paged.retry() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Retry loading photos") }
            } else if (loading && paged.itemCount == 0) {
                Text("Loading photographs…", style = MaterialTheme.typography.bodyMedium)
            } else if (paged.itemCount == 0) {
                Text(
                    if (favoritesOnly)
                        "No favorites in this collection yet. Mark a photo in Photo Details " +
                            "to include it here. Select All photos to browse everything."
                    else if (recentOnly)
                        "No photos were imported in the last seven days. " +
                            "Select All photos to browse the collection."
                    else "No photographs available in this collection.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 102.dp),
                state = gridState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paged.itemCount, key = paged.itemKey { it.id }) { position ->
                    val photo = paged[position]
                    val active = photo?.id == selected?.id
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(128.dp)
                            .border(if (active) 3.dp else 1.dp,
                                if (active) Color(0xFF24665E)
                                else Color(0xFFC6B7A4), shape)
                            .then(if (photo != null) Modifier.clickable {
                                collectionId?.let { model.browsePhotoInCollection(it, photo.id) }
                            } else Modifier)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photo == null) {
                            Text("Loading…", style = MaterialTheme.typography.labelSmall)
                        } else PagedGridThumbnail(
                            photo = photo,
                            modifier = Modifier.fillMaxSize(),
                            label = "Photograph ${position + 1} of ${paged.itemCount}" +
                                if (active) ", selected" else ", choose photo"
                        )
                    }
                }
                if (paged.loadState.append is LoadState.Error) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "page-error") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("More photos could not be loaded. Existing photos are safe.",
                                style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(
                                onClick = { paged.retry() },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Retry loading more") }
                        }
                    }
                } else if (paged.loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "page-loading") {
                        Text("Loading more photographs…",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
