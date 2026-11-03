package io.github.gamesbyian.glimpse

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gamesbyian.glimpse.data.CollectionRecord
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.PrivatePublicationReceiptJournal
import io.github.gamesbyian.glimpse.debug.ManualRecoveryProbeActivity
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android accessibility actions through actual Compose and actual Room.
 * No fake confirmation state, simulated button callbacks or owner photos.
 * All fixtures are generated inside the disposable emulator's private app.
 */
@RunWith(AndroidJUnit4::class)
class ManualRecoveryPanelUiDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation

    private fun allNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        fun descend(current: AccessibilityNodeInfo) {
            found += current
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { descend(it) }
            }
        }
        descend(node)
        return found
    }

    private fun findClickable(label: String): AccessibilityNodeInfo? {
        val root = automation.rootInActiveWindow ?: return null
        // Compose frequently merges Text semantics into its button parent;
        // the provider search-by-text index is not guaranteed to include
        // the merged clickable node. Traverse actual accessibility nodes
        // instead of relying on a mock Compose callback or tap coordinates.
        for (candidate in allNodes(root)) {
            val text = candidate.text?.toString()?.trim()
            val description = candidate.contentDescription?.toString()?.trim()
            if (text != label && description != label) continue
            var current: AccessibilityNodeInfo? = candidate
            while (current != null) {
                if (current.isClickable && current.isEnabled) return current
                current = current.parent
            }
        }
        return null
    }

    private fun safeWindowSummary(): String {
        val root = automation.rootInActiveWindow ?: return "root=unavailable"
        val window = root.packageName?.toString().orEmpty().take(70)
        val nodes = allNodes(root)
        val matches = nodes.take(60).mapNotNull { node ->
            val words = node.text?.toString()?.trim().orEmpty()
            val description = node.contentDescription?.toString()?.trim().orEmpty()
            val shown = (words.ifEmpty { description }).take(66)
            if (shown.isEmpty()) null else
                (if (node.isClickable) "[clickable] " else "") + shown
        }
        // The fixture contains generated pixels only; these are capped
        // semantic labels, never screen bitmaps, photo paths or receipt IDs.
        return "window=$window nodes=${nodes.size} labels=${matches.joinToString(" | ").take(1100)}"
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child)
            if (found != null) return found
        }
        return null
    }

    private fun tapAccessible(label: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val display = context.resources.displayMetrics
        val viewport = Rect(0, 0, display.widthPixels, display.heightPixels)
        val deadline = SystemClock.elapsedRealtime() + 25_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val click = findClickable(label)
            if (click != null) {
                val bounds = Rect()
                click.getBoundsInScreen(bounds)
                if (Rect.intersects(bounds, viewport) &&
                    click.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) return
            }
            val scrollable = automation.rootInActiveWindow?.let { findScrollable(it) }
            scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(250L)
        }
        throw AssertionError(
            "Real accessible action unavailable: $label; ${safeWindowSummary()}"
        )
    }

    private fun awaitText(text: String) {
        val deadline = SystemClock.elapsedRealtime() + 25_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val root = automation.rootInActiveWindow
            if (root != null && root.findAccessibilityNodeInfosByText(text)
                    ?.any { it.text?.toString()?.contains(text) == true } == true
            ) return
            SystemClock.sleep(200L)
        }
        throw AssertionError("Actual Android accessibility text missing: $text")
    }

    @Test fun compactTwoHundredPercentConsentCancelThenConfirmPreservesAllBytes() = runBlocking {
        // This debug-only acceptance must never create synthetic records on
        // the owner's real handset, regardless of instrumentation settings.
        assertTrue(
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true)
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuredWidthDp = context.resources.displayMetrics.widthPixels /
            context.resources.displayMetrics.density
        assertTrue("Real UI must run at 320dp, not just scaled pixels",
            configuredWidthDp in 300f..340f)
        assertTrue("Real system text scale must be 200%, not simulated",
            context.resources.configuration.fontScale >= 1.95f)
        val originals = File(context.filesDir, "originals")
        assertTrue(originals.isDirectory || originals.mkdirs())
        val stage = File.createTempFile("import-", ".partial", originals)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb((System.nanoTime() and 255).toInt(), 41, 73))
            FileOutputStream(stage).use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.fd.sync()
            }
        } finally {
            bitmap.recycle()
        }
        val bytes = stage.readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val target = File(originals, "$sha.png")
        assertFalse("Fixture must not reuse an existing original", target.exists())
        val collection = "recovery-consent-test-${UUID.randomUUID()}"
        val dao = GlimpseDatabase.open(context).dao()
        dao.insertCollection(CollectionRecord(collection, "Synthetic recovery collection", 1L))
        val receipt = PrivatePublicationReceiptJournal(originals).begin(
            stage, target, sha, bytes.size.toLong(), collection
        )

        val activity = instrumentation.startActivitySync(
            Intent(context, ManualRecoveryProbeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        assertNotNull(activity)
        try {
            tapAccessible("Review retained photos")
            awaitText("Retained photo 1 of 1")
            awaitText("Destination collection: Synthetic recovery collection")
            tapAccessible("Review restoring this photo")
            awaitText("Restore this one photo?")
            tapAccessible("Cancel")

            // Actual cancel must not run any writer or Room attach.
            assertFalse(target.exists())
            assertArrayEquals(bytes, stage.readBytes())
            assertTrue(receipt.file.exists())
            assertNull(dao.findPhoto(sha))
            assertNull(dao.findMembership(collection, sha))

            tapAccessible("Review restoring this photo")
            awaitText("Restore this one photo?")
            tapAccessible("Restore this photo")
            awaitText("One photo was added to its collection")
            assertArrayEquals(bytes, target.readBytes())
            assertArrayEquals(bytes, stage.readBytes())
            assertTrue(receipt.file.exists())
            assertEquals(1, dao.membershipCount(sha))
            assertEquals(collection, dao.findMembership(collection, sha)?.collectionId)
            assertNull(dao.readRotation())
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
