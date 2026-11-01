package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Real filesystem evidence. These tests NEVER mutate a suspect final name,
 * authorize recovery, attach Room rows or expose photo identifiers.
 */
class PendingPublicationEvidenceTest {
    @get:Rule val tmp = TemporaryFolder()
    private val source = "complete verified source bytes".toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(source)
        .joinToString("") { "%02x".format(it) }
    private val journal by lazy { PrivatePublicationReceiptJournal(tmp.root) }
    private fun stage() =
        File.createTempFile("import-", ".partial", tmp.root).apply { writeBytes(source) }
    private fun target() = File(tmp.root, "$sha.jpg")
    private fun begin(staged: File) = journal.begin(
        staged, target(), sha, source.size.toLong(), "synthetic-collection"
    )

    @Test fun completeRetainedStageAndMissingTargetAreOnlyEvidence() {
        val staged = stage()
        val receipt = begin(staged)
        val state = journal.inspectFileEvidence()!!
        assertEquals(1, state.examinedReceipts)
        assertEquals(0, state.untrustedReceipts)
        assertEquals(1, state.stagesExact)
        assertEquals(1, state.targetsMissing)
        assertEquals(0, state.targetsExact)
        assertEquals(0, state.targetsUnverified)
        assertFalse(state.hashBudgetExhausted)
        assertArrayEquals(source, staged.readBytes())
        assertTrue(receipt.file.exists())
        assertFalse(target().exists())
    }

    @Test fun bothBytesMatchingDoNotRemoveOrAdmitAnyFile() {
        val staged = stage()
        val destination = target().apply { writeBytes(source) }
        val receipt = begin(staged)
        val state = journal.inspectFileEvidence()!!
        assertEquals(1, state.stagesExact)
        assertEquals(1, state.targetsExact)
        assertEquals(0, state.targetsMissing)
        assertTrue(receipt.file.exists())
        assertArrayEquals(source, destination.readBytes())
        assertArrayEquals(source, staged.readBytes())
    }

    @Test fun corruptAndForeignFilesArePreservedWithoutRecoveryClaims() {
        val staged = stage()
        val foreign = "other user's original".toByteArray()
        val destination = target().apply { writeBytes(foreign) }
        begin(staged)
        staged.writeText("corrupted stage")
        val report = journal.inspectFileEvidence()!!
        assertEquals(0, report.stagesExact)
        assertEquals(1, report.stagesUnverified)
        assertEquals(1, report.targetsUnverified)
        assertArrayEquals(foreign, destination.readBytes())
        assertEquals("corrupted stage", staged.readText())
    }

    @Test fun linkToOutsideOwnerFileIsNeverTraversed() {
        val staged = stage()
        val elsewhere = tmp.newFolder("elsewhere")
        val foreign = File(elsewhere, "private").apply { writeBytes(source) }
        val handle = begin(staged)
        assertTrue(staged.delete())
        Files.createSymbolicLink(staged.toPath(), foreign.toPath())
        val state = journal.inspectFileEvidence()!!
        assertEquals(1, state.stagesUnverified)
        assertEquals(1, state.targetsMissing)
        assertArrayEquals(source, foreign.readBytes())
        assertTrue(handle.file.exists())
        assertTrue(Files.isSymbolicLink(staged.toPath()))
    }

    @Test fun malformedReceiptIsUntrustedWithNoFileAction() {
        val staged = stage()
        val receipt = begin(staged).file
        receipt.appendText("\tmalicious")
        val state = journal.inspectFileEvidence()!!
        assertEquals(1, state.untrustedReceipts)
        assertEquals(0, state.stagesExact)
        assertEquals(0, state.targetsExact)
        assertTrue(receipt.exists())
        assertArrayEquals(source, staged.readBytes())
    }

    @Test fun limitsBoundHashWorkAndReceiptTraversal() {
        begin(stage())
        begin(stage())
        val limited = journal.inspectFileEvidence(maxReceipts = 1, maxHashedBytes = 1)!!
        assertEquals(1, limited.examinedReceipts)
        assertEquals(1, limited.omittedReceipts)
        assertEquals(1, limited.stagesUnverified)
        assertEquals(1, limited.targetsMissing)
        assertTrue(limited.hashBudgetExhausted)
        assertEquals(2, journal.inspect()!!.structurallyValid)
    }

    @Test fun cancelledReadOnlyInspectionPropagatesCancellation() {
        begin(stage())
        val result = runCatching {
            journal.inspectFileEvidence(checkpoint = { throw CancellationException("cancelled") })
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(1, journal.inspect()!!.structurallyValid)
    }

    @Test fun cancellationDuringMultiChunkDigestPropagatesAndLeavesFilesUntouched() {
        // Two 64-KiB chunks must be read before cancellation is injected.
        // This distinguishes a real mid-hash checkpoint from the existing
        // per-receipt checkpoints which already protected short scans.
        val content = ByteArray(3 * 64 * 1024) { (it % 251).toByte() }
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }
        val staged = File.createTempFile("import-", ".partial", tmp.root)
            .apply { writeBytes(content) }
        val destination = File(tmp.root, "$digest.jpg")
        val receipt = journal.begin(
            staged, destination, digest, content.size.toLong(), "test-collection"
        )
        var checkpoints = 0
        val cancelled = runCatching {
            journal.inspectFileEvidence(checkpoint = {
                checkpoints++
                if (checkpoints == 5) throw CancellationException("cancelled during SHA read")
            })
        }
        assertTrue(cancelled.exceptionOrNull() is CancellationException)
        assertEquals(5, checkpoints)
        assertArrayEquals(content, staged.readBytes())
        assertTrue(receipt.file.exists())
        assertFalse(destination.exists())
    }

    @Test fun symlinkedRootIsNotAnAuthorityToInspectExternalBytes() {
        val external = tmp.newFolder("outside-root")
        val fakeRoot = File(tmp.root, "root-link")
        Files.createSymbolicLink(fakeRoot.toPath(), external.toPath())
        assertEquals(null, PrivatePublicationReceiptJournal(fakeRoot).inspectFileEvidence())
    }
}
