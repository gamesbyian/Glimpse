package io.github.gamesbyian.glimpse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.domain.ReelWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/** Small global bounded thumbnail cache; never loads full-resolution originals. */
private object ReelThumbs : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private fun smallBitmap(file: String): Bitmap? {
    synchronized(ReelThumbs) { ReelThumbs.get(file)?.let { return it } }
    val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file, size)
    if (size.outWidth <= 0 || size.outHeight <= 0) return null
    var sample = 1
    while (size.outWidth / sample > 512 || size.outHeight / sample > 512) {
        sample *= 2
    }
    val image = BitmapFactory.decodeFile(file, BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }) ?: return null
    synchronized(ReelThumbs) { ReelThumbs.put(file, image) }
    return image
}

/** Draws a paper aperture around real photo identity without mutating wallpaper. */
private fun aperture(cx: Float, cy: Float, outer: Float, inner: Float, middleDegrees: Float): Path {
    val rectOuter = RectF(cx - outer, cy - outer, cx + outer, cy + outer)
    val rectInner = RectF(cx - inner, cy - inner, cx + inner, cy + inner)
    val start = middleDegrees - 31f
    val end = middleDegrees + 31f
    val x = cx + outer * cos(Math.toRadians(start.toDouble())).toFloat()
    val y = cy + outer * sin(Math.toRadians(start.toDouble())).toFloat()
    return Path().apply {
        moveTo(x, y)
        arcTo(rectOuter, start, 62f)
        lineTo(
            cx + inner * cos(Math.toRadians(end.toDouble())).toFloat(),
            cy + inner * sin(Math.toRadians(end.toDouble())).toFloat()
        )
        arcTo(rectInner, end, -62f)
        close()
    }
}

/**
 * V0 functional circular browsing prototype:
 * - bounded five cutouts into any finite collection
 * - left/right swipes change one photo ID
 * - all display rows are decoupled from WallpaperManager
 * - fixed pointer and center hub; only selected photo in hero is authoritative
 *
 * The final tactile texture, rotation physics and exact responsive geometry
 * remain subject to reference approval and Android-device capture.
 */
@Composable
fun CircularPhotoReel(
    records: List<PhotoRecord>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ids = remember(records) { records.map { it.id } }
    val focus = ReelWindow.focusIndex(ids, selectedId)
    val slots = ReelWindow.slots(ids, selectedId)
    val local = remember(records) { records.associateBy { it.id } }
    val thumbnailFiles = slots.filterNotNull().mapNotNull { local[it]?.localPath }
    val thumbs by produceState(
        initialValue = emptyMap<String, Bitmap>(),
        key1 = thumbnailFiles
    ) {
        value = withContext(Dispatchers.IO) {
            thumbnailFiles.distinct().mapNotNull { file ->
                try { smallBitmap(file)?.let { file to it } } catch (_: Exception) { null }
            }.toMap()
        }
    }
    val updatedOnSelect by rememberUpdatedState(onSelect)
    val dragThreshold = with(LocalDensity.current) { 44.dp.toPx() }
    var totalDrag by remember(selectedId, records) { mutableFloatStateOf(0f) }
    Column(modifier = modifier) {
        Text("Browse the reel", fontWeight = FontWeight.SemiBold)
        Text(if (focus < 0) "No photographs" else "Photo ${focus + 1} of ${records.size} · Swipe to browse")
        Box(modifier = Modifier.fillMaxWidth().height(316.dp).clipToBounds()) {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(316.dp)
                    .semantics {
                        contentDescription = "Paper photo reel. Swipe left for next photo, right for previous. Previous and Next buttons are also available."
                    }
                    .pointerInput(ids, selectedId) {
                        detectTapGestures { position ->
                            ReelWindow.tapOffset(
                                position.x, position.y,
                                size.width.toFloat(), size.height.toFloat()
                            )?.let { offset ->
                                slots.getOrNull(offset + 2)?.let(updatedOnSelect)
                            }
                        }
                    }
                    .pointerInput(ids, selectedId) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                totalDrag += amount
                            },
                            onDragEnd = {
                                val delta = when {
                                    totalDrag < -dragThreshold -> 1
                                    totalDrag > dragThreshold -> -1
                                    else -> 0
                                }
                                if (delta != 0) {
                                    ReelWindow.next(ids, selectedId, delta)?.let(updatedOnSelect)
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = { totalDrag = 0f }
                        )
                    }
            ) {
                val native = drawContext.canvas.nativeCanvas
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h * 0.82f
                val r = w * .84f
                val inner = r * .29f
                val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEAD9BD.toInt() }
                val empty = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF6F0E6.toInt() }
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF9F886A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5.dp.toPx()
                }
                val picture = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val selectedStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1D5960.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 5.dp.toPx()
                }
                // The outside of this full disc is clipped by the physical
                // content viewport; the disc has five genuine cutout paths.
                native.drawCircle(cx, cy + 5.dp.toPx(), r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x22000000
                })
                native.drawCircle(cx, cy, r, rim)
                for (offset in -2..2) {
                    val angle = -90f + offset * 72f
                    val wedge = aperture(cx, cy, r - 14.dp.toPx(), inner, angle)
                    native.drawPath(wedge, empty)
                    val id = slots[offset + 2]
                    val bitmap = id?.let(local::get)?.let { thumbs[it.localPath] }
                    if (bitmap != null) {
                        val rad = Math.toRadians(angle.toDouble())
                        val mid = (r + inner) * .53f
                        val x = cx + cos(rad).toFloat() * mid
                        val y = cy + sin(rad).toFloat() * mid
                        val dw = r * 1.04f
                        val dh = r * 1.05f
                        val dst = RectF(x - dw / 2, y - dh / 2, x + dw / 2, y + dh / 2)
                        native.save()
                        native.clipPath(wedge)
                        // Centre-crop into the paper aperture. Never stretch
                        // the original imported artwork to fit a wedge.
                        val targetAspect = dw / dh
                        val srcAspect = bitmap.width.toFloat() / bitmap.height
                        val src: Rect = if (srcAspect > targetAspect) {
                            val crop = (bitmap.height * targetAspect).toInt().coerceAtLeast(1)
                            val x0 = (bitmap.width - crop) / 2
                            Rect(x0, 0, x0 + crop, bitmap.height)
                        } else {
                            val crop = (bitmap.width / targetAspect).toInt().coerceAtLeast(1)
                            val y0 = (bitmap.height - crop) / 2
                            Rect(0, y0, bitmap.width, y0 + crop)
                        }
                        native.drawBitmap(bitmap, src, dst, picture)
                        native.restore()
                    }
                    native.drawPath(wedge, stroke)
                    if (offset == 0 && id != null) native.drawPath(wedge, selectedStroke)
                }
                native.drawCircle(cx, cy, inner * 1.02f, rim)
                native.drawCircle(cx, cy, inner * .62f, empty)
                val pointer = Path().apply {
                    moveTo(cx - 14.dp.toPx(), 1.dp.toPx())
                    lineTo(cx + 14.dp.toPx(), 1.dp.toPx())
                    lineTo(cx, 25.dp.toPx())
                    close()
                }
                native.drawPath(pointer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFE8876C.toInt()
                })
            }
        }
    }
}
