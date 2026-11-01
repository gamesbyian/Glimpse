package io.github.gamesbyian.glimpse.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import io.github.gamesbyian.glimpse.debug.PublicationKillProbeService
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual Android app-private filesystem, not a Linux-hosted JVM simulation.
 * Run on API29 and modern Android because SELinux can forbid hard links while
 * Files.createLink succeeds on the JVM. This test never touches real originals:
 * all objects live under one dedicated, randomly named cache child that the
 * test itself creates. No broad storage permissions or user Photo Picker URIs.
 */
@RunWith(AndroidJUnit4::class)
class PrivateOriginalPublicationDeviceTest {
    private lateinit var owned: File
    private val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        owned = File(context.cacheDir, "glimpse-publish-instrumentation-${UUID.randomUUID()}")
        assertTrue(owned.mkdir())
    }

    @After fun tearDown() {
        // Delete only our uniquely created instrumentation directory.
        if (::owned.isInitialized && owned.name.startsWith("glimpse-publish-instrumentation-") &&
            owned.canonicalFile.parentFile == owned.parentFile.canonicalFile) {
            owned.deleteRecursively()
        }
    }

    private fun staged(name: String) = File(owned, name).apply { writeBytes(bytes) }
    private fun durableStage(name: String) = File(owned, name).apply {
        FileOutputStream(this).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }

    private fun finalName() = File(owned, "${sha(bytes)}.jpg")

    @Test fun defaultPublicationWorksWithRealAndroidSELinuxAndSurvivesStagingCleanup() {
        val stage = staged("import-real-android.partial")
        val target = finalName()
        publishPrivateOriginalExclusively(stage, target, sha(bytes), bytes.size.toLong())
        assertArrayEquals(bytes, target.readBytes())
        assertTrue(stage.delete())
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun forcedNoLinkFallbackIsExclusiveAndPreservesPreviouslyPublishedBytes() {
        val source = staged("import-original.partial")
        val target = finalName()
        publishPrivateOriginalExclusively(
            source, target, sha(bytes), bytes.size.toLong(), preferHardLink = false
        )
        val prior = target.readBytes()
        val second = staged("import-raced.partial")
        publishPrivateOriginalExclusively(
            second, target, sha(bytes), bytes.size.toLong(), preferHardLink = false
        )
        assertArrayEquals(prior, target.readBytes())
        assertArrayEquals(bytes, second.readBytes())
    }

    @Test fun incompleteOrForeignDigestIsNotTruncatedOrOverwritten() {
        val target = finalName()
        val fragment = byteArrayOf(1, 2, 3, 4, 5)
        target.writeBytes(fragment)
        val source = staged("import-complete.partial")
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                source, target, sha(bytes), bytes.size.toLong(), preferHardLink = false
            )
        }.isFailure)
        assertArrayEquals(fragment, target.readBytes())
        assertArrayEquals(bytes, source.readBytes())
    }

    @Test fun receiptEvidenceRemainsReadOnlyAgainstRealAndroidIncompleteDigest() {
        val stage = staged("import-device-receipt.partial")
        val target = finalName()
        val journal = PrivatePublicationReceiptJournal(owned)
        val receipt = journal.begin(stage, target, sha(bytes), bytes.size.toLong(), "test")
        // Simulate an abruptly interrupted CREATE_NEW copy using a
        // disposable, test-owned fragment only, never a real app original.
        val fragment = byteArrayOf(1, 2, 3, 4)
        target.writeBytes(fragment)
        val evidence = journal.inspectFileEvidence()
        assertTrue(evidence != null)
        assertTrue(evidence!!.stagesExact == 1)
        assertTrue(evidence.targetsUnverified == 1)
        assertTrue(evidence.targetsExact == 0)
        assertArrayEquals(bytes, stage.readBytes())
        assertArrayEquals(fragment, target.readBytes())
        assertTrue(receipt.file.exists())
    }

    @Test fun preflightConfirmsStageAndNoFollowMissingTargetOnActualAndroid() {
        val source = durableStage("import-device-preflight.partial")
        val target = finalName()
        val journal = PrivatePublicationReceiptJournal(owned)
        val receipt = journal.begin(
            source, target, sha(bytes), bytes.size.toLong(), "device-test"
        )
        val ready = journal.inspectMissingTargetStages()!!
        assertEquals(1, ready.possibleCount)
        assertEquals(1, ready.examinedReceipts)
        assertEquals("device-test", ready.candidates.single().collectionId)
        assertArrayEquals(bytes, source.readBytes())
        assertTrue(receipt.file.exists())
        assertFalse(target.exists())

        // A dangling digest symlink is never classified as missing, even
        // though ordinary File.exists() would incorrectly return false.
        val notPresent = File(owned, "never-created")
        java.nio.file.Files.createSymbolicLink(target.toPath(), notPresent.toPath())
        val refused = journal.inspectMissingTargetStages()!!
        assertEquals(0, refused.possibleCount)
        assertTrue(java.nio.file.Files.isSymbolicLink(target.toPath()))
        assertArrayEquals(bytes, source.readBytes())
        assertTrue(receipt.file.exists())
    }

    @Test fun midCopyFaultRetainsDurableStageReceiptAndUnmodifiedPartialTarget() {
        val staged = durableStage("import-midcopy-fault.partial")
        val target = finalName()
        val journal = PrivatePublicationReceiptJournal(owned)
        val receipt = journal.begin(staged, target, sha(bytes), bytes.size.toLong(), "test")
        val result = runCatching {
            publishPrivateOriginalExclusively(
                staged, target, sha(bytes), bytes.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    if (copied in 1L until bytes.size.toLong()) {
                        throw java.io.IOException("Injected failure after real Android partial write")
                    }
                }
            )
        }
        assertTrue(result.isFailure)
        val savedFragment = target.readBytes()
        assertTrue(savedFragment.isNotEmpty())
        assertTrue(savedFragment.size < bytes.size)
        assertArrayEquals(bytes.copyOf(savedFragment.size), savedFragment)
        assertArrayEquals(bytes, staged.readBytes())
        assertTrue(receipt.file.exists())
        val evidence = journal.inspectFileEvidence()!!
        assertTrue(evidence.stagesExact == 1 && evidence.targetsUnverified == 1)
        // A subsequent publisher must refuse this existing partial rather
        // than silently repair it by truncating or replacing the digest path.
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                staged, target, sha(bytes), bytes.size.toLong(), preferHardLink = false
            )
        }.isFailure)
        assertArrayEquals(savedFragment, target.readBytes())
        assertArrayEquals(bytes, staged.readBytes())
    }

    @Test fun afterLastWriteFaultLeavesCompleteUncommittedDigestAndReceipt() {
        val staged = durableStage("import-lastwrite-fault.partial")
        val target = finalName()
        val journal = PrivatePublicationReceiptJournal(owned)
        val receipt = journal.begin(staged, target, sha(bytes), bytes.size.toLong(), "test")
        val result = runCatching {
            publishPrivateOriginalExclusively(
                staged, target, sha(bytes), bytes.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    if (copied == bytes.size.toLong()) {
                        throw java.io.IOException("Injected failure before destination force")
                    }
                }
            )
        }
        assertTrue(result.isFailure)
        assertArrayEquals(bytes, staged.readBytes())
        assertArrayEquals(bytes, target.readBytes())
        assertTrue(receipt.file.exists())
        val evidence = journal.inspectFileEvidence()!!
        assertTrue(evidence.stagesExact == 1 && evidence.targetsExact == 1)
        // A matching SHA here is still no evidence of a committed Room row,
        // fsync or an exclusively owned origin. The scan must change nothing.
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun actualChildProcessSigkillRetainsPartialDigestAndDurableSource() {
        verifyChildProcessKill(64L * 1024L)
    }

    @Test fun actualChildProcessSigkillAfterFinalWriteStillDoesNotCommitOriginal() {
        verifyChildProcessKill(bytes.size.toLong())
    }

    private fun verifyChildProcessKill(killAfter: Long) {
        // This is an actual separate Android app process, not an exception
        // injected into the instrumentation runner. All files are disposable
        // and scoped to the unique cache child created by setUp().
        val staged = durableStage("import-kill-source.partial")
        val target = finalName()
        val journal = PrivatePublicationReceiptJournal(owned)
        val receipt = journal.begin(staged, target, sha(bytes), bytes.size.toLong(), "test")
        val marker = File(owned, "probe-killed.pid")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, PublicationKillProbeService::class.java)
            .putExtra(PublicationKillProbeService.EXTRA_DIRECTORY, owned.name)
            .putExtra(PublicationKillProbeService.EXTRA_KILL_AFTER_BYTES, killAfter)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) = Unit
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue("Debug child service must bind", context.bindService(
            intent, connection, Context.BIND_AUTO_CREATE
        ))
        try {
            var childPid: Int? = null
            val markerDeadline = SystemClock.elapsedRealtime() + 15_000L
            while (SystemClock.elapsedRealtime() < markerDeadline && childPid == null) {
                childPid = runCatching {
                    DataInputStream(FileInputStream(marker)).use { it.readInt() }
                }.getOrNull()
                if (childPid == null) SystemClock.sleep(50L)
            }
            assertTrue("Separate process must reach the first real chunk", childPid != null)
            val pid = childPid!!
            assertTrue("The test process must survive its child kill", pid != android.os.Process.myPid())

            // Only ESRCH confirms that the distinct child PID actually died.
            // The debug service records its own PID just before SIGKILL.
            var childDied = false
            val deathDeadline = SystemClock.elapsedRealtime() + 15_000L
            while (SystemClock.elapsedRealtime() < deathDeadline && !childDied) {
                try {
                    Os.kill(pid, 0)
                    SystemClock.sleep(50L)
                } catch (error: ErrnoException) {
                    if (error.errno == OsConstants.ESRCH) childDied = true else throw error
                }
            }
            assertTrue("The isolated writer must die via actual SIGKILL", childDied)

            val fragment = target.readBytes()
            assertEquals(killAfter, fragment.size.toLong())
            assertArrayEquals(bytes.copyOf(fragment.size), fragment)
            assertArrayEquals(bytes, staged.readBytes())
            assertTrue(receipt.file.exists())
            val report = journal.inspectFileEvidence()!!
            assertEquals(1, report.stagesExact)
            if (killAfter < bytes.size.toLong()) {
                assertEquals(1, report.targetsUnverified)
                // A crash after one chunk leaves an incomplete reserved path.
                // No later importer may delete or replace the suspect bytes.
                assertTrue(runCatching {
                    publishPrivateOriginalExclusively(
                        staged, target, sha(bytes), bytes.size.toLong(),
                        preferHardLink = false
                    )
                }.isFailure)
            } else {
                assertEquals(1, report.targetsExact)
                // Exact current bytes do not prove fsync or a committed Room
                // membership. A later writer may verify and reuse them, but
                // neither this test nor the scanner attaches anything to DB.
                publishPrivateOriginalExclusively(
                    staged, target, sha(bytes), bytes.size.toLong(),
                    preferHardLink = false
                )
            }
            assertArrayEquals(fragment, target.readBytes())
            assertTrue(receipt.file.exists())
        } finally {
            context.unbindService(connection)
        }
    }

    @Test fun competingSameShaWritersCannotOverwriteEachOther() {
        val left = staged("import-left.partial")
        val right = staged("import-right.partial")
        val target = finalName()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = listOf(left, right).map { stage ->
                pool.submit<Boolean> {
                    start.await(10, TimeUnit.SECONDS) &&
                        runCatching {
                            publishPrivateOriginalExclusively(
                                stage, target, sha(bytes), bytes.size.toLong(),
                                preferHardLink = false
                            )
                            true
                        }.getOrDefault(false)
                }
            }
            start.countDown()
            assertTrue("Both same-SHA writers must safely deduplicate",
                jobs.all { it.get(30, TimeUnit.SECONDS) })
            assertArrayEquals(bytes, target.readBytes())
            assertFalse(target.length() < bytes.size)
        } finally {
            pool.shutdownNow()
        }
    }
}
