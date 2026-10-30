package io.github.gamesbyian.glimpse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.github.gamesbyian.glimpse.data.PhotoRecord
import java.io.File

/**
 * Coil owns cancellation, decoding and a bounded viewport-specific bitmap
 * request/cache for local contact-sheet thumbnails. This is deliberately
 * isolated from the canonical original verifier and final wallpaper renderer.
 * It never uploads or modifies the photo, fetches a URL, or applies wallpaper.
 */
@Composable
internal fun PagedGridThumbnail(
    photo: PhotoRecord,
    modifier: Modifier,
    label: String
) {
    var failed by remember(photo.id) { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = File(photo.localPath),
            contentDescription = label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { failed = true },
            onSuccess = { failed = false }
        )
        if (failed) Text("Image unavailable", style = MaterialTheme.typography.labelSmall)
    }
}
