package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Proof of read-only candidate discovery ONLY. These tests never create a
 * published original, attach Room rows, quarantine or delete user data.
 */
class MissingTargetStagePreflightTest {
    @get:Rule val tmp = TemporaryFolder()
    private val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
    private val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun stage() = File.createTempFile("import-", ".partial", tmp.root)
        .apply { writeBytes(bytes) }
    private fun target() = File(tmp.root, "$digest.jpg")
    private val journal by lazy { PrivatePublicationReceiptJournal(tmp.root) }

    @Test fun byteVerifiedStageAndAbsentTargetAreIdentifiedWithoutMutation() {
        val source = stage()
        val receipt = journal.begin(
            source, target(), digest, bytes.size.toLong(), "still-existing-collection"
        )
        val preflight = journal.inspectMissingTargetStages()!!
        assertEquals(1, preflight.possibleCount)
        assertEquals(1, preflight.examinedReceipts)
        assertEquals(0, preflight.untrustedReceipts)
        assertFalse(preflight.hashBudgetExhausted)
        val candidate = preflight.candidates.single()
        assertEquals("still-existing-collection", candidate.collectionId)
        assertEquals(digest, candidate.sha256)
        assertEquals(bytes.size.toLong(), candidate.byteLength)
        assertEquals(receipt.file, candidate.receipt.file)
        assertArrayEquals(bytes, source.readBytes())
        assertTrue(receipt.file.exists())
        assertFalse(target().exists())
    }

    @Test fun existingTargetEvenIfItsBytesAreExactIsNeverListedForRepair() {
        val source = stage()
        val destination = target().apply { writeBytes(bytes) }
        val receipt = journal.begin(
            source, destination, digest, bytes.size.toLong(), "collection"
        )
        assertEquals(0, journal.inspectMissingTargetStages()!!.possibleCount)
        assertArrayEquals(bytes, destination.readBytes())
        assertArrayEquals(bytes, source.readBytes())
        assertTrue(receipt.file.exists())
    }

    @Test fun danglingDestinationSymlinkDoesNotMasqueradeAsMissingFile() {
        val source = stage()
        val destination = target()
        val external = tmp.newFile("foreign-original").apply { writeText("foreign") }
        val receipt = journal.begin(
            source, destination, digest, bytes.size.toLong(), "collection"
        )
        Files.createSymbolicLink(destination.toPath(), external.toPath())
        assertEquals(0, journal.inspectMissingTargetStages()!!.possibleCount)
        assertEquals("foreign", external.readText())
        assertTrue(Files.isSymbolicLink(destination.toPath()))
        assertTrue(receipt.file.exists())
    }

    @Test fun tamperedStageAndMalformedReceiptCannotBecomeCandidates() {
        val source = stage()
        val receipt = journal.begin(source, target(), digest, bytes.size.toLong(), "collection")
        source.writeBytes(ByteArray(bytes.size) { 33 })
        assertEquals(0, journal.inspectMissingTargetStages()!!.possibleCount)
        source.writeBytes(bytes)
        receipt.file.appendText("\tunauthorized")
        val outcome = journal.inspectMissingTargetStages()!!
        assertEquals(0, outcome.possibleCount)
        assertEquals(1, outcome.untrustedReceipts)
        assertArrayEquals(bytes, source.readBytes())
    }

    @Test fun budgetLimitsCanNeverRepresentUnhashedBytesAsRecoverable() {
        val source = stage()
        journal.begin(source, target(), digest, bytes.size.toLong(), "collection")
        val evidence = journal.inspectMissingTargetStages(maxHashedBytes = 1)!!
        assertEquals(0, evidence.possibleCount)
        assertTrue(evidence.hashBudgetExhausted)
        assertArrayEquals(bytes, source.readBytes())
        assertFalse(target().exists())
    }

    @Test fun midDigestCancellationPropagatesWithoutChangingTheStagingSource() {
        val source = stage()
        val receipt = journal.begin(source, target(), digest, bytes.size.toLong(), "collection")
        var count = 0
        val result = runCatching {
            journal.inspectMissingTargetStages(checkpoint = {
                count++
                if (count == 4) throw CancellationException("scan cancelled")
            })
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertArrayEquals(bytes, source.readBytes())
        assertTrue(receipt.file.exists())
        assertFalse(target().exists())
    }

    @Test fun symlinkedRootCannotInspectFilesOutsidePrivateDirectory() {
        val destination = tmp.newFolder("elsewhere")
        val alias = File(tmp.root, "alias")
        Files.createSymbolicLink(alias.toPath(), destination.toPath())
        assertNull(PrivatePublicationReceiptJournal(alias).inspectMissingTargetStages())
    }
}
