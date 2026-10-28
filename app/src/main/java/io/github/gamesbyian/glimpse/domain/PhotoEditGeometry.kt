package io.github.gamesbyian.glimpse.domain

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Reversible crop/rotation geometry now powers the interactive Photo Detail
 * preview and bounded PNG export. It is not yet a persisted Room edit recipe
 * or an edited Android wallpaper apply operation.
 *
 * Coordinates are normalized against the EXIF-upright image after the selected
 * clockwise quarter-turn rotation, so the editor can retain one definition
 * regardless of preview/export decoded pixel resolution.
 */
data class NormalizedCrop(
    val left: Double = 0.0,
    val top: Double = 0.0,
    val right: Double = 1.0,
    val bottom: Double = 1.0
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() }) {
            "Crop coordinates must be finite."
        }
        require(left >= 0.0 && top >= 0.0 && right <= 1.0 && bottom <= 1.0 &&
            right > left && bottom > top
        ) { "Crop must have positive area and stay within the oriented image." }
    }
}

data class PixelCrop(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class PhotoEditGeometry(
    val clockwiseQuarterTurns: Int = 0,
    val crop: NormalizedCrop = NormalizedCrop()
) {
    init {
        require(clockwiseQuarterTurns in 0..3) {
            "Use 0, 1, 2 or 3 clockwise quarter-turns."
        }
    }

    /** Image dimensions after EXIF orientation, before any editor rotation. */
    fun orientedDimensions(uprightWidth: Int, uprightHeight: Int): Pair<Int, Int> {
        require(uprightWidth > 0 && uprightHeight > 0)
        return if (clockwiseQuarterTurns % 2 == 0) {
            uprightWidth to uprightHeight
        } else {
            uprightHeight to uprightWidth
        }
    }

    /**
     * Round outward to include every selected pixel, guarantee at least 1 px
     * in each axis for a positive-area crop, and clamp within valid bounds.
     * Identical recipe scales between bounded preview and original-resolution
     * export without destructive changes to the source image.
     */
    fun pixelCrop(uprightWidth: Int, uprightHeight: Int): PixelCrop {
        val (width, height) = orientedDimensions(uprightWidth, uprightHeight)
        val l = floor(crop.left * width).toInt().coerceIn(0, width - 1)
        val t = floor(crop.top * height).toInt().coerceIn(0, height - 1)
        val r = ceil(crop.right * width).toInt().coerceIn(l + 1, width)
        val b = ceil(crop.bottom * height).toInt().coerceIn(t + 1, height)
        return PixelCrop(l, t, r, b)
    }

    fun rotateClockwise(): PhotoEditGeometry =
        copy(clockwiseQuarterTurns = (clockwiseQuarterTurns + 1) % 4)

    fun rotateCounterclockwise(): PhotoEditGeometry =
        copy(clockwiseQuarterTurns = (clockwiseQuarterTurns + 3) % 4)

    fun reset(): PhotoEditGeometry = PhotoEditGeometry()
}
