package org.openedx.downloads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DownloadCoursePreview
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.DownloadsAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.downloads.domain.interactor.DownloadInteractor
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.downloads.presentation.download.DownloadsViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import java.net.UnknownHostException
import java.util.Date

class DownloadsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    // Mocks for all dependencies
    private val downloadsRouter = mockk<DownloadsRouter>(relaxed = true)
    private val networkConnection = mockk<NetworkConnection>(relaxed = true)
    private val interactor = mockk<DownloadInteractor>(relaxed = true)
    private val downloadDialogManager = mockk<DownloadDialogManager>(relaxed = true)
    private val resourceManager = mockk<ResourceManager>(relaxed = true)
    private val fileUtil = mockk<FileUtil>(relaxed = true)
    private val config = mockk<Config>(relaxed = true)
    private val analytics = mockk<DownloadsAnalytics>(relaxed = true)
    private val preferencesManager = mockk<CorePreferences>(relaxed = true)
    private val coreAnalytics = mockk<CoreAnalytics>(relaxed = true)
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val workerController = mockk<DownloadWorkerController>(relaxed = true)
    private val downloadHelper = mockk<DownloadHelper>(relaxed = true)
    private val router = mockk<DownloadsRouter>(relaxed = true)
    private val discoveryNotifier = mockk<DiscoveryNotifier>(relaxed = true)
    private val courseNotifier = mockk<CourseNotifier>(relaxed = true)

    private val noInternet = "No connection"
    private val unknownError = "Unknown error"

    private val downloadCoursePreview =
        DownloadCoursePreview(
            id = "course1",
            name = "",
            image = "",
            totalSize = DownloadDialogManager.MAX_CELLULAR_SIZE.toLong()
        )
    private val assignmentProgress = AssignmentProgress(
        assignmentType = "Homework",
        numPointsEarned = 1f,
        numPointsPossible = 3f,
        shortLabel = "HW1",
    )
    private val blocks = listOf(
        Block(
            id = "id",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.CHAPTER,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = listOf("1", "id1"),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        ),
        Block(
            id = "id1",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.HTML,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = listOf("id2"),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        ),
        Block(
            id = "id2",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.HTML,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = emptyList(),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        )
    )

    private val downloadModel = DownloadModel(
        "id",
        "title",
        "",
        0,
        "",
        "url",
        FileType.VIDEO,
        DownloadedState.NOT_DOWNLOADED,
        null
    )

    private val courseStructure = CourseStructure(
        root = "",
        blockData = blocks,
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        certificate = null,
        isSelfPaced = false,
        progress = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { config.getApiHostURL() } returns "http://localhost:8000"
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns unknownError
        every { networkConnection.isOnline() } returns true

        coEvery { interactor.getDownloadCoursesPreview(any()) } returns flow {
            emit(listOf(downloadCoursePreview))
        }
        coEvery { interactor.getCourseStructureFromCache("course1") } returns courseStructure
        coEvery { interactor.getCourseStructure("course1") } returns courseStructure
        coEvery { interactor.getDownloadModelsByCourseIds(any()) } returns emptyList()
        coEvery { downloadDao.getAllDataFlow() } returns flowOf(
            listOf(
                DownloadModelEntity.createFrom(
                    downloadModel
                )
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onSettingsClick should navigate to settings`() = runTest {
        val viewModel = DownloadsViewModel(
            downloadsRouter,
            networkConnection,
            interactor,
            downloadDialogManager,
            resourceManager,
            fileUtil,
            config,
            analytics,
            discoveryNotifier,
            courseNotifier,
            router,
            preferencesManager,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper
        )
        advanceUntilIdle()

        val fragmentManager = mockk<FragmentManager>(relaxed = true)
        viewModel.onSettingsClick(fragmentManager)
        verify(exactly = 1) { downloadsRouter.navigateToSettings(fragmentManager) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `downloadCourse should show download dialog`() = runTest {
        val viewModel = DownloadsViewModel(
            downloadsRouter,
            networkConnection,
            interactor,
            downloadDialogManager,
            resourceManager,
            fileUtil,
            config,
            analytics,
            discoveryNotifier,
            courseNotifier,
            router,
            preferencesManager,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper
        )
        advanceUntilIdle()
        val fragmentManager = mockk<FragmentManager>(relaxed = true)
        viewModel.downloadCourse(fragmentManager, "course1")
        advanceUntilIdle()

        verify(exactly = 1) { analytics.logEvent(any(), any()) }

        coVerify(exactly = 1) {
            downloadDialogManager.showPopup(
                coursePreview = any(),
                isBlocksDownloaded = any(),
                fragmentManager = any(),
                removeDownloadModels = any(),
                saveDownloadModels = any(),
                onDismissClick = any(),
                onConfirmClick = any()
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelDownloading should update courseDownloadState to NOT_DOWNLOADED and cancel download job`() =
        runTest {
            val viewModel = DownloadsViewModel(
                downloadsRouter,
                networkConnection,
                interactor,
                downloadDialogManager,
                resourceManager,
                fileUtil,
                config,
                analytics,
                discoveryNotifier,
                courseNotifier,
                router,
                preferencesManager,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper
            )
            advanceUntilIdle()

            val fragmentManager = mockk<FragmentManager>(relaxed = true)
            viewModel.downloadCourse(fragmentManager, "course1")
            advanceUntilIdle()

            viewModel.cancelDownloading("course1")
            advanceUntilIdle()

            coVerify { interactor.getDownloadModelsByCourseIds(any()) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `removeDownloads should show remove popup with correct parameters`() = runTest {
        coEvery { interactor.getDownloadModelsByCourseIds(any()) } returns listOf(downloadModel)

        val viewModel = DownloadsViewModel(
            downloadsRouter,
            networkConnection,
            interactor,
            downloadDialogManager,
            resourceManager,
            fileUtil,
            config,
            analytics,
            discoveryNotifier,
            courseNotifier,
            router,
            preferencesManager,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper
        )
        advanceUntilIdle()

        val fragmentManager = mockk<FragmentManager>(relaxed = true)
        viewModel.removeDownloads(fragmentManager, "course1")
        advanceUntilIdle()

        coVerify {
            downloadDialogManager.showRemoveDownloadModelPopup(
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 1) { analytics.logEvent(any(), any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `refreshData no internet error should emit snack bar message`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getDownloadCoursesPreview(any()) } returns flow { throw UnknownHostException() }

        val viewModel = DownloadsViewModel(
            downloadsRouter,
            networkConnection,
            interactor,
            downloadDialogManager,
            resourceManager,
            fileUtil,
            config,
            analytics,
            discoveryNotifier,
            courseNotifier,
            router,
            preferencesManager,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper
        )
        val deferred = async { viewModel.uiMessage.first() }
        advanceUntilIdle()

        viewModel.refreshData()
        advanceUntilIdle()

        assertEquals(noInternet, (deferred.await() as? UIMessage.SnackBarMessage)?.message)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
