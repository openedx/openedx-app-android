package org.openedx.course.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.room.OfflineXBlockProgress
import org.openedx.core.data.model.room.XBlockProgressData
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceDatabases
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.module.db.DownloadDao

class CourseRepositoryOfflineSyncTest {

    private class Selection(override var current: StorageNamespace) : StorageNamespaceProvider {
        override fun namespaceFor(instance: Instance?) = StorageNamespace(instance?.key)
        override fun namespaceFor(instanceKey: String?) = StorageNamespace(instanceKey)
    }

    private val alpha = StorageNamespace("alpha")
    private val beta = StorageNamespace("beta")
    private val alphaDao = mockk<DownloadDao>(relaxed = true)
    private val betaDao = mockk<DownloadDao>(relaxed = true)
    private val selectedSiteDao = mockk<DownloadDao>(relaxed = true)
    private val api = mockk<CourseApi>(relaxed = true)
    private val selection = Selection(alpha)
    private val databases = object : InstanceDatabases {
        override fun downloadDao(namespace: StorageNamespace) = if (namespace == alpha) alphaDao else betaDao
    }

    private fun progress(blockId: String) =
        OfflineXBlockProgress(blockId, "course", XBlockProgressData("https://alpha/handler", "POST", "a%3D1"))

    private fun repository() = CourseRepository(
        api = api,
        courseDao = mockk(relaxed = true),
        downloadDao = selectedSiteDao,
        preferencesManager = mockk(relaxed = true),
        networkConnection = mockk(relaxed = true),
        namespaces = selection,
        databases = databases,
    )

    @Test
    fun `a switch during sync stops it and never touches the new site`() = runTest {
        coEvery { alphaDao.getAllOfflineXBlockProgress() } returns listOf(progress("block-1"), progress("block-2"))
        coEvery { api.submitOfflineXBlockProgress("course", "block-1", any()) } answers {
            selection.current = beta
        }

        repository().submitAllOfflineXBlockProgress()

        coVerify { alphaDao.removeOfflineXBlockProgress(listOf("block-1")) }
        coVerify(exactly = 0) { api.submitOfflineXBlockProgress(any(), "block-2", any()) }
        coVerify(exactly = 0) { betaDao.removeOfflineXBlockProgress(any()) }
        coVerify(exactly = 0) { selectedSiteDao.removeOfflineXBlockProgress(any()) }
    }

    @Test
    fun `without a switch every stored block is submitted and cleared`() = runTest {
        coEvery { alphaDao.getAllOfflineXBlockProgress() } returns listOf(progress("block-1"), progress("block-2"))

        repository().submitAllOfflineXBlockProgress()

        coVerify { alphaDao.removeOfflineXBlockProgress(listOf("block-1")) }
        coVerify { alphaDao.removeOfflineXBlockProgress(listOf("block-2")) }
    }
}
