package io.github.gamesbyian.glimpse.debug

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.github.gamesbyian.glimpse.data.publishPrivateOriginalExclusively
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Unexported debug-only bound service in a separate app process. It calls the
 * real exclusive publisher on a disposable instrumentation cache child, then
 * SIGKILLs its OWN process at a strictly allowlisted chunk boundary.
 *
 * A bound service avoids background Activity-launch policy as a confounder.
 * No user original, provider URI, Room database or external path is accessed.
 */
class PublicationKillProbeService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        val directoryName = intent?.getStringExtra(EXTRA_DIRECTORY)
        val killAfter = intent?.getLongExtra(EXTRA_KILL_AFTER_BYTES, -1L) ?: -1L
        if (directoryName != null && DIRECTORY_NAME.matches(directoryName) &&
            killAfter in setOf(64L * 1024L, EXPECTED_BYTES.toLong())) {
            val directory = File(cacheDir, directoryName)
            if (directory.isDirectory && !Files.isSymbolicLink(directory.toPath()) &&
                directory.canonicalFile.parentFile == cacheDir.canonicalFile
            ) {
                val stage = File(directory, "import-kill-source.partial")
                // BIND_AUTO_CREATE can restart a killed bound service on some
                // Android versions. Never replay the publication probe after
                // the first child has recorded its imminent SIGKILL identity.
                val marker = File(directory, "probe-killed.pid")
                if (!marker.exists() &&
                    stage.isFile && !Files.isSymbolicLink(stage.toPath()) &&
                    stage.length() == EXPECTED_BYTES.toLong()
                ) {
                    Thread({ runProbe(directory, stage, killAfter) }, "glimpse-publication-sigkill")
                        .start()
                }
            }
        }
        return Binder()
    }

    private fun runProbe(directory: File, stage: File, killAfter: Long) {
        try {
            val data = stage.readBytes()
            val sha = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it) }
            val target = File(directory, "$sha.jpg")
            publishPrivateOriginalExclusively(
                stage, target, sha, data.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    if (copied == killAfter) {
                        val pid = android.os.Process.myPid()
                        FileOutputStream(File(directory, "probe-killed.pid")).use { out ->
                            out.write(ByteBuffer.allocate(4).putInt(pid).array())
                            out.fd.sync()
                        }
                        // Actual SIGKILL, not a throw or cooperative cancellation.
                        android.os.Process.killProcess(pid)
                    }
                }
            )
        } finally {
            // If SIGKILL failed, the test detects a live PID or an unexpected
            // complete digest. A finished service never claims success.
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_DIRECTORY = "glimpse_test_private_directory"
        const val EXTRA_KILL_AFTER_BYTES = "glimpse_test_kill_after_bytes"
        const val EXPECTED_BYTES = 128 * 1024
        private val DIRECTORY_NAME = Regex(
            "^glimpse-publish-instrumentation-[0-9a-fA-F-]{36}$"
        )
    }
}
