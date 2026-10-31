package io.github.gamesbyian.glimpse

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.GlimpseDao
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PhotoRecord
import io.github.gamesbyian.glimpse.data.RotationRecord
import io.github.gamesbyian.glimpse.data.verifyPrivateOriginal
import io.github.gamesbyian.glimpse.domain.WallpaperCommands
import io.github.gamesbyian.glimpse.domain.WidgetPreviewKind
import io.github.gamesbyian.glimpse.domain.widgetPreviewDecision
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import java.io.File

/**
 * The system hosts this read-only Home widget. No arbitrary external action
 * or photo URI is accepted from its exported AppWidgetProvider receiver.
 * Image decoding and Room access run in a separate short WorkManager job.
 */
class GlimpseHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        GlimpseHomeWidgetRefresh.enqueue(context)
    }

    override fun onEnabled(context: Context) {
        GlimpseHomeWidgetRefresh.enqueue(context)
    }

    override fun onRestored(
        context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray
    ) {
        GlimpseHomeWidgetRefresh.enqueue(context)
    }
}

/**
 * Private (exported=false) receiver accepts only PendingIntents minted by our
 * own widget UI. The OS widget host invokes them under our application UID.
 * Reuses exactly the same unique serialized one-shot worker as Quick Settings.
 */
class GlimpseWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!GlimpseHomeWidgetRefresh.hasWidgets(context)) return
        when (intent.action) {
            ACTION_CHANGE_HOME -> {
                val request = OneTimeWorkRequestBuilder<QuickHomeTileWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    QuickHomeTileService.UNIQUE_WORK, ExistingWorkPolicy.KEEP, request
                )
            }
            ACTION_SKIP_PREVIEW -> {
                // Capture the *displayed* candidate at tap time. Do not allow
                // WorkManager delay or a collection change to skip another photo.
                val collection = intent.getStringExtra(EXTRA_SKIP_COLLECTION).orEmpty()
                val expected = intent.getStringExtra(EXTRA_SKIP_HEAD).orEmpty()
                if (collection.isBlank() || !expected.matches(Regex("[0-9a-f]{64}"))) return
                val input = Data.Builder()
                    .putString(EXTRA_SKIP_COLLECTION, collection)
                    .putString(EXTRA_SKIP_HEAD, expected)
                    .build()
                val request = OneTimeWorkRequestBuilder<GlimpseWidgetSkipWorker>()
                    .setInputData(input).build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "glimpse-widget-user-skip-preview", ExistingWorkPolicy.KEEP, request
                )
            }
        }
    }

    companion object {
        const val ACTION_CHANGE_HOME =
            "io.github.gamesbyian.glimpse.action.USER_WIDGET_NEXT_HOME"
        const val ACTION_SKIP_PREVIEW =
            "io.github.gamesbyian.glimpse.action.USER_WIDGET_SKIP_PREVIEW"
        const val EXTRA_SKIP_COLLECTION = "glimpse.widget.skip.collection"
        const val EXTRA_SKIP_HEAD = "glimpse.widget.skip.expected_head"
    }
}

/**
 * Only a user tap through the private widget action receiver can request
 * a skip. Use the same serialized Room/WallpaperCommands state gateway.
 * The gateway never invokes WallpaperManager for this operation.
 */
class GlimpseWidgetSkipWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val dao = GlimpseDatabase.open(applicationContext).dao()
        val collection = inputData.getString(
            GlimpseWidgetActionReceiver.EXTRA_SKIP_COLLECTION
        ).orEmpty()
        val expected = inputData.getString(
            GlimpseWidgetActionReceiver.EXTRA_SKIP_HEAD
        ).orEmpty()
        if (collection.isBlank() || !expected.matches(Regex("[0-9a-f]{64}"))) {
            Result.failure()
        } else {
            WallpaperCommands(applicationContext, dao).skipNextPreview(collection, expected)
            // Even a stale tap should replace the outdated widget preview.
            GlimpseHomeWidgetRefresh.enqueue(applicationContext)
            Result.success()
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        Result.failure()
    }
}

/**
 * Widget update requests are always best-effort. The normal Android widget
 * updatePeriodMillis is 30 minutes minimum, not a five-minute timer.
 * This worker only reads saved state and publishes bounded RemoteViews.
 */
internal object GlimpseHomeWidgetRefresh {
    private const val UNIQUE_REFRESH = "glimpse-home-widget-read-only-refresh"

