package io.github.gamesbyian.glimpse.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExclusivePrivateOriginalPublicationTest {
    @get:Rule val scratch = TemporaryFolder()

    private val picture = "verified private image bytes".toByteArray()
    private fun hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Test fun newShaIsPublishedAtomicallyAsSameVerifiedInode() {
        val stage = scratch.newFile("import-test.partial").apply { writeBytes(picture) }
        val target = File(scratch.root, hex(picture) + ".jpg")
        publishPrivateOriginalExclusively(stage, target, hex(picture), picture.size.toLong())
        assertArrayEquals(picture, target.readBytes())
        assertTrue(Files.isSameFile(stage.toPath(), target.toPath()))
        // The owned staged name can be deleted without losing the final SHA.
        assertTrue(stage.delete())
        assertArrayEquals(picture, target.readBytes())
    }

    @Test fun exclusiveCopyFallbackWorksWhenAndroidDisallowsHardLinks() {
        val stage = scratch.newFile("import-copy.partial").apply { writeBytes(picture) }
        val target = File(scratch.root, hex(picture) + ".jpg")
        publishPrivateOriginalExclusively(
            stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
        )
        assertArrayEquals(picture, target.readBytes())
        assertFalse(Files.isSameFile(stage.toPath(), target.toPath()))
        assertTrue(stage.delete())
        assertArrayEquals(picture, target.readBytes())
    }

    @Test fun exclusiveCopyFallbackNeverReplacesCommittedDigest() {
        val stage = scratch.newFile("import-copy-race.partial").apply { writeBytes(picture) }
        val target = scratch.newFile(hex(picture) + ".jpg").apply {
            writeBytes(picture)
        }
        publishPrivateOriginalExclusively(
            stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
        )
        assertArrayEquals(picture, target.readBytes())
        assertFalse(Files.isSameFile(stage.toPath(), target.toPath()))
    }

    @Test fun exclusiveCopyFallbackDoesNotOverwriteCorruptedDigest() {
        val stage = scratch.newFile("import-copy-corrupt.partial").apply { writeBytes(picture) }
        val target = scratch.newFile(hex(picture) + ".jpg").apply {
            writeBytes("do not erase".toByteArray())
        }
        val before = target.readBytes()
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
            )
        }.isFailure)
        assertArrayEquals(before, target.readBytes())
    }

    @Test fun sameShaNeverReplacesPreviouslyCommittedInode() {
        val stage = scratch.newFile("import-new.partial").apply { writeBytes(picture) }
        val originalStage = scratch.newFile("original-source.partial").apply {
            writeBytes(picture)
        }
        val target = File(scratch.root, hex(picture) + ".jpg")
        Files.createLink(target.toPath(), originalStage.toPath())
        publishPrivateOriginalExclusively(stage, target, hex(picture), picture.size.toLong())
        assertTrue(Files.isSameFile(target.toPath(), originalStage.toPath()))
        assertFalse(Files.isSameFile(target.toPath(), stage.toPath()))
        assertArrayEquals(picture, target.readBytes())
    }

    @Test fun corruptedExistingShaIsNotReplacedOrDeleted() {
        val stage = scratch.newFile("import-verified.partial").apply { writeBytes(picture) }
        val target = scratch.newFile(hex(picture) + ".jpg").apply {
            writeBytes("different bytes".toByteArray())
        }
        val before = target.readBytes()
        val result = runCatching {
            publishPrivateOriginalExclusively(stage, target, hex(picture), picture.size.toLong())
        }
        assertTrue(result.isFailure)
        assertArrayEquals(before, target.readBytes())
        assertArrayEquals(picture, stage.readBytes())
    }

    @Test fun externalSymlinkTargetCannotBeFollowedOrReplaced() {
        val stage = scratch.newFile("import-verified.partial").apply { writeBytes(picture) }
        val outside = scratch.newFolder("foreign").resolve("private.bin").apply {
            writeBytes("outside data".toByteArray())
        }
        val target = File(scratch.root, hex(picture) + ".jpg")
        Files.createSymbolicLink(target.toPath(), outside.toPath())
        val result = runCatching {
            publishPrivateOriginalExclusively(stage, target, hex(picture), picture.size.toLong())
        }
        assertTrue(result.isFailure)
        assertArrayEquals("outside data".toByteArray(), outside.readBytes())
        assertTrue(Files.isSymbolicLink(target.toPath()))
    }

    @Test fun sixteenSameHashPublishersAllDeduplicateWithoutReadingPartialOutput() {
        val sources = (0 until 16).map { index ->
            scratch.newFile("import-concurrent-$index.partial").apply {
                writeBytes(picture)
            }
        }
        val target = File(scratch.root, hex(picture) + ".jpg")
        val gate = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = sources.map { staged ->
                executor.submit<Boolean> {
                    if (!gate.await(10, TimeUnit.SECONDS)) return@submit false
                    runCatching {
                        publishPrivateOriginalExclusively(
                            staged, target, hex(picture), picture.size.toLong(),
                            preferHardLink = false
                        )
                        true
                    }.getOrDefault(false)
                }
            }
            gate.countDown()
            assertTrue(
                "Every collaborating same-SHA publisher must receive verified bytes",
                futures.all { it.get(30, TimeUnit.SECONDS) }
            )
            assertArrayEquals(picture, target.readBytes())
            assertTrue(sources.all { it.exists() })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test fun injectedExclusiveCopyFailureDoesNotDiscardSourceOrOverwriteFragment() {
        val content = ByteArray(128 * 1024) { (it % 251).toByte() }
        val stage = scratch.newFile("import-fault-proof.partial")
        FileOutputStream(stage).use { output ->
            output.write(content)
            output.fd.sync()
        }
        val target = File(scratch.root, hex(content) + ".jpg")
        val progress = mutableListOf<Long>()
        val failure = runCatching {
            publishPrivateOriginalExclusively(
                stage, target, hex(content), content.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    progress += copied
                    if (copied < content.size) {
                        throw java.io.IOException("Injected mid-copy error")
                    }
                }
            )
        }
        assertTrue(failure.isFailure)
        assertTrue(progress.isNotEmpty())
        val fragment = target.readBytes()
        assertTrue(fragment.isNotEmpty() && fragment.size < content.size)
        assertArrayEquals(content.copyOf(fragment.size), fragment)
        assertArrayEquals(content, stage.readBytes())
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                stage, target, hex(content), content.size.toLong(),
                preferHardLink = false
            )
        }.isFailure)
        assertArrayEquals(fragment, target.readBytes())
        assertArrayEquals(content, stage.readBytes())
    }

    @Test fun durableCoordinationSentinelIsStableAndNeverTruncated() {
        val stage = scratch.newFile("import-stable-lock.partial").apply { writeBytes(picture) }
        val target = File(scratch.root, hex(picture) + ".jpg")
        publishPrivateOriginalExclusively(
            stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
        )
        val lockFile = File(scratch.root, ".glimpse-publication-${hex(picture)}.lock")
        assertTrue(lockFile.isFile)
        assertFalse(Files.isSymbolicLink(lockFile.toPath()))
        val metadata = "stable lock inode content (test only)".toByteArray()
        lockFile.writeBytes(metadata)
        // A later identical publication must reuse the very same lock path
        // without truncating/replacing its existing contents.
        publishPrivateOriginalExclusively(
            stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
        )
        assertArrayEquals(metadata, lockFile.readBytes())
        assertArrayEquals(picture, stage.readBytes())
        assertArrayEquals(picture, target.readBytes())
    }

    @Test fun forgedSymlinkedCoordinationSentinelCannotRedirectPublisher() {
        val stage = scratch.newFile("import-symlink-lock.partial").apply { writeBytes(picture) }
        val target = File(scratch.root, hex(picture) + ".jpg")
        val foreign = scratch.newFile("must-not-write").apply {
            writeText("sentinel user data")
        }
        val lockFile = File(scratch.root, ".glimpse-publication-${hex(picture)}.lock")
        Files.createSymbolicLink(lockFile.toPath(), foreign.toPath())
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                stage, target, hex(picture), picture.size.toLong(), preferHardLink = false
            )
        }.isFailure)
        assertFalse(target.exists())
        assertArrayEquals(picture, stage.readBytes())
        assertTrue(Files.isSymbolicLink(lockFile.toPath()))
        assertTrue(foreign.readText() == "sentinel user data")
    }

    @Test fun differentShaOriginalsRemainIndependentlyPublishableDuringPausedWriter() {
        val first = ByteArray(128 * 1024) { (it % 251).toByte() }
        val second = ByteArray(128 * 1024) { ((it + 17) % 251).toByte() }
        val firstStage = scratch.newFile("import-first-concurrent.partial")
            .apply { writeBytes(first) }
        val secondStage = scratch.newFile("import-second-concurrent.partial")
            .apply { writeBytes(second) }
        val firstTarget = File(scratch.root, hex(first) + ".jpg")
        val secondTarget = File(scratch.root, hex(second) + ".jpg")
        val firstPaused = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val threads = Executors.newFixedThreadPool(2)
        try {
            val firstJob = threads.submit {
                publishPrivateOriginalExclusively(
                    firstStage, firstTarget, hex(first), first.size.toLong(),
                    preferHardLink = false,
                    copyCheckpoint = { copied ->
                        if (copied == 64L * 1024L) {
                            firstPaused.countDown()
                            check(releaseFirst.await(10, TimeUnit.SECONDS)) {
                                "Synthetic writer not released"
                            }
                        }
                    }
                )
            }
            assertTrue("First writer must pause while holding its SHA lock",
                firstPaused.await(5, TimeUnit.SECONDS))
            val otherJob = threads.submit {
                publishPrivateOriginalExclusively(
                    secondStage, secondTarget, hex(second), second.size.toLong(),
                    preferHardLink = false
                )
            }
            // The second digest must finish while the first remains paused.
            // A global JVM or OS lock would time out here.
            otherJob.get(5, TimeUnit.SECONDS)
            assertArrayEquals(second, secondTarget.readBytes())
            assertFalse(firstJob.isDone)
            releaseFirst.countDown()
            firstJob.get(10, TimeUnit.SECONDS)
            assertArrayEquals(first, firstTarget.readBytes())
            assertEquals(0, activePrivateOriginalPublicationGates())
        } finally {
            releaseFirst.countDown()
            threads.shutdownNow()
        }
    }

    @Test fun shaGateRegistryDoesNotGrowWithDistinctPublishedOriginals() {
        val initial = activePrivateOriginalPublicationGates()
        assertEquals(0, initial)
        repeat(128) { index ->
            val content = "unique verified image bytes $index".toByteArray()
            val stage = scratch.newFile("import-bounded-$index.partial")
                .apply { writeBytes(content) }
            val target = File(scratch.root, hex(content) + ".jpg")
            publishPrivateOriginalExclusively(
                stage, target, hex(content), content.size.toLong(),
                preferHardLink = false
            )
            assertArrayEquals(content, target.readBytes())
            assertEquals("No idle SHA monitor must be retained", 0,
                activePrivateOriginalPublicationGates())
        }
    }

    @Test fun failedPublicationAlsoReleasesBorrowedShaMonitor() {
        val target = scratch.newFile(hex(picture) + ".jpg")
        val fragment = "untrusted fragment".toByteArray()
        target.writeBytes(fragment)
        val stage = scratch.newFile("import-bad-existing.partial")
            .apply { writeBytes(picture) }
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(
                stage, target, hex(picture), picture.size.toLong(),
                preferHardLink = false
            )
        }.isFailure)
        assertEquals(0, activePrivateOriginalPublicationGates())
        assertArrayEquals(fragment, target.readBytes())
        assertArrayEquals(picture, stage.readBytes())
    }

    @Test fun missingOrForeignStagingSourceIsNeverPublished() {
        val target = File(scratch.root, hex(picture) + ".jpg")
        val missing = File(scratch.root, "import-missing.partial")
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(missing, target, hex(picture), picture.size.toLong())
        }.isFailure)
        assertFalse(target.exists())
        val externalStage = scratch.newFolder("other").resolve("import-other.partial")
        externalStage.writeBytes(picture)
        assertTrue(runCatching {
            publishPrivateOriginalExclusively(externalStage, target, hex(picture), picture.size.toLong())
        }.isFailure)
        assertFalse(target.exists())
    }
}
