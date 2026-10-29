package io.github.gamesbyian.glimpse

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.domain.QuickTileAvailability
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
import io.github.gamesbyian.glimpse.domain.quickTileAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-tapped Quick Settings action for the saved Home shuffle source.
 *
 * TileService is short-lived. Enqueue a unique one-shot OS-controlled request
 * instead of running a potentially slow WallpaperManager call from onClick.
 * A queued request is not a confirmed wallpaper change. The existing command
 * gateway remains the only owner of wallpaper and shuffle mutations.
 */
class QuickHomeTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { refreshFromSavedSource() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    GlimpseDatabase.open(applicationContext).dao().readRotation()
                }
                when (quickTileAvailability(record)) {
                    QuickTileAvailability.UNCERTAIN ->
                        paint(Tile.STATE_UNAVAILABLE, "Review uncertain result in Glimpse")
                    QuickTileAvailability.NO_SOURCE ->
                        paint(Tile.STATE_UNAVAILABLE, "Choose a shuffle collection in Glimpse")
                    QuickTileAvailability.READY -> {
                        val request = OneTimeWorkRequestBuilder<QuickHomeTileWorker>().build()
                        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                            UNIQUE_WORK, ExistingWorkPolicy.KEEP, request
                        )
                        paint(Tile.STATE_ACTIVE, "Requested. Check Glimpse for the result")
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                paint(Tile.STATE_UNAVAILABLE, "Could not queue. Open Glimpse")
            }
        }
    }

    private suspend fun refreshFromSavedSource() {
        try {
            val record = withContext(Dispatchers.IO) {
                GlimpseDatabase.open(applicationContext).dao().readRotation()
            }
            when (quickTileAvailability(record)) {
                QuickTileAvailability.NO_SOURCE ->
                    paint(Tile.STATE_UNAVAILABLE, "Choose a collection in Glimpse")
                QuickTileAvailability.UNCERTAIN ->
                    paint(Tile.STATE_UNAVAILABLE, "Review uncertain result in Glimpse")
                QuickTileAvailability.READY ->
                    paint(Tile.STATE_ACTIVE, "Request next Home wallpaper")
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            paint(Tile.STATE_UNAVAILABLE, "Open Glimpse to check status")
        }
    }

    private fun paint(status: Int, description: String) {
        qsTile?.let { tile ->
            tile.state = status
            tile.label = "Next Home photo"
            tile.subtitle = description
            tile.contentDescription = "Glimpse. " + description
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        internal const val UNIQUE_WORK = "glimpse-user-requested-home-tile-change"
    }
}

/**
 * WorkManager may delay even this user-triggered request. It does not retry a
 * failed Android side effect, consume a pending operation or invent success.
 * The durable rotation record in Room is the only outcome ledger.
 */
class QuickHomeTileWorker(
    context: Context, params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val dao = GlimpseDatabase.open(applicationContext).dao()
            if (quickTileAvailability(dao.readRotation()) != QuickTileAvailability.READY) {
                return Result.failure()
            }
            WallpaperCommands(applicationContext, dao).changeNow()
            Result.success()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Only the user may resubmit after a failure. No automatic replay.
            Result.failure()
        }
    }
}
