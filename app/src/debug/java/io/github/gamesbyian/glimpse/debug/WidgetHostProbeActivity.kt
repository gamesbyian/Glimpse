package io.github.gamesbyian.glimpse.debug

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import io.github.gamesbyian.glimpse.GlimpseHomeWidgetProvider
import io.github.gamesbyian.glimpse.GlimpseHomeWidgetRefresh
import java.io.File

/**
 * Debug-only real AppWidgetHost on a disposable emulator.
 *
 * Uses Android's documented ACTION_APPWIDGET_BIND user confirmation when
 * system permission isn't already present. Never requests the privileged
 * signature BIND_APPWIDGET permission or ships this host in release builds.
 */
class WidgetHostProbeActivity : ComponentActivity() {
    private lateinit var widgetHost: AppWidgetHost
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(Build.HARDWARE.contains("ranchu", true) ||
            Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.contains("generic", true) ||
            Build.MODEL.contains("sdk_gphone", true)) {
            "Widget probe must never run on the owner's physical phone"
        }

        // Debug test synchronization must be specific to this launch.
        // Android force-stop can bypass onDestroy and leave stale READY.
        File(filesDir, "widget-probe-ready.txt").delete()
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, GlimpseHomeWidgetProvider::class.java)
        widgetHost = AppWidgetHost(this, 19405)

        // Wallpaper or launcher configuration changes can recreate this
        // debug-only Activity without a useful saved-instance Bundle.
        // Query IDs owned by *this* AppWidgetHost before ever deleting it;
        // otherwise an existing bound widget is discarded and another
        // Settings CREATE permission dialog appears instead of its result.
        val savedId = savedInstanceState
            ?.getInt(SAVED_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val alreadyBound = widgetHost.appWidgetIds.filter { id ->
            manager.getAppWidgetInfo(id)?.provider == provider
        }
        val reused = (listOf(savedId) + alreadyBound)
            .firstOrNull { id ->
                id in alreadyBound && id != AppWidgetManager.INVALID_APPWIDGET_ID
            }
        if (reused != null) {
            widgetId = reused
            showActualWidget()
            return
        }

        // Only a genuinely fresh test-host install is allowed to request a
        // new Android widget binding. No privileged grant or fake launcher.
        widgetHost.deleteHost()
        widgetId = widgetHost.allocateAppWidgetId()

        if (manager.bindAppWidgetIdIfAllowed(widgetId, provider)) {
            showActualWidget()
        } else {
            val request = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            try {
                startActivityForResult(request, REQUEST_BIND_WIDGET)
            } catch (failure: Exception) {
                File(filesDir, "widget-probe-ready.txt").writeText(
                    "ERROR:Android could not launch the actual widget-binding consent screen"
                )
            }
        }
    }

    @Deprecated("System AppWidget bind UI returns through the legacy activity callback.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_BIND_WIDGET) return
        if (resultCode == Activity.RESULT_OK) {
            showActualWidget()
        } else {
            File(filesDir, "widget-probe-ready.txt").writeText(
                "ERROR:Android AppWidget binding was not approved"
            )
        }
    }

    private fun showActualWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val info = requireNotNull(manager.getAppWidgetInfo(widgetId)) {
            "Actual platform widget info was not returned"
        }
        val widgetView = widgetHost.createView(this, widgetId, info)
        val container = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(32, 56, 53))
            val px = (16 * resources.displayMetrics.density).toInt()
            setPadding(px, px, px, px)
        }
        val height = (268 * resources.displayMetrics.density).toInt()
        container.addView(widgetView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, height, Gravity.CENTER
        ))
        setContentView(container)
        widgetHost.startListening()
        GlimpseHomeWidgetRefresh.enqueue(this)
        File(filesDir, "widget-probe-ready.txt").writeText("READY:" + widgetId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SAVED_WIDGET_ID, widgetId)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        runCatching { widgetHost.stopListening() }
        if (!isChangingConfigurations) {
            runCatching { widgetHost.deleteHost() }
            File(filesDir, "widget-probe-ready.txt").delete()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_BIND_WIDGET = 19405
        private const val SAVED_WIDGET_ID = "glimpse_debug_host_widget_id"
    }
}
