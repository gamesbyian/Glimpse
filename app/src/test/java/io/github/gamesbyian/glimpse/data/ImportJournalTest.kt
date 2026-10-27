package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportJournalTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun recordedCompletionIsNotMisdiagnosedOnRestart() {
        val log = ImportJournal(temporary.newFile("history.log"), now = { 123L })
        log.record("batch-1", "collection-1", 0, ImportJournal.State.START)
        log.record("batch-1", "collection-1", 0, ImportJournal.State.IMPORTED)
        log.record("batch-1", "collection-1", 1, ImportJournal.State.START)
        log.record("batch-1", "collection-1", 1, ImportJournal.State.DUPLICATE)
        assertEquals(0, log.reconcileInterrupted())
        assertEquals(4, log.recent().size)
    }

    @Test fun unfinishedStartIsMarkedOnlyOnceAndNeverAutoRetried() {
        val file = temporary.newFile("history.log")
        val beforeRestart = ImportJournal(file, now = { 456L })
        beforeRestart.record("batch-2", "collection-1", 0, ImportJournal.State.START)
        beforeRestart.record("batch-2", "collection-1", 1, ImportJournal.State.START)
        beforeRestart.record("batch-2", "collection-1", 1, ImportJournal.State.FAILED)
        val afterRestart = ImportJournal(file, now = { 789L })
        assertEquals(1, afterRestart.reconcileInterrupted())
        assertEquals(0, afterRestart.reconcileInterrupted())
        assertEquals(ImportJournal.State.INTERRUPTED, afterRestart.recent().last().state)
        assertFalse(file.readText().contains("content://"))
    }

    @Test fun malformedAndPartialLinesAreIgnoredInsteadOfCrashing() {
        val file = temporary.newFile("history.log")
        file.writeText("bad line\n1\tbatch\tcollection\tbogus\tSTART\t1\n")
        val journal = ImportJournal(file)
        assertTrue(journal.recent().isEmpty())
        assertEquals(0, journal.reconcileInterrupted())
        journal.record("batch", "collection", 0, ImportJournal.State.IMPORTED)
        assertEquals(1, journal.recent().size)
    }

    @Test fun journalRetentionBoundsDiskAndRetainsNewestOperation() {
        val file = temporary.newFile("history.log")
        val journal = ImportJournal(file, now = { 100L }, maxBytes = 4096L)
        repeat(200) { i ->
            journal.record("batch", "collection", i, ImportJournal.State.IMPORTED)
        }
        assertTrue("Bounded local log", file.length() <= 4096L)
        assertEquals(199, journal.recent().last().position)
        assertTrue(journal.recent().size < 200)
    }

    @Test fun invalidIdentifiersDoNotBreakRecordBoundaries() {
        val journal = ImportJournal(temporary.newFile("history.log"))
        try {
            journal.record("one\nspoof", "collection", 0, ImportJournal.State.START)
            throw AssertionError("Expected unsafe record boundary to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(journal.recent().isEmpty())
        }
    }
}
