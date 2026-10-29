package io.github.gamesbyian.glimpse.domain

import android.app.WallpaperManager
import io.github.gamesbyian.glimpse.BestEffortRotationWork
import android.content.Context
import android.os.PowerManager
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.GlimpseDao
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import io.github.gamesbyian.glimpse.data.RotationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/** Both the UI and WorkManager worker are in the main app process. */
private object WallpaperProcessGate {
    val mutex = Mutex()
}

/**
 * Single serialized side-effect gateway. Browsing never calls this class.
 * Foreground checks and explicitly enabled best-effort OS work use this same
 * process-wide operation gate; no second bitmap writer exists.
 *
 * A persisted pending photo means a process died or the OS failed to confirm
 * the result. Never retry that photo automatically or advance the queue.
 */
class WallpaperCommands(
    private val context: Context,
    private val dao: GlimpseDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val batterySaverActive: () -> Boolean = {
        runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.isPowerSaveMode == true
        }.getOrDefault(false)
    }
) {
    private val mutex = WallpaperProcessGate.mutex
    private val intervalMillis = 5 * 60 * 1000L

    suspend fun start(collectionId: String): String = mutex.withLock {
        val photos = dao.photosIn(collectionId)
        if (photos.isEmpty()) return@withLock "Import at least one photo first."
        val previous = dao.readRotation() ?: RotationRecord()
        if (previous.pendingPhotoId != null) {
            return@withLock "An earlier wallpaper operation has an uncertain outcome. Review it first."
        }
        if (previous.active && previous.collectionId == collectionId) {
            return@withLock "Shuffle is already running for this collection."
        }
        val resumed = !previous.active && previous.collectionId == collectionId
        dao.saveRotation(prepareRotationStart(
            previous = previous, requestedCollectionId = collectionId,
            nowMillis = now(), intervalMillis = intervalMillis
        ))
        if (resumed)
            "Resumed the saved no-repeat shuffle for this collection. " +
                "Next eligible in about five minutes while Glimpse is visible."
        else
            "Started a new Home wallpaper shuffle for this collection. " +
                "Next eligible in about five minutes while Glimpse is visible."
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

    /** Runs only from Activity STARTED's existing five-minute loop. */
    suspend fun pollWhileVisible() = mutex.withLock { pollEligibleLocked() }

    /** OS-delayed check, independently gated by explicit user consent. */
    suspend fun pollFromBackground() = mutex.withLock {
        if (BestEffortRotationWork.enabled(context)) pollEligibleLocked()
    }

    /** Never replay missed intervals or consume a reserved candidate twice. */
    private suspend fun pollEligibleLocked() {
        val previous = dao.readRotation() ?: return
        val current = now()
        when (automaticRotationGate(previous, current, batterySaverActive())) {
            AutomaticRotationGate.PAUSED, AutomaticRotationGate.UNCERTAIN,
            AutomaticRotationGate.NOT_DUE -> return
            AutomaticRotationGate.BATTERY_SAVER -> {
                // Do not consume a candidate or replay missed time windows.
                dao.saveRotation(previous.copy(
                    nextEligibleAt = BatterySaverRotationPolicy.deferDue(
                        current, intervalMillis
                    ),
                    outcome = "Battery Saver: automatic rotation deferred; original queue preserved"
                ))
                return
            }
            AutomaticRotationGate.ELIGIBLE -> {
                val collectionId = previous.collectionId ?: return
                performNextLocked(previous, collectionId)
            }
        }
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
                nextEligibleAt = BatterySaverRotationPolicy.deferDue(now(), intervalMillis),
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
        dao.saveRotation(beginWallpaperAttempt(previous, photo.id, started))
        // Once WallpaperManager is entered, an exception or cancellation
        // cannot prove the prior image remains in place. The reducer retains
        // a durable pending token and refuses to consume the reserved bag.
        var systemCallStarted = false

        suspend fun persist(
            result: WallpaperAttemptResult,
            remaining: List<String>? = null
        ) {
            dao.saveRotation(resolveWallpaperAttempt(
                previous = previous,
                photoId = photo.id,
                startedAt = started,
                finishedAt = now(),
                intervalMillis = intervalMillis,
                remainingAfterSuccessJson = remaining?.let(::encodeBag),
                result = result
            ))
        }

        return try {
            val wallpaperId = withContext(Dispatchers.IO) {
                val bitmap = decodeForWallpaper(photo)
                try {
                    val manager = WallpaperManager.getInstance(context)
                    systemCallStarted = true
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } finally {
                    bitmap.recycle()
                }
            }
            if (wallpaperId <= 0) {
                persist(WallpaperAttemptResult.MissingAcknowledgment)
                "Wallpaper outcome uncertain; automatic rotation paused."
            } else {
                persist(WallpaperAttemptResult.Acknowledged(wallpaperId),
                    remainingAfterSuccess)
                "Android acknowledged the Home wallpaper update."
            }
        } catch (cancel: CancellationException) {
            // A cancelled coroutine can still persist the conservative gate.
            withContext(NonCancellable + Dispatchers.IO) {
                persist(WallpaperAttemptResult.Interrupted)
            }
            throw cancel
        } catch (failure: Exception) {
            if (systemCallStarted) {
                persist(WallpaperAttemptResult.PlatformError)
                "Wallpaper outcome uncertain; automatic rotation paused."
            } else {
                // Decode or manager lookup failed before entering the OS.
                // Retain the whole shuffle reservation, defer one interval.
                persist(WallpaperAttemptResult.BeforePlatformError(
                    failure.javaClass.simpleName
                ))
                "Wallpaper application failed before Android was asked to change it."
            }
        }
    }

    private fun decodeForWallpaper(photo: PhotoRecord): android.graphics.Bitmap {
        // Exact physical bytes are checked before invoking the OS side effect.
        // A corrupt or replaced original is a pre-OS failure, so the prior
        // wallpaper acknowledgment and no-repeat queue remain untouched.
        val original = verifyPrivateOriginal(File(context.filesDir, "originals"), photo)
        return BoundedPhotoDecode.decode(original, maxEdge = 3200)
    }

    private fun decodeBag(raw: String): List<String> = try {
        val json = JSONArray(raw)
        (0 until json.length()).map { json.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun encodeBag(items: List<String>): String = JSONArray(items).toString()
}
