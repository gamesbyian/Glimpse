package io.github.gamesbyian.glimpse.data

import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine API29+ syscall proof against synthetic disposable app-private data. */
@RunWith(AndroidJUnit4::class)
class PrivateOriginalFileIdentityEvidenceDeviceTest {
    private lateinit var owned: File
    private val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
    private val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        owned = File(context.cacheDir, "glimpse-publish-instrumentation-${UUID.randomUUID()}")
        assertTrue(owned.mkdir())
    }

    @After fun tearDown() {
        if (::owned.isInitialized &&
            owned.name.startsWith("glimpse-publish-instrumentation-") &&
            owned.canonicalFile.parentFile == owned.parentFile.canonicalFile
        ) owned.deleteRecursively()
    }

    private fun original() = File(owned, "$digest.jpg")

    @Test fun noFollowDescriptorCorroboratesOnlyCurrentRegularInode() {
        val target = original()
        FileOutputStream(target).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        val initial = readPrivateOriginalFileIdentityEvidence(owned, target)!!
        assertTrue(initial.inode > 0)
        assertTrue(initial.device >= 0)
        assertEquals(Process.myUid(), initial.ownerUid)
        assertEquals(bytes.size.toLong(), initial.size)
        assertTrue(initial.hardLinks >= 1)
        val second = readPrivateOriginalFileIdentityEvidence(owned, target)!!
        assertEquals(initial.device, second.device)
        assertEquals(initial.inode, second.inode)
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun symlinkedOriginalAndRootCannotBeInterpretedAsOwnedInode() {
        val target = original()
        val foreign = File(owned, "outside-claim").apply {
            writeBytes(bytes)
        }
        Files.createSymbolicLink(target.toPath(), foreign.toPath())
        assertNull(readPrivateOriginalFileIdentityEvidence(owned, target))
        assertArrayEquals(bytes, foreign.readBytes())
        assertTrue(Files.isSymbolicLink(target.toPath()))
        val alias = File(owned.parentFile, "glimpse-identity-alias-${UUID.randomUUID()}")
        try {
            Files.createSymbolicLink(alias.toPath(), owned.toPath())
            assertNull(readPrivateOriginalFileIdentityEvidence(alias, target))
        } finally {
            Files.deleteIfExists(alias.toPath())
        }
        assertArrayEquals(bytes, foreign.readBytes())
    }

    @Test fun absentOrUnrecognizedNameIsNotInodeEvidence() {
        assertNull(readPrivateOriginalFileIdentityEvidence(owned, original()))
        val arbitrary = File(owned, "user-unrelated.txt").apply {
            writeText("no file admission")
        }
        assertNull(readPrivateOriginalFileIdentityEvidence(owned, arbitrary))
        assertEquals("no file admission", arbitrary.readText())
        assertFalse(original().exists())
    }
}
