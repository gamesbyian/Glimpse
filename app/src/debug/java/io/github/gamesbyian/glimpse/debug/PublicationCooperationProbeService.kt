package io.github.gamesbyian.glimpse.debug

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import io.github.gamesbyian.glimpse.data.publishPrivateOriginalExclusively
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Debug-only and non-exported. A distinct process writes a synthetic original
 * and pauses after the real first CREATE_NEW chunk. The parent instrumentation
 * must prove a same-SHA second publisher cannot inspect those partial bytes
 * until the first writer finishes. Never touches filesDir/originals or media.
 */
class PublicationCooperationProbeService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        val name = intent?.getStringExtra(EXTRA_DIRECTORY)
        if (name != null && DIRECTORY_NAME.matches(name)) {
            val owned = File(cacheDir, name)
            val stage = File(owned, "import-cooperation-child.partial")
            if (owned.isDirectory && !Files.isSymbolicLink(owned.toPath()) &&
                owned.canonicalFile.parentFile == cacheDir.canonicalFile &&
                stage.isFile && !Files.isSymbolicLink(stage.toPath()) &&
                stage.length() == EXPECTED_BYTES.toLong() &&
                !File(owned, "cooperation-paused.pid").exists()
            ) {
                Thread({ runProbe(owned, stage) }, "glimpse-cross-process-publisher").start()
            }
        }
        return Binder()
    }

    private fun runProbe(owned: File, stage: File) {
        try {
            val source = stage.readBytes()
            val sha = MessageDigest.getInstance("SHA-256").digest(source)
                .joinToString("") { "%02x".format(it) }
            val target = File(owned, "$sha.jpg")
            publishPrivateOriginalExclusively(
                stage, target, sha, source.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    if (copied == 64L * 1024L) {
                        val pid = android.os.Process.myPid()
                        FileOutputStream(File(owned, "cooperation-paused.pid")).use { out ->
                            out.write(ByteBuffer.allocate(4).putInt(pid).array())
                            out.fd.sync()
                        }
                        val release = File(owned, "cooperation-release.signal")
                        val deadline = SystemClock.elapsedRealtime() + 15_000L
                        while (SystemClock.elapsedRealtime() < deadline &&
                            (!release.isFile || Files.isSymbolicLink(release.toPath()))
                        ) {
                            Thread.sleep(25L)
                        }
                        check(release.isFile && !Files.isSymbolicLink(release.toPath())) {
                            "Disposable publisher was not released by instrumentation."
                        }
                    }
                }
            )
            FileOutputStream(File(owned, "cooperation-complete.marker")).use { out ->
                out.write(1)
                out.fd.sync()
            }
        } catch (failure: Exception) {
            // A failure flag contains an exception *type*, not input bytes,
            // media paths or photo metadata.
            FileOutputStream(File(owned, "cooperation-failed.marker")).use { out ->
                out.write(1)
                out.fd.sync()
            }
        } finally {
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_DIRECTORY = "glimpse_test_private_directory"
        const val EXPECTED_BYTES = 128 * 1024
        private val DIRECTORY_NAME = Regex(
            "^glimpse-publish-instrumentation-[0-9a-fA-F-]{36}$"
        )
    }
}
