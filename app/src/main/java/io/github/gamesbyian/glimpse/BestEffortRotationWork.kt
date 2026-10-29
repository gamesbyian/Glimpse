package io.github.gamesbyian.glimpse

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Explicit user opt-in. This is OS-deferrable, not a five-minute background
 * clock. Visible-app rotation still checks eligibility every five minutes.
 * Android may postpone periodic jobs for hours during Doze/OEM restrictions.
 *
 * WorkManager records scheduling across ordinary process exits and reboots.
 * This worker shares the SAME serialized wallpaper operation mutex with the
 * foreground ViewModel via WallpaperCommands. Never invent a second writer.
 */
internal object BestEffortRotationWork {
    const val PERIOD_MINUTES = 15L // Android PeriodicWorkRequest hard minimum.
    private const val UNIQUE_NAME = "glimpse-opt-in-home-wallpaper-check"
    private const val PREFERENCES = "glimpse_background_rotation"
    private const val ENABLED = "explicit_user_opt_in"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(ENABLED, false)

    /** Persist the consent BEFORE enqueue, and clear it BEFORE cancellation. */
    fun setConsent(context: Context, allowed: Boolean) {
        val applied = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, allowed).commit()
        check(applied) { "Unable to save the requested background preference." }
    }

    fun schedule(context: Context) {
        if (!enabled(context)) return
        val request = PeriodicWorkRequestBuilder<BestEffortRotationWorker>(
            PERIOD_MINUTES, TimeUnit.MINUTES
        ).addTag(UNIQUE_NAME).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}

/** No source URI, photo bytes, IDs or collection names are passed as work input. */
class BestEffortRotationWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        if (!BestEffortRotationWork.enabled(applicationContext)) return Result.success()
        return try {
            val dao = GlimpseDatabase.open(applicationContext).dao()
            // Recheck explicit consent under the same process-wide mutex as
            // foreground operations and respect paused/pending/next-due gates.
            WallpaperCommands(applicationContext, dao).pollFromBackground()
            Result.success()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // WorkManager controls retry and its system quotas; don't spin
            // application-side catch-up loops or claim wallpaper success.
            Result.retry()
        }
    }
}
