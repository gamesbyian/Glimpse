package io.github.gamesbyian.glimpse.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gamesbyian.glimpse.debug.PublicationCooperationProbeService
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real two-process Android acceptance using the app's actual SHA publication
 * path and a uniquely owned disposable cache fixture. No synthetic lock mocks.
 */
@RunWith(AndroidJUnit4::class)
class CrossProcessPublicationDeviceTest {
    private lateinit var owned: File
    private val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
    private fun sha() = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        owned = File(context.cacheDir, "glimpse-publish-instrumentation-${UUID.randomUUID()}")
        assertTrue(owned.mkdir())
    }

    @After fun tearDown() {
        if (::owned.isInitialized &&
            owned.name.startsWith("glimpse-publish-instrumentation-") &&
            owned.canonicalFile.parentFile == owned.parentFile.canonicalFile
        ) {
            owned.deleteRecursively()
        }
    }

    private fun durableStage(name: String): File = File(owned, name).apply {
        FileOutputStream(this).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
    }

    @Test fun sameShaSecondProcessWaitsForRealFirstWriterThenDeduplicates() {
        val childStage = durableStage("import-cooperation-child.partial")
        val parentStage = durableStage("import-cooperation-parent.partial")
        val target = File(owned, "${sha()}.jpg")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val started = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val release = File(owned, "cooperation-release.signal")
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) = Unit
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val intent = Intent(context, PublicationCooperationProbeService::class.java)
            .putExtra(PublicationCooperationProbeService.EXTRA_DIRECTORY, owned.name)
        assertTrue("Distinct Android writer process must bind",
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            val pausedMarker = File(owned, "cooperation-paused.pid")
            var pid: Int? = null
            val pauseDeadline = SystemClock.elapsedRealtime() + 15_000L
            while (SystemClock.elapsedRealtime() < pauseDeadline && pid == null) {
                pid = runCatching {
                    DataInputStream(FileInputStream(pausedMarker)).use { it.readInt() }
                }.getOrNull()
                if (pid == null) SystemClock.sleep(50L)
            }
            assertTrue("Child must pause after first real CREATE_NEW chunk", pid != null)
            assertTrue("Publisher must be a genuinely distinct Android PID",
                pid != android.os.Process.myPid())
            assertTrue("Partial target must be visible during the child writer",
                target.isFile && target.length() == 64L * 1024L)

            val later = executor.submit<Boolean> {
                started.countDown()
                publishPrivateOriginalExclusively(
                    parentStage, target, sha(), bytes.size.toLong(), preferHardLink = false
                )
                true
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            SystemClock.sleep(300L)
            // Without an interprocess lock this publisher sees an incomplete
            // digest path and fails immediately. The test rejects that result.
            assertFalse("Same-SHA collaborator must wait for active writer", later.isDone)

            FileOutputStream(release).use { it.fd.sync() }
            val childComplete = File(owned, "cooperation-complete.marker")
            val childFailed = File(owned, "cooperation-failed.marker")
            val completionDeadline = SystemClock.elapsedRealtime() + 15_000L
            while (SystemClock.elapsedRealtime() < completionDeadline &&
                !childComplete.exists() && !childFailed.exists()
            ) {
                SystemClock.sleep(50L)
            }
            assertTrue("Child publisher must complete without a hidden error",
                childComplete.exists() && !childFailed.exists())
            assertTrue("Waiting parent publisher must verify same SHA",
                later.get(15, TimeUnit.SECONDS))
            assertArrayEquals(bytes, childStage.readBytes())
            assertArrayEquals(bytes, parentStage.readBytes())
            assertArrayEquals(bytes, target.readBytes())
        } finally {
            if (!release.exists()) {
                runCatching { FileOutputStream(release).use { it.fd.sync() } }
            }
            executor.shutdownNow()
            context.unbindService(connection)
        }
    }
}
