package org.openedx.core.module.download

import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openedx.core.instance.Instance
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
import java.io.File
import java.nio.file.Files

class DownloadHelperInstanceFolderTest {

    private val base = Files.createTempDirectory("downloads").toFile()

    private class FixedNamespace(override var current: StorageNamespace) : StorageNamespaceProvider {
        override fun namespaceFor(instance: Instance?) = StorageNamespace(instance?.key)
        override fun namespaceFor(instanceKey: String?) = StorageNamespace(instanceKey)
    }

    private val namespace = FixedNamespace(StorageNamespace.LEGACY)
    private val helper = DownloadHelper(mockk(), mockk(), namespace)

    @After
    fun tearDown() {
        base.deleteRecursively()
    }

    @Test
    fun `legacy namespace keeps downloading into the base folder`() {
        assertEquals(base.path, helper.instanceFolder(base.path))
    }

    @Test
    fun `each instance downloads into a folder of its own`() {
        namespace.current = StorageNamespace("alpha")
        val alpha = helper.instanceFolder(base.path)
        namespace.current = StorageNamespace("beta")
        val beta = helper.instanceFolder(base.path)

        assertNotEquals(alpha, beta)
        assertTrue(File(alpha).isDirectory)
        assertTrue(File(beta).isDirectory)
        assertEquals(File(base, "instances"), File(alpha).parentFile)
    }
}
