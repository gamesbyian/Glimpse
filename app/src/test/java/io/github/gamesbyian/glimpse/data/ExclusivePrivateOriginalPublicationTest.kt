package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
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