    fun ids(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, GlimpseHomeWidgetProvider::class.java)
        )

    fun hasWidgets(context: Context): Boolean = ids(context).isNotEmpty()

    fun enqueue(context: Context) {
        if (!hasWidgets(context)) return
        val request = OneTimeWorkRequestBuilder<GlimpseHomeWidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_REFRESH, ExistingWorkPolicy.REPLACE, request
        )
    }
}

/**
 * The "next" picture is only predictable if it already sits in the unused
 * saved bag. A fresh shuffle is random and must NOT be falsely previewed as
 * decided. Never read source URIs or transmit private paths to the launcher.
 */
class GlimpseHomeWidgetRefreshWorker(
    context: Context, params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val widgetIds = GlimpseHomeWidgetRefresh.ids(context)
            if (widgetIds.isEmpty()) return Result.success()

            val dao = GlimpseDatabase.open(context).dao()
            val rotation = dao.readRotation()
            val collection = rotation?.collectionId?.let { dao.findCollection(it) }
            val liveCount = rotation?.collectionId?.takeIf { collection != null }
                ?.let { dao.widgetSourcePhotoCount(it) } ?: 0
            val hasPhotos = liveCount > 0
            val solePhotoId = if (liveCount == 1 && rotation?.collectionId != null)
                dao.widgetSoleSourcePhotoId(rotation.collectionId) else null
            val nextCandidates = if (rotation != null && collection != null && hasPhotos &&
                rotation.pendingPhotoId == null) {
                knownNextCandidates(dao, rotation)
            } else emptyList()
            val nextPhoto = nextCandidates.firstOrNull()
            val decision = widgetPreviewDecision(
                rotation, collection != null && hasPhotos, nextPhoto?.id,
                soleEligiblePhotoId = solePhotoId,
                knownValidQueueSecond = nextCandidates.getOrNull(1)?.id
            )

            val verifiedThumb = if (decision.kind == WidgetPreviewKind.NEXT_KNOWN &&
                nextPhoto != null) {
                runCatching {
                    val privateFile = verifyPrivateOriginal(
                        File(context.filesDir, "originals"), nextPhoto
                    )
                    BoundedPhotoDecode.decode(privateFile, maxEdge = 256)
                }.getOrNull()
            } else null

            // Skip is offered only beside a SHA-verified visible next image.
            // A registered but damaged original must not create a blind skip.
            val skipVisible = decision.canSkipNext && verifiedThumb != null &&
                !rotation?.collectionId.isNullOrBlank() &&
                !decision.knownNextId.isNullOrBlank()
            try {
                val manager = AppWidgetManager.getInstance(context)
                for (widgetId in widgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.glimpse_home_widget)
                    views.setOnClickPendingIntent(
                        R.id.glimpse_widget_action,
                        if (decision.canRequestChange) nextHomeIntent(context)
                        else openAppIntent(context)
                    )
                    views.setOnClickPendingIntent(
                        R.id.glimpse_widget_root, openAppIntent(context)
                    )
                    views.setViewVisibility(
                        R.id.glimpse_widget_skip,
                        if (skipVisible) View.VISIBLE else View.GONE
                    )
                    if (skipVisible) {
                        views.setOnClickPendingIntent(
                            R.id.glimpse_widget_skip,
                            skipPreviewIntent(context, rotation!!.collectionId!!,
                                decision.knownNextId!!)
                        )
                    }
                    views.setContentDescription(
                        R.id.glimpse_widget_skip,
                        "Skip the confirmed upcoming photo in the saved shuffle without changing Home wallpaper"
                    )
                    // The XML's static description describes the empty state.
                    // Keep TalkBack aligned with the *actual* tap target:
                    // a requested wallpaper change is not an acknowledged one.
                    views.setContentDescription(
                        R.id.glimpse_widget_action,
                        if (decision.canRequestChange)
                            "Request a Home wallpaper change. Android may delay or decline it."
                        else "Open Glimpse to configure or review Home wallpaper."
                    )
                    views.setImageViewResource(
                        R.id.glimpse_widget_preview, R.drawable.glimpse_tile_next
                    )
                    views.setTextViewText(
                        R.id.glimpse_widget_collection,
                        collection?.title ?: "Create or select a collection"
                    )
                    when (decision.kind) {
                        WidgetPreviewKind.NO_SOURCE -> {
                            views.setTextViewText(R.id.glimpse_widget_status, "Choose a collection")
                            views.setTextViewText(R.id.glimpse_widget_action, "OPEN GLIMPSE")
                            views.setTextViewText(R.id.glimpse_widget_explainer,
                                "A saved collection is required before changing Home.")
                        }
                        WidgetPreviewKind.UNCERTAIN -> {
                            views.setTextViewText(R.id.glimpse_widget_status, "Result uncertain")
                            views.setTextViewText(R.id.glimpse_widget_action, "REVIEW IN APP")
                            views.setTextViewText(R.id.glimpse_widget_explainer,
                                "Review the previous Android result. No automatic retry.")
                        }
                        WidgetPreviewKind.CURRENT_ONLY -> {
                            views.setTextViewText(R.id.glimpse_widget_status,
                                "Only photo already applied")
                            views.setTextViewText(R.id.glimpse_widget_action, "OPEN GLIMPSE")
                            views.setTextViewText(R.id.glimpse_widget_explainer,
                                "Import another photo to change Home without a repeat.")
                        }
                        WidgetPreviewKind.NEXT_KNOWN -> {
                            views.setTextViewText(R.id.glimpse_widget_status,
                                if (verifiedThumb != null) "Next in the bag"
                                else "Next saved photo")
                            views.setTextViewText(R.id.glimpse_widget_action, "CHANGE HOME")
                            views.setTextViewText(R.id.glimpse_widget_explainer,
                                "Saved order only. Android may delay a manual request.")
                        }
                        WidgetPreviewKind.NEXT_UNDECIDED -> {
                            views.setTextViewText(R.id.glimpse_widget_status,
                                "Next photo unconfirmed")
                            views.setTextViewText(R.id.glimpse_widget_action, "CHANGE HOME")
                            views.setTextViewText(R.id.glimpse_widget_explainer,
                                "The exact next photo is not confirmed in this preview.")
                        }
                    }
                    if (skipVisible) {
                        views.setTextViewText(R.id.glimpse_widget_action, "APPLY HOME")
                    }
                    if (verifiedThumb != null) {
                        views.setImageViewBitmap(R.id.glimpse_widget_preview, verifiedThumb)
                        views.setContentDescription(R.id.glimpse_widget_preview,
                            "Preview of next verified image from the saved shuffle bag")
                    } else {
                        views.setContentDescription(R.id.glimpse_widget_preview,
                            "No verified next-image preview available")
                    }
                    manager.updateAppWidget(widgetId, views)
                }
            } finally {
                verifiedThumb?.recycle()
            }
            Result.success()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // A widget is decorative; never interfere with the wallpaper
            // gateway or claim the Home image changed because of UI failure.
            Result.failure()
        }
    }

    private suspend fun knownNextCandidates(
        dao: GlimpseDao, rotation: RotationRecord
    ): List<PhotoRecord> {
        val source = rotation.collectionId ?: return emptyList()
        // Read-only, bounded lookup; never draw or reseed a random queue.
        if (rotation.remainingJson.length > 1_000_000) return emptyList()
        val entries = runCatching { JSONArray(rotation.remainingJson) }.getOrNull()
            ?: return emptyList()
        val found = mutableListOf<PhotoRecord>()
        val seen = mutableSetOf<String>()
        for (index in 0 until minOf(entries.length(), 32)) {
            val id = entries.optString(index)
            if (id.isBlank() || !seen.add(id)) continue
            if (dao.findMembership(source, id) == null) continue
            val photo = dao.findPhoto(id) ?: continue
            found.add(photo)
            if (found.size == 2) break
        }
        return found
    }

    private fun nextHomeIntent(context: Context): PendingIntent {
        val intent = Intent(context, GlimpseWidgetActionReceiver::class.java).apply {
            action = GlimpseWidgetActionReceiver.ACTION_CHANGE_HOME
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun skipPreviewIntent(
        context: Context, collectionId: String, expectedPhotoId: String
    ): PendingIntent {
        val intent = Intent(context, GlimpseWidgetActionReceiver::class.java).apply {
            action = GlimpseWidgetActionReceiver.ACTION_SKIP_PREVIEW
            putExtra(GlimpseWidgetActionReceiver.EXTRA_SKIP_COLLECTION, collectionId)
            putExtra(GlimpseWidgetActionReceiver.EXTRA_SKIP_HEAD, expectedPhotoId)
            // PendingIntent identity ignores extras. Distinguish previews by
            // their identity so FLAG_UPDATE_CURRENT cannot silently retarget
            // an old launcher tap to an entirely different visible photo.
            setIdentifier("widget-skip:$collectionId:$expectedPhotoId")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(context, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
