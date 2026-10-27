package io.github.gamesbyian.glimpse

import android.content.Intent
import android.net.Uri

/**
 * Incoming Android Sharesheet grants are transient and user supplied.
 * No provider URI is written to Room, preferences, SavedState, diagnostics or logs.
 * Actual image content is validated again by the existing bounded PhotoImporter.
 */
internal sealed interface IncomingPhotoShare {
    data class Ready(val uris: List<Uri>) : IncomingPhotoShare
    data class Rejected(val message: String) : IncomingPhotoShare
}

internal enum class IncomingShareCheck {
    NOT_SHARE, READY, UNSUPPORTED_TYPE, MISSING_IMAGES, INVALID_URI, TOO_MANY
}

internal fun validateIncomingShare(
    action: String?,
    mime: String?,
    schemes: List<String?>
): IncomingShareCheck {
    if (action != "android.intent.action.SEND" &&
        action != "android.intent.action.SEND_MULTIPLE"
    ) return IncomingShareCheck.NOT_SHARE
    if (mime == null || !mime.startsWith("image/", ignoreCase = true) ||
        mime.length <= "image/".length
    ) return IncomingShareCheck.UNSUPPORTED_TYPE
    if (schemes.isEmpty()) return IncomingShareCheck.MISSING_IMAGES
    if (schemes.size > 100) return IncomingShareCheck.TOO_MANY
    if (schemes.any { it != "content" }) return IncomingShareCheck.INVALID_URI
    return IncomingShareCheck.READY
}

internal object IncomingPhotoShareReceiver {
    fun parse(intent: Intent?): IncomingPhotoShare? {
        if (intent == null ||
            (intent.action != Intent.ACTION_SEND &&
                intent.action != Intent.ACTION_SEND_MULTIPLE)
        ) return null
        val uris = try {
            val fromExtras: List<Uri> = when (intent.action) {
                Intent.ACTION_SEND -> listOfNotNull(singleStream(intent))
                else -> multipleStreams(intent)
            }
            val clip = intent.clipData
            val fromClip = if (clip == null) emptyList() else
                (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
            (fromExtras + fromClip).distinct()
        } catch (_: RuntimeException) {
            return IncomingPhotoShare.Rejected(
                "The sending app supplied an unreadable image selection. Share it again or use Add photos."
            )
        }
        return when (validateIncomingShare(intent.action, intent.type, uris.map { it.scheme })) {
            IncomingShareCheck.NOT_SHARE -> null
            IncomingShareCheck.READY -> IncomingPhotoShare.Ready(uris)
            IncomingShareCheck.UNSUPPORTED_TYPE -> IncomingPhotoShare.Rejected(
                "Only shared images are accepted. Use Add photos for other sources."
            )
            IncomingShareCheck.MISSING_IMAGES -> IncomingPhotoShare.Rejected(
                "No shared image was provided. Try Share again or choose Add photos."
            )
            IncomingShareCheck.INVALID_URI -> IncomingPhotoShare.Rejected(
                "Only Android content-provider images are supported. Share through an app or use Add photos."
            )
            IncomingShareCheck.TOO_MANY -> IncomingPhotoShare.Rejected(
                "Share at most 100 images at once. No images were imported."
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun singleStream(intent: Intent): Uri? =
        intent.getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun multipleStreams(intent: Intent): List<Uri> =
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
}
