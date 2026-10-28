package io.github.gamesbyian.glimpse.data

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PrivateImportRootGuardTest {
    @Test fun ordinaryPrivateDirectoryAcceptsImportStaging() {
        val root = Files.createTempDirectory("glimpse-private-import-root")
        try {
            requirePrivateImportRoot(root.toFile())
            assertTrue(Files.isDirectory(root))
        } finally { Files.deleteIfExists(root) }
    }

    @Test fun cannotStageThroughSymlinkedRoot() {
        val parent = Files.createTempDirectory("glimpse-import-guard")
        try {
            val elsewhere = Files.createDirectory(parent.resolve("elsewhere"))
            val alias = Files.createSymbolicLink(parent.resolve("originals"), elsewhere)
            assertThrows(IllegalArgumentException::class.java) {
                requirePrivateImportRoot(alias.toFile())
            }
            assertTrue(Files.list(elsewhere).use { !it.findAny().isPresent })
        } finally { parent.toFile().deleteRecursively() }
    }

    @Test fun missingOrFileRootIsRefused() {
        val dir = Files.createTempDirectory("glimpse-bad-import-root")
        try {
            assertThrows(IllegalArgumentException::class.java) {
                requirePrivateImportRoot(dir.resolve("missing").toFile())
            }
            val regular = Files.createTempFile(dir, "root", ".txt")
            assertThrows(IllegalArgumentException::class.java) {
                requirePrivateImportRoot(regular.toFile())
            }
        } finally { dir.toFile().deleteRecursively() }
    }
}
