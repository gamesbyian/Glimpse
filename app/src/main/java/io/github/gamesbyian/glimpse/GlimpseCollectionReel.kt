package io.github.gamesbyian.glimpse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Path
import android.graphics.RectF
import android.provider.Settings
import android.os.PowerManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.CollectionOverview
import io.github.gamesbyian.glimpse.domain.CollectionReelMath
import io.github.gamesbyian.glimpse.domain.ReelMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A SECOND, functional physical reel. It changes which real collection the
 * user is browsing, never the active shuffle source or device wallpaper.
 *
 * The five-window paper material is shared with the photographic reel, but
 * its top-right hub/selection point is intentionally independent. It is only
 * shown in Home when at least two collections exist and there is enough space.
 * The ordinary horizontally scrolling collection cards remain the accessible
 * small-screen and large-type alternative.
 */
@Composable
internal fun GlimpseCollectionReel(
    collections: List<CollectionRecord>,
    overviews: List<CollectionOverview>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ids = remember(collections) { collections.map { it.id } }
    val focus = ids.indexOf(selectedId)
    if (ids.size < 2 || focus < 0) return
    // A collection wheel should identify its *real* contents, not merely
    // display invented ordinal sectors. Reuse the existing SHA-originals
    // bounded thumbnail cache and draw only the five physically visible
    // collection covers behind the real die-cut paper apertures.
    val coverPaths = remember(collections, overviews, focus) {
        val byId = overviews.associateBy { it.id }
        (-2..2).mapNotNull { slot ->
            CollectionReelMath.target(focus, ids.size, slot)
                ?.let { byId[ids[it]]?.coverPath }
        }.distinct()
    }
    val covers by produceState(
        initialValue = emptyMap<String, Bitmap>(),
        key1 = coverPaths
    ) {
        value = withContext(Dispatchers.IO) {
            coverPaths.mapNotNull { path ->
                try { smallBitmap(path)?.let { path to it } }
                catch (_: Exception) { null }
            }.toMap()
        }
    }
    val coverById = remember(collections, overviews) {
        val byId = overviews.associateBy { it.id }
        collections.associate { collection ->
            collection.id to byId[collection.id]?.coverPath
        }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val lowPower = runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.isPowerSaveMode ?: false
        }.getOrDefault(false)
        scale <= 0f || lowPower
    }
    val updatedOnSelect by rememberUpdatedState(onSelect)
    val paperFace = remember(context) {
        BitmapFactory.decodeResource(context.resources, R.drawable.glimpse_paper_reel_face,
            BitmapFactory.Options().apply { inScaled = false })
    }
    val facePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val windowPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF315B56.toInt() } }
    val coverPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val digits = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8EEDC.toInt()
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
    } }
    val angle = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragAngle by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val renderedAngle = if (dragging) dragAngle else angle.value
    val centerFractionX = .80f
    val centerFractionY = .18f
    val radiusFraction = .68f
    val currentTitle = collections[focus].title

    fun settle(start: Float, steps: Int) {
        settleJob?.cancel()
        val target = CollectionReelMath.target(focus, ids.size, steps)
        settleJob = scope.launch {
            angle.snapTo(start)
            dragging = false
            if (reducedMotion) {
                angle.snapTo(ReelMotion.targetAngle(steps))
            } else {
                angle.animateTo(ReelMotion.targetAngle(steps),
                    tween(durationMillis = 250, easing = FastOutSlowInEasing))
            }
            if (target != null && target != focus) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                updatedOnSelect(ids[target])
            }
            angle.snapTo(0f)
        }
    }

    LaunchedEffect(selectedId) {
        settleJob?.cancel()
        angle.snapTo(0f)
        dragging = false
        dragAngle = 0f
    }

    Box(modifier = modifier.size(196.dp).clipToBounds()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .semantics {
                    contentDescription = "Collection paper reel. Turn to switch collections."
                    stateDescription = "Collection ${focus + 1} of ${ids.size}: ${currentTitle}"
                    customActions = buildList {
                        if (focus > 0) {
                            add(CustomAccessibilityAction("Previous collection") {
                                updatedOnSelect(ids[focus - 1])
                                true
                            })
                        }
                        if (focus < ids.lastIndex) {
                            add(CustomAccessibilityAction("Next collection") {
                                updatedOnSelect(ids[focus + 1])
                                true
                            })
                        }
                    }
                }
                .pointerInput(ids, selectedId) {
                    detectTapGestures { pos ->
                        if (dragging) return@detectTapGestures
                        val cx = size.width * centerFractionX
                        val cy = size.height * centerFractionY
                        val r = size.width * radiusFraction
                        val offset = CollectionReelMath.windowOffset(
                            pos.x, pos.y, cx, cy, r
                        ) ?: return@detectTapGestures
                        val target = CollectionReelMath.target(focus, ids.size, offset)
                        if (target != null && target != focus) settle(0f, offset)
                    }
                }
                .pointerInput(ids, selectedId) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val cx = size.width * centerFractionX
                            val cy = size.height * centerFractionY
                            val r = size.width * radiusFraction
                            val distance = hypot(pos.x - cx, pos.y - cy)
                            dragging = distance <= r && distance >= r * .27f
                            if (dragging) {
                                settleJob?.cancel()
                                dragAngle = angle.value
                            }
                        },
                        onDrag = { change, _ ->
                            if (dragging) {
                                change.consume()
                                val cx = size.width * centerFractionX
                                val cy = size.height * centerFractionY
                                val r = size.width * radiusFraction
                                dragAngle = (
                                    dragAngle + ReelMotion.angularDragDegrees(
                                        change.previousPosition.x, change.previousPosition.y,
                                        change.position.x, change.position.y,
                                        cx, cy, r, size.width.toFloat()
                                    )
                                ).coerceIn(-144f, 144f)
                            }
                        },
                        onDragEnd = {
                            if (dragging) {
                                val old = dragAngle
                                val steps = ReelMotion.settleSteps(old, focus, ids.size)
                                settle(old, steps)
                            }
                        },
                        onDragCancel = {
                            if (dragging) settle(dragAngle, 0)
                        }
                    )
                }
        ) {
            val native = drawContext.canvas.nativeCanvas
            val cx = size.width * centerFractionX
            val cy = size.height * centerFractionY
            val radius = size.width * radiusFraction
            val labelRadius = radius * .61f
            digits.textSize = 12.sp.toPx()
            native.save()
            // The owner-approved paper face's selected window points at -90°.
            // Rotating its material by 216° positions it at the exposed lower-
            // left sector of this clipped *top-right* physical reel.
            native.rotate(216f + renderedAngle, cx, cy)
            native.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFEEE0C5.toInt()
            })
            for (offset in -2..2) {
                val candidate = CollectionReelMath.target(focus, ids.size, offset)
                if (candidate != null) {
                    val radians = Math.toRadians((-90f + offset * 72f).toDouble())
                    val x = cx + cos(radians).toFloat() * labelRadius
                    val y = cy + sin(radians).toFloat() * labelRadius
                    val oval = aperture(cx, cy, radius,
                        -90f + offset * ReelMotion.NOTCH_DEGREES)
                    val cover = coverById[ids[candidate]]?.let(covers::get)
                    if (cover != null) {
                        // Actual photo cover, clipped inside exactly the same
                        // physical window geometry as the primary photo reel.
                        val side = minOf(cover.width, cover.height)
                        val src = Rect((cover.width - side) / 2,
                            (cover.height - side) / 2,
                            (cover.width + side) / 2,
                            (cover.height + side) / 2)
                        val half = radius * .53f
                        native.save()
                        native.clipPath(oval)
                        native.drawBitmap(cover, src,
                            RectF(x - half, y - half, x + half, y + half),
                            coverPaint)
                        native.restore()
                    } else {
                        // An empty collection still owns a real selectable
                        // window, not a fabricated stock-photo thumbnail.
                        native.drawRoundRect(
                            RectF(x - radius * .145f, y - radius * .095f,
                                x + radius * .145f, y + radius * .095f),
                            radius * .028f, radius * .028f, windowPaint
                        )
                    }
                    // Legible finite index overlay; identity remains the
                    // collection's actual title in the fixed label and a11y.
                    native.drawRoundRect(
                        RectF(x - radius * .13f, y - radius * .09f,
                            x + radius * .13f, y + radius * .09f),
                        radius * .03f, radius * .03f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xBE1A3933.toInt()
                        }
                    )
                    native.drawText((candidate + 1).toString().padStart(2, '0'),
                        x, y + digits.textSize * .33f, digits)
                }
            }
            if (paperFace != null) {
                native.drawBitmap(paperFace, null,
                    RectF(cx - radius, cy - radius, cx + radius, cy + radius), facePaint)
            }
            native.restore()
            // The indexing pointer does not turn with the wheel.
            val pointerAngle = Math.toRadians(CollectionReelMath.SELECTED_DEGREES.toDouble())
            val markerRadius = radius * .94f
            val px = cx + cos(pointerAngle).toFloat() * markerRadius
            val py = cy + sin(pointerAngle).toFloat() * markerRadius
            val marker = Path().apply {
                moveTo(px - 6.dp.toPx(), py + 6.dp.toPx())
                lineTo(px + 6.dp.toPx(), py + 6.dp.toPx())
                lineTo(px, py - 6.dp.toPx())
                close()
            }
            native.drawPath(marker, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF385C55.toInt()
            })
        }
        Text(
            text = "${focus + 1}/${ids.size} · ${currentTitle}",
            fontFamily = GlimpseType.editorial,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFF294640),
            // Keep the actual selected collection discoverable while the
            // photographic hero remains dominant. A two-line floating card
            // obscured too much of the owner-approved visual composition.
            modifier = Modifier.align(Alignment.BottomStart)
                .widthIn(max = 146.dp)
                .padding(bottom = 3.dp)
                .background(Color(0xFFF8EDDC), RoundedCornerShape(9.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
