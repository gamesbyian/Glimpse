package io.github.gamesbyian.glimpse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.os.PowerManager
import android.content.Context
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.domain.ReelWindow
import io.github.gamesbyian.glimpse.domain.ReelMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

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

/**
 * A rounded trapezoidal photographic opening set inside a broad paper ring.
 * Unlike the original pie-slice sectors, the photograph is inset from both the
 * rim and the hub. The baked-face PNG masks the final softly rounded edges.
 */
private fun aperture(cx: Float, cy: Float, radius: Float, middleDegrees: Float): Path {
    val outer = radius * ReelWindow.WINDOW_OUTER_RADIUS
    val inner = radius * ReelWindow.WINDOW_INNER_RADIUS
    val half = ReelWindow.WINDOW_HALF_ANGLE
    val start = middleDegrees - half
    val end = middleDegrees + half
    val corner = 3f
    val cornerRadius = radius * .037f
    fun radial(r: Float, degree: Float): Pair<Float, Float> {
        val a = Math.toRadians(degree.toDouble())
        return (cx + r * cos(a).toFloat()) to (cy + r * sin(a).toFloat())
    }
    val outerRect = RectF(cx - outer, cy - outer, cx + outer, cy + outer)
    val innerRect = RectF(cx - inner, cy - inner, cx + inner, cy + inner)
    return Path().apply {
        val first = radial(outer, start + corner)
        moveTo(first.first, first.second)
        arcTo(outerRect, start + corner, 2f * (half - corner))
        val outerEnd = radial(outer - cornerRadius, end)
        radial(outer, end).let { quadTo(it.first, it.second, outerEnd.first, outerEnd.second) }
        val innerEnd = radial(inner + cornerRadius, end)
        lineTo(innerEnd.first, innerEnd.second)
        val innerCurve = radial(inner, end - corner)
        radial(inner, end).let { quadTo(it.first, it.second, innerCurve.first, innerCurve.second) }
        arcTo(innerRect, end - corner, -2f * (half - corner))
        val innerStart = radial(inner + cornerRadius, start)
        radial(inner, start).let { quadTo(it.first, it.second, innerStart.first, innerStart.second) }
        val outerStart = radial(outer - cornerRadius, start)
        lineTo(outerStart.first, outerStart.second)
        radial(outer, start).let { quadTo(it.first, it.second, first.first, first.second) }
        close()
    }
}

