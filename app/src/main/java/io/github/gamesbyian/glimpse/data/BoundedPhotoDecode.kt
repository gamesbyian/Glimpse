package io.github.gamesbyian.glimpse.data

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Preview and wallpaper pixels must respect the same EXIF-orientation contract.
 * ImageDecoder is available across Glimpse's API29+ range and applies embedded
 * EXIF orientation without modifying the immutable imported original.
 */
internal object BoundedPhotoDecode {
    /** Never upscale or exceed maxEdge; preserve aspect ratio within rounding. */
    fun targetSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxEdge > 0)
        val factor = (maxEdge.toDouble() / max(width, height)).coerceAtMost(1.0)
        return (
            (width * factor).roundToInt().coerceIn(1, maxEdge)
        ) to (
            (height * factor).roundToInt().coerceIn(1, maxEdge)
        )
    }

    /** Invoke only from IO/background work, never during a Compose draw pass. */
    fun decode(file: File, maxEdge: Int, lowMemory: Boolean = false): Bitmap {
        require(maxEdge in 1..4096) { "Invalid bounded decoder size" }
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val (width, height) = targetSize(
                info.size.width, info.size.height, maxEdge
            )
            decoder.setTargetSize(width, height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            if (lowMemory) {
                decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
            }
        }
    }
}
