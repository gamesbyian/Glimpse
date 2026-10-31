package io.github.gamesbyian.glimpse.data

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Real files and exact bytes: no Room mock, media provider or user originals.
 * Receipts describe an attempt but are NEVER authority for modifying its
 * target. Crash simulation simply leaves the receipt/stage in place.
 */
class PrivatePublicationReceiptJournalTest {
    @get:Rule val temp = TemporaryFolder()
    private val input = "fully validated image source".toByteArray()
    private fun digest() = MessageDigest.getInstance("SHA-256").digest(input)
        .joinToString("") { "%02x".format(it) }
    private fun staging() =
        File.createTempFile("import-", ".partial", temp.root).apply { writeBytes(input) }
    private fun target() = File(temp.root, digest() + ".jpg")

    @Test fun fsyncedReceiptIsUriFreeAndRetiredAfterVerifiedCommit() {
        val source = staging()
        val receipts = PrivatePublicationReceiptJournal(temp.root)
        val handle = receipts.begin(
            source, target(), digest(), input.size.toLong(), "source-collection"
        )
        val raw = handle.file.readText(Charsets.UTF_8)
        assertFalse(raw.contains("content://"))
        assertFalse(raw.contains(temp.root.absolutePath))
        assertTrue(raw.contains(source.name))
        assertTrue(raw.contains(digest()))
        assertEquals(ReceiptCounts(1, 0), receipts.inspect())
        assertTrue(receipts.complete(handle))
        assertEquals(ReceiptCounts(0, 0), receipts.inspect())
        assertTrue(source.exists())
    }

    @Test fun interruptedAttemptRemainsReadOnlyAndPreservesFullStage() {
        val source = staging()
        val receipts = PrivatePublicationReceiptJournal(temp.root)
        val handle = receipts.begin(
            source, target(), digest(), input.size.toLong(), "source-collection"
        )
        // Simulate process termination: never invoke complete.
        val freshReader = PrivatePublicationReceiptJournal(temp.root)
        assertTrue(handle.file.exists())
        assertEquals(ReceiptCounts(1, 0), freshReader.inspect())
        assertTrue(source.readBytes().contentEquals(input))
        assertFalse(target().exists())
    }

    @Test fun unexpectedOrModifiedReceiptIsNeverDeleted() {
        val source = staging()
        val receipts = PrivatePublicationReceiptJournal(temp.root)
        val handle = receipts.begin(source, target(), digest(), input.size.toLong(), "a")
        handle.file.appendText("tampering")
        assertFalse(receipts.complete(handle))
        assertTrue(handle.file.exists())
        assertEquals(ReceiptCounts(0, 1), receipts.inspect())
    }

    @Test fun twoIndependentAttemptsHaveDistinctReceiptNames() {
        val receipts = PrivatePublicationReceiptJournal(temp.root)
        val a = receipts.begin(staging(), target(), digest(), input.size.toLong(), "a")
        val b = receipts.begin(staging(), target(), digest(), input.size.toLong(), "a")
        assertFalse(a.file.name == b.file.name)
        assertEquals(ReceiptCounts(2, 0), receipts.inspect())
        assertTrue(receipts.complete(a))
        assertEquals(ReceiptCounts(1, 0), receipts.inspect())
        assertTrue(b.file.exists())
    }

    @Test fun foreignStagingAndSymlinkTargetAreRejectedWithoutReceipt() {
        val source = staging()
        val elsewhere = temp.newFolder("outside")
        val foreign = File(elsewhere, "import-unowned.partial").apply { writeBytes(input) }
        val receipts = PrivatePublicationReceiptJournal(temp.root)
        assertTrue(runCatching {
            receipts.begin(foreign, target(), digest(), input.size.toLong(), "a")
        }.isFailure)
        val outsideTarget = File(elsewhere, "existing-file.jpg").apply {
            writeBytes("unrelated private data".toByteArray())
        }
        val target = target()
        Files.createSymbolicLink(target.toPath(), outsideTarget.toPath())
        assertTrue(runCatching {
            receipts.begin(source, target, digest(), input.size.toLong(), "a")
        }.isFailure)
        assertEquals(ReceiptCounts(0, 0), receipts.inspect())
        assertTrue(outsideTarget.readBytes().contentEquals("unrelated private data".toByteArray()))
    }

    @Test fun symlinkedOriginalsDirectoryIsNotTrusted() {
        val other = temp.newFolder("other-owned")
        val link = File(temp.root, "alias")
        Files.createSymbolicLink(link.toPath(), other.toPath())
        val journal = PrivatePublicationReceiptJournal(link)
        assertTrue(runCatching { journal.inspect() }.getOrNull() == null)
    }
}
