package io.github.gamesbyian.glimpse.data

import java.io.File
import java.io.FileOutputStream

/**
 * Bounded, device-private history of import attempts. No provider URI, photo
 * path, image bytes, filename, EXIF or exception message ever enters the log.
 *
 * This is an advisory recovery ledger, not a substitute for the transactional
 * Room photo/membership commit. A START without a terminal event means that
 * completion was not recorded; the photo might already have been committed.
 */
internal class ImportJournal(
    private val file: File,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxBytes: Long = 512L * 1024L
) {
    enum class State { START, IMPORTED, DUPLICATE, FAILED, INTERRUPTED }

    data class Event(
        val run: String,
        val collection: String,
        val position: Int,
        val state: State,
        val recordedAt: Long
    )

    @Synchronized
    fun record(run: String, collection: String, position: Int, state: State) {
        require(run.length in 1..80 && collection.length in 1..80 && position >= 0)
        require((run + collection).none { it == '\t' || it == '\n' || it == '\r' })
        file.parentFile?.mkdirs()
        val line = "1\t$run\t$collection\t$position\t${state.name}\t${now()}\n"
        FileOutputStream(file, true).use { out ->
            out.write(line.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (file.length() > maxBytes) compact()
    }

    @Synchronized
    fun recent(): List<Event> {
        if (!file.isFile) return emptyList()
        return file.useLines { lines ->
            lines.mapNotNull { parse(it) }.takeLast(4096).toList()
        }
    }

    /**
     * Run only at fresh ViewModel initialization, before new import jobs start.
     * Mark unclosed operations as INTERRUPTED once, without claiming failure or
     * attempting a provider-URI retry that may have lost its grant.
     */
    @Synchronized
    fun reconcileInterrupted(): Int {
        val latest = linkedMapOf<Pair<String, Int>, Event>()
        for (event in recent()) latest[event.run to event.position] = event
        val dangling = latest.values.filter { it.state == State.START }
        for (event in dangling) {
            record(event.run, event.collection, event.position, State.INTERRUPTED)
        }
        return dangling.size
    }

    private fun parse(line: String): Event? {
        val parts = line.split('\t')
        if (parts.size != 6 || parts[0] != "1") return null
        return try {
            val run = parts[1]
            val collection = parts[2]
            if (run.length !in 1..80 || collection.length !in 1..80) return null
            Event(
                run = run,
                collection = collection,
                position = parts[3].toInt().also { require(it >= 0) },
                state = State.valueOf(parts[4]),
                recordedAt = parts[5].toLong()
            )
        } catch (_: RuntimeException) {
            null
        }
    }

    /**
     * Bound storage even with long-running repeated imports. Keep newest full
     * lines and replace via rename. Interrupted work older than retained history
     * is not advertised as recovered.
     */
    private fun compact() {
        val all = file.readLines(Charsets.UTF_8)
        val retained = ArrayDeque<String>()
        var bytes = 0L
        val targetBytes = maxOf(1024L, maxBytes / 2)
        for (line in all.asReversed()) {
            val size = line.toByteArray(Charsets.UTF_8).size + 1L
            if (bytes + size > targetBytes) break
            retained.addFirst(line)
            bytes += size
        }
        val staging = File(file.parentFile, file.name + ".rewrite")
        try {
            FileOutputStream(staging).use { out ->
                for (line in retained) {
                    out.write((line + "\n").toByteArray(Charsets.UTF_8))
                }
                out.fd.sync()
            }
            check(staging.renameTo(file)) { "Import journal retention update failed" }
        } finally {
            staging.delete()
        }
    }
}
