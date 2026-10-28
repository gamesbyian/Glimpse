package io.github.gamesbyian.glimpse.data

import android.content.Context
import io.github.gamesbyian.glimpse.domain.NormalizedCrop
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry

/**
 * Small, versioned normalized-coordinate recipe. No URI, path, image pixels or
 * private metadata is stored. All records are keyed only by stable photo SHA.
 *
 * This is separate from the rendered edited PNG: originals stay immutable and
 * saved recipes can be reopened, changed or reset without baking each step.
 */
internal object PhotoEditRecipeCodec {
    fun encode(value: PhotoEditGeometry): String = listOf(
        "1",
        value.clockwiseQuarterTurns.toString(),
        value.crop.left.toString(),
        value.crop.top.toString(),
        value.crop.right.toString(),
        value.crop.bottom.toString()
    ).joinToString("|")

    fun decode(raw: String?): PhotoEditGeometry? {
        if (raw.isNullOrBlank() || raw.length > 256) return null
        val values = raw.split('|')
        if (values.size != 6 || values[0] != "1") return null
        return try {
            val quarter = values[1].toInt()
            val geometry = PhotoEditGeometry(quarter, NormalizedCrop(
                values[2].toDouble(), values[3].toDouble(),
                values[4].toDouble(), values[5].toDouble()
            ))
            geometry
        } catch (_: Exception) {
            null
        }
    }
}

internal class PhotoEditRecipeStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "glimpse_edit_recipes_v1", Context.MODE_PRIVATE
    )
    private fun key(sha: String): String {
        require(sha.matches(Regex("[0-9a-f]{64}"))) {
            "The photo identifier is invalid."
        }
        return "photo:" + sha
    }

    /** These methods must be invoked on Dispatchers.IO, not from Compose. */
    fun load(sha: String): PhotoEditGeometry? =
        PhotoEditRecipeCodec.decode(prefs.getString(key(sha), null))

    fun save(sha: String, recipe: PhotoEditGeometry): Boolean =
        prefs.edit().putString(key(sha), PhotoEditRecipeCodec.encode(recipe)).commit()

    fun clear(sha: String): Boolean =
        prefs.edit().remove(key(sha)).commit()
}