/**
 * V0 functional circular browsing prototype:
 * - bounded five cutouts into any finite collection
 * - swipes and fixed semantic controls turn real photo IDs through legal notches
 * - all display rows are decoupled from WallpaperManager
 * - fixed pointer and center hub; only selected photo in hero is authoritative
 *
 * This first physically moving version is NOT the final photographed paper material;
 * it is the motion acceptance foundation. Real material art must be versioned
 * and measured against owner-supplied references before visual acceptance.
 * The final tactile texture, motion physics and exact responsive geometry
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
    val local = remember(records) { records.associateBy { it.id } }
    // The anchored slot IDs remain unchanged while the paper/photo disc turns.
    // Once the selected window settles at the pointer, rebind hidden slots.
    var anchorId by remember(ids) {
        mutableStateOf(if (selectedId in ids) selectedId else ids.firstOrNull())
    }
    val slots = ReelWindow.slots(ids, anchorId)
    val scope = rememberCoroutineScope()
    val angle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragAngle by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    // Baked material face: real raster fiber, bevel/edge light and five transparent
    // windows. Cached once; dynamic photos remain distinct bitmaps beneath it.
    val paperFace = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.glimpse_paper_reel_face,
            BitmapFactory.Options().apply { inScaled = false })
    }
    val facePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val reducedMotion = remember(context) {
        val animatorScale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val powerSave = runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
        }.getOrDefault(false)
        animatorScale <= 0f || powerSave
    }
    val renderedAngle = if (dragging) dragAngle else angle.value
    val updatedAngle by rememberUpdatedState(renderedAngle)
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

    fun beginSettle(startAngle: Float, steps: Int) {
        settleJob?.cancel()
        dragging = false
        val origin = ReelWindow.focusIndex(ids, anchorId)
        val legal = ReelMotion.legalSteps(origin, ids.size, steps)
        settleJob = scope.launch {
            angle.snapTo(startAngle)
            val target = ReelMotion.targetAngle(legal)
            if (reducedMotion) angle.snapTo(target)
            else angle.animateTo(target, tween(durationMillis = 270, easing = FastOutSlowInEasing))
            // Logical selection changes only at completed settle; never apply wallpaper.
            val targetId = ids.getOrNull(origin + legal)
            if (targetId != null) {
                anchorId = targetId
                angle.snapTo(0f)
                if (targetId != selectedId) updatedOnSelect(targetId)
            } else angle.snapTo(0f)
        }
    }

    // Stable external Previous/Next/Grid choice gets the same photographed wheel
    // continuity, without making a second selection mutation during animation.
    LaunchedEffect(selectedId, ids) {
        val target = ids.indexOf(selectedId)
        val origin = ids.indexOf(anchorId)
        if (target < 0) {
            settleJob?.cancel()
            anchorId = ids.firstOrNull()
            angle.snapTo(0f)
        } else if (target != origin) {
            settleJob?.cancel()
            dragging = false
            val delta = target - origin
            if (!reducedMotion && abs(delta) == 1) {
                angle.snapTo(0f)
                angle.animateTo(ReelMotion.targetAngle(delta),
                    tween(durationMillis = 270, easing = FastOutSlowInEasing))
            }
            anchorId = selectedId
            angle.snapTo(0f)
        }
    }
    Column(modifier = modifier) {
        Text("Browse the reel", fontWeight = FontWeight.SemiBold)
        Text(if (focus < 0) "No photographs" else "Photo ${focus + 1} of ${records.size} · Swipe to browse")
        Box(modifier = Modifier.fillMaxWidth().height(316.dp).clipToBounds()) {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(316.dp)
                    .semantics {
                        contentDescription = "Paper photo reel. Swipe left for next photo, right for previous. Previous and Next buttons are also available."
                    }
                    .pointerInput(ids, anchorId) {
                        detectTapGestures { position ->
                            // Prevent rotating an already moving paper window as a
                            // tap target. Fixed Previous/Next remain available.
                            if (dragging || abs(updatedAngle) > 1f) return@detectTapGestures
                            ReelWindow.tapOffset(
                                position.x, position.y,
                                size.width.toFloat(), size.height.toFloat()
                            )?.let { offset ->
                                if (offset != 0 && slots.getOrNull(offset + 2) != null) {
                                    beginSettle(0f, offset)
                                }
                            }
                        }
                    }
                    .pointerInput(ids, anchorId) {
                        detectDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                                dragging = true
                                dragAngle = angle.value
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragAngle = (dragAngle + ReelMotion.dragDegrees(
                                    amount.x, size.width.toFloat()
                                )).coerceIn(-2.5f * 72f, 2.5f * 72f)
                            },
                            onDragEnd = {
                                val origin = ReelWindow.focusIndex(ids, anchorId)
                                beginSettle(dragAngle,
                                    ReelMotion.settleSteps(dragAngle, origin, ids.size))
                            },
                            onDragCancel = { beginSettle(dragAngle, 0) }
                        )
                    }
            ) {
                val native = drawContext.canvas.nativeCanvas
                val w = size.width
                val h = size.height
                val placement = ReelWindow.placement(w, h)
                val cx = placement.centerX
                val cy = placement.centerY
                val r = placement.radius
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
                // The whole photo-and-paper surface turns together around its hub.
                // Fixed pointer below does not move. All five slots have real IDs.
                native.save()
                native.rotate(renderedAngle, cx, cy)
                // The outside of this full disc is clipped by the physical
                // content viewport; the disc has five genuine cutout paths.
                native.drawCircle(cx, cy + 5.dp.toPx(), r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x22000000
                })
                native.drawCircle(cx, cy, r, rim)
                for (offset in -2..2) {
                    val angle = -90f + offset * 72f
                    val wedge = aperture(cx, cy, r, angle)
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
                // Photo cutouts are painted first. The prepared translucent face
                // supplies actual material lighting, recessed cutout edges and hub.
                // It turns in exactly the same coordinate space as the photos.
                if (paperFace != null) {
                    native.drawBitmap(paperFace, null,
                        RectF(cx - r, cy - r, cx + r, cy + r), facePaint)
                } else {
                    native.drawCircle(cx, cy, inner * 1.02f, rim)
                    native.drawCircle(cx, cy, inner * .62f, empty)
                }
                native.restore()
                val markerTop = (cy - r + 4.dp.toPx()).coerceAtLeast(1.dp.toPx())
                val pointer = Path().apply {
                    moveTo(cx - 14.dp.toPx(), markerTop)
                    lineTo(cx + 14.dp.toPx(), markerTop)
                    lineTo(cx, markerTop + 24.dp.toPx())
                    close()
                }
                native.drawPath(pointer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFE8876C.toInt()
                })
            }
        }
    }
}
