package io.github.gamesbyian.glimpse.domain

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import io.github.gamesbyian.glimpse.data.GlimpseDao
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest

/**
 * Single serialized side-effect gateway. Browsing never calls this class.
 * All automatic work happens only while the app is STARTED in the foreground.
 *
 * A persisted pending photo means a process died or the OS failed to confirm
 * the result. Never retry that photo automatically or advance the queue.
 */
class WallpaperCommands(
    private val context: Context,
    private val dao: GlimpseDao,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val intervalMillis = 5 * 60 * 1000L

    suspend fun start(collectionId: String): String = mutex.withLock {
        val photos = dao.photosIn(collectionId)
        if (photos.isEmpty()) return@withLock "Import at least one photo first."
        val previous = dao.readRotation() ?: RotationRecord()
        if (previous.pendingPhotoId != null) {
            return@withLock "An earlier wallpaper operation has an uncertain outcome. Review it first."
        }
        dao.saveRotation(previous.copy(
            collectionId = collectionId,
            active = true,
            remainingJson = "[]",
            nextEligibleAt = now() + intervalMillis,
            outcome = "Rotation enabled while Glimpse is in use. Next eligible in about five minutes."
        ))
        "Rotation enabled for Home wallpaper. Android sleep is not scheduled."
    }

    suspend fun pause(): String = mutex.withLock {
        val previous = dao.readRotation() ?: RotationRecord()
        dao.saveRotation(previous.copy(active = false, outcome = "Rotation paused"))
        "Rotation paused."
    }

    suspend fun acknowledgeUncertain(): String = mutex.withLock {
        val previous = dao.readRotation() ?: return@withLock "Nothing to review."
        if (previous.pendingPhotoId == null) return@withLock "No unresolved attempt."
        dao.saveRotation(previous.copy(
            pendingPhotoId = null,
            active = false,
            outcome = "Prior attempt outcome uncertain; automatic rotation paused. Inspect your wallpaper."
        ))
        "Uncertain attempt dismissed; rotation remains paused."
    }

    suspend fun applySelected(photoId: String): String = mutex.withLock {
        val photo = dao.findPhoto(photoId) ?: return@withLock "Selected photo is unavailable."
        val previous = dao.readRotation() ?: RotationRecord()
        if (previous.pendingPhotoId != null) return@withLock "Resolve the uncertain attempt first."
        applyLocked(photo, previous, null)
    }

    suspend fun changeNow(): String = mutex.withLock {
        val previous = dao.readRotation() ?: return@withLock "Start a collection first."
        if (previous.pendingPhotoId != null) return@withLock "Resolve the uncertain attempt first."
        val collectionId = previous.collectionId ?: return@withLock "Select a rotation collection first."
        performNextLocked(previous, collectionId)
    }

    suspend fun pollWhileVisible() = mutex.withLock {
        val previous = dao.readRotation() ?: return@withLock
        if (!previous.active || previous.pendingPhotoId != null) return@withLock
        if (now() < (previous.nextEligibleAt ?: Long.MAX_VALUE)) return@withLock
        val collectionId = previous.collectionId ?: return@withLock
        performNextLocked(previous, collectionId)
    }

    private suspend fun performNextLocked(
        previous: RotationRecord,
        collectionId: String
    ): String {
        val photos = dao.photosIn(collectionId)
        if (photos.isEmpty()) {
            dao.saveRotation(previous.copy(active = false, outcome = "No eligible photos; rotation paused"))
            return "No photos in rotation collection."
        }
        if (photos.size == 1 && photos[0].id == previous.lastAcknowledgedPhotoId) {
            dao.saveRotation(previous.copy(
                nextEligibleAt = now() + intervalMillis,
                outcome = "One photo already applied; skipped unnecessary repeat"
            ))
            return "Only one eligible photo; nothing to change."
        }
        val bag = ShuffleBag.replenish(
            photos.map { it.id },
            decodeBag(previous.remainingJson),
            previous.lastAcknowledgedPhotoId
        )
        val candidate = photos.firstOrNull { it.id == bag.firstOrNull() }
            ?: return "No eligible photo could be selected."
        return applyLocked(candidate, previous, bag.drop(1))
    }

    private suspend fun applyLocked(
        photo: PhotoRecord,
        previous: RotationRecord,
        remainingAfterSuccess: List<String>?
    ): String {
        val started = now()
        dao.saveRotation(previous.copy(
            pendingPhotoId = photo.id,
            lastAttemptAt = started,
            outcome = "Attempting to apply Home wallpaper"
        ))
        return try {
            val wallpaperId = withContext(Dispatchers.IO) {
                val bitmap = decodeForWallpaper(photo)
                try {
                    WallpaperManager.getInstance(context).setBitmap(
                        bitmap, null, true, WallpaperManager.FLAG_SYSTEM
                    )
                } finally {
                    bitmap.recycle()
                }
            }
            if (wallpaperId <= 0) {
                // Some systems cannot provide an authoritative result. Never
                // fabricate acknowledgment or advance the rotation bag.
                dao.saveRotation(previous.copy(
                    pendingPhotoId = photo.id,
                    lastAttemptAt = started,
                    active = false,
                    outcome = "Wallpaper API returned no confirmed ID; outcome uncertain"
                ))
                "Wallpaper outcome uncertain; automatic rotation paused."
            } else {
                dao.saveRotation(previous.copy(
                    pendingPhotoId = null,
                    lastAttemptAt = started,
                    lastAcknowledgedPhotoId = photo.id,
                    lastAcknowledgedAt = now(),
                    nextEligibleAt = if (previous.active) now() + intervalMillis else previous.nextEligibleAt,
                    remainingJson = remainingAfterSuccess?.let(::encodeBag) ?: previous.remainingJson,
                    outcome = "Android acknowledged Home wallpaper update (ID " + wallpaperId + ")"
                ))
                "Android acknowledged the Home wallpaper update."
            }
        } catch (failure: Exception) {
            dao.saveRotation(previous.copy(
                pendingPhotoId = null,
                lastAttemptAt = started,
                nextEligibleAt = if (previous.active) now() + intervalMillis else previous.nextEligibleAt,
                outcome = "Apply failed: " + failure.javaClass.simpleName
            ))
            "Wallpaper application failed. The rotation cursor has not advanced."
        }
    }

    private fun decodeForWallpaper(photo: PhotoRecord): android.graphics.Bitmap {
        // A decodable but corrupted private copy must never be applied under
        // a stable SHA-derived ImageAssetId. Validate the *present* source
        // bytes immediately before the irreversible Android OS side-effect.
        val local = File(photo.localPath)
        require(Files.isRegularFile(local.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Original offline file is missing or not a regular file"
        }
        require(photo.id == photo.sha256 && photo.byteLength in 1..(50L * 1024 * 1024) &&
                local.length() == photo.byteLength) {
            "Original offline file identity or length changed"
        }
        val digest = MessageDigest.getInstance("SHA-256")
        local.inputStream().use { stream ->
            val chunk = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(chunk)
                if (count < 0) break
                if (count > 0) digest.update(chunk, 0, count)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == photo.sha256) {
            "Original offline file SHA-256 mismatch"
        }
        val bound = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photo.localPath, bound)
        require(bound.outWidth > 0 && bound.outHeight > 0) { "Image missing or corrupt" }
        var sample = 1
        while (bound.outWidth / sample > 3200 || bound.outHeight / sample > 3200) {
            sample *= 2
        }
        return requireNotNull(
            BitmapFactory.decodeFile(photo.localPath, BitmapFactory.Options().apply {
                inSampleSize = sample
            })
        ) { "Unable to decode offline image" }
    }

    private fun decodeBag(raw: String): List<String> = try {
        val json = JSONArray(raw)
        (0 until json.length()).map { json.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun encodeBag(items: List<String>): String = JSONArray(items).toString()
}
