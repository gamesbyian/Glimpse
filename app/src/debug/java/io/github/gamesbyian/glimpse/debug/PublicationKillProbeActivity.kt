package io.github.gamesbyian.glimpse.debug

import android.app.Activity
import android.os.Bundle
import io.github.gamesbyian.glimpse.data.publishPrivateOriginalExclusively
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Debug APK only. Runs in its own process and kills THAT process after the
 * real exclusive CREATE_NEW writer has written one 64-KiB chunk. The Android
 * instrumentation runner lives in the main process and can inspect the bytes.
 *
 * Accept only a uniquely test-owned cache child. Never use filesDir/originals,
 * external storage, a picker URI, or a user-selected photo.
 */
class PublicationKillProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val directoryName = intent.getStringExtra(EXTRA_DIRECTORY)
        if (directoryName == null ||
            !DIRECTORY_NAME.matches(directoryName)
        ) {
            finish()
            return
        }
        val directory = File(cacheDir, directoryName)
        if (!directory.isDirectory || Files.isSymbolicLink(directory.toPath()) ||
            directory.canonicalFile.parentFile != cacheDir.canonicalFile
        ) {
            finish()
            return
        }
        val stage = File(directory, "import-kill-source.partial")
        if (!stage.isFile || Files.isSymbolicLink(stage.toPath()) ||
            stage.length() != EXPECTED_BYTES.toLong()
        ) {
            finish()
            return
        }
        val data = stage.readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }
        val destination = File(directory, "$sha.jpg")
        try {
            publishPrivateOriginalExclusively(
                stage, destination, sha, data.size.toLong(),
                preferHardLink = false,
                copyCheckpoint = { copied ->
                    if (copied == 64L * 1024L) {
                        val pid = android.os.Process.myPid()
                        FileOutputStream(File(directory, "probe-killed.pid")).use { out ->
                            out.write(ByteBuffer.allocate(4).putInt(pid).array())
                            out.fd.sync()
                        }
                        // Actual SIGKILL, not a simulated exception or
                        // cooperative coroutine cancellation.
                        android.os.Process.killProcess(pid)
                    }
                }
            )
        } finally {
            // Reaching this path indicates the kill did not interrupt the
            // writer. Instrumentation rejects its complete output.
            finish()
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
