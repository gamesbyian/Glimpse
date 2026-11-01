package io.github.gamesbyian.glimpse.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import java.io.File
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
