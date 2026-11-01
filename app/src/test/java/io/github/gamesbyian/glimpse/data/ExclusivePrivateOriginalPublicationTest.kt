package io.github.gamesbyian.glimpse.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
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
