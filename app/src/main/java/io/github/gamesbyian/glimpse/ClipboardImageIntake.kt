package io.github.gamesbyian.glimpse

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

/**
 * Deliberate foreground-only clipboard read. Does not poll, observe clipboard
 * changes, store ephemeral grants, or make background image acquisitions.
 */
internal sealed interface ClipboardImageResult {
    data class Ready(val uris: List<Uri>) : ClipboardImageResult
    data class Unavailable(val explanation: String) : ClipboardImageResult
}

internal enum class ClipboardImageCheck {
    READY, EMPTY, NOT_IMAGE, NON_CONTENT_URI, TOO_MANY
}

internal fun checkClipboardImages(mimeTypes: List<String>, schemes: List<String?>):
    ClipboardImageCheck {
    if (schemes.isEmpty()) return ClipboardImageCheck.EMPTY
    if (schemes.size > 100) return ClipboardImageCheck.TOO_MANY
    if (mimeTypes.none {
            it.startsWith("image/", ignoreCase = true) && it.length > "image/".length
        }
    ) return ClipboardImageCheck.NOT_IMAGE
    if (schemes.any { it != "content" }) return ClipboardImageCheck.NON_CONTENT_URI
    return ClipboardImageCheck.READY
}

internal object ClipboardImageIntake {
    fun readOnUserTap(context: Context): ClipboardImageResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
            val clip = clipboard.primaryClip ?: return ClipboardImageResult.Unavailable(
                "Android clipboard has no image. Use Add photos or Share from the screenshot toolbar."
            )
            val mimeTypes = (0 until clip.description.mimeTypeCount)
                .map { clip.description.getMimeType(it) }
            val uris = (0 until clip.itemCount).map { clip.getItemAt(it).uri }.distinct()
            when (checkClipboardImages(mimeTypes, uris.map { it?.scheme })) {
                ClipboardImageCheck.READY ->
                    ClipboardImageResult.Ready(uris.filterNotNull())
                ClipboardImageCheck.EMPTY -> ClipboardImageResult.Unavailable(
                    "The clipboard has no image URI. Use Add photos or Android Share."
                )
                ClipboardImageCheck.NOT_IMAGE -> ClipboardImageResult.Unavailable(
                    "Clipboard contains text or another non-image type. Use Add photos."
                )
                ClipboardImageCheck.NON_CONTENT_URI -> ClipboardImageResult.Unavailable(
                    "Clipboard image is not an accessible Android content URI. Use Add photos."
                )
                ClipboardImageCheck.TOO_MANY -> ClipboardImageResult.Unavailable(
                    "Clipboard contains over 100 items. Select a smaller batch with Add photos."
                )
            }
        } catch (_: RuntimeException) {
            ClipboardImageResult.Unavailable(
                "Android denied access to the clipboard image. Use Add photos or Share again."
            )
        }
    }
}
