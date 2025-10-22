package org.openedx.course.presentation.videos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.BlockType
import org.openedx.core.config.Config
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.helper.VideoPreviewHelper
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseVideoViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val courseNotifier = spyk<CourseNotifier>()
    private val coreAnalytics = mockk<CoreAnalytics>()
    private val courseAnalytics = mockk<CourseAnalytics>()
    private val preferencesManager = mockk<CorePreferences>()
    private val networkConnection = mockk<NetworkConnection>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()
    private val courseRouter = mockk<CourseRouter>()
    private val downloadHelper = mockk<DownloadHelper>()
    private val downloadDialogManager = mockk<DownloadDialogManager>()
    private val fileUtil = mockk<FileUtil>()
    private val videoPreviewHelper = mockk<VideoPreviewHelper>()

    private val cantDownload = "You can download content only from Wi-fi"

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

    private val downloadModelEntity =
        DownloadModelEntity("", "", "", 1, "", "", "VIDEO", "DOWNLOADED", null)

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

    @Before
    fun setUp() {
        every { resourceManager.getString(R.string.course_can_download_only_with_wifi) } returns cantDownload
        Dispatchers.setMain(dispatcher)
        every { config.getApiHostURL() } returns "http://localhost:8000"
        every { courseNotifier.notifier } returns flowOf()
        every { preferencesManager.isRelativeDatesEnabled } returns true
        every {
            downloadDialogManager.showPopup(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Unit

        every { videoPreviewHelper.getVideoPreviewWithId(any(), any(), any()) } returns Pair(
            "test",
            null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getVideos empty list`() = runTest(UnconfinedTestDispatcher()) {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
        coEvery {
            interactor.getCourseStructureForVideos(any())
        } returns courseStructure.copy(blockData = emptyList())
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        every { preferencesManager.videoSettings } returns VideoSettings.default
        val viewModel = CourseVideoViewModel(
            "",
            config,
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            courseNotifier,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            courseAnalytics,
            videoPreviewHelper,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )

        viewModel.getVideos()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureForVideos(any()) }

        assert(viewModel.uiState.value is CourseVideoUIState.Empty)
    }

    @Test
    fun `getVideos success`() = runTest(UnconfinedTestDispatcher()) {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
        every { downloadDao.getAllDataFlow() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
        every { preferencesManager.videoSettings } returns VideoSettings.default
        val viewModel = CourseVideoViewModel(
            "",
            config,
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            courseNotifier,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            courseAnalytics,
            videoPreviewHelper,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }

        assert(viewModel.uiState.value is CourseVideoUIState.CourseData)
    }

    @Test
    fun `updateVideos success`() = runTest(UnconfinedTestDispatcher()) {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
        coEvery { courseNotifier.notifier } returns flow {
            emit(CourseStructureUpdated(""))
        }
        every { downloadDao.getAllDataFlow() } returns flow {
            emit(emptyList())
        }
        every { preferencesManager.videoSettings } returns VideoSettings.default
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getVideoProgress(any()) } returns VideoProgressEntity("", "", 0L, 0L)
        val viewModel = CourseVideoViewModel(
            "",
            config,
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            courseNotifier,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            courseAnalytics,
            videoPreviewHelper,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureForVideos(any()) }

        assert(viewModel.uiState.value is CourseVideoUIState.CourseData)
    }

    @Test
    fun `setIsUpdating success`() = runTest(UnconfinedTestDispatcher()) {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
        every { preferencesManager.videoSettings } returns VideoSettings.default
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
        coEvery { downloadDao.getAllDataFlow() } returns flow { emit(listOf(downloadModelEntity)) }
        advanceUntilIdle()
    }

    @Test
    fun `saveDownloadModels test`() = runTest(UnconfinedTestDispatcher()) {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
        every { preferencesManager.videoSettings } returns VideoSettings.default
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        val viewModel = CourseVideoViewModel(
            "",
            config,
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            courseNotifier,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            courseAnalytics,
            videoPreviewHelper,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
        coEvery { downloadDao.getAllDataFlow() } returns flow { emit(listOf(downloadModelEntity)) }
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        viewModel.saveDownloadModels("", "", "")
        advanceUntilIdle()

        assert(message.await()?.message.isNullOrEmpty())
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() =
        runTest(UnconfinedTestDispatcher()) {
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
            every { preferencesManager.videoSettings } returns VideoSettings.default
            coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
            every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
            val viewModel = CourseVideoViewModel(
                "",
                config,
                interactor,
                resourceManager,
                networkConnection,
                preferencesManager,
                courseNotifier,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                courseAnalytics,
                videoPreviewHelper,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )
            coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
            coEvery { downloadDao.getAllDataFlow() } returns flow { emit(listOf(downloadModelEntity)) }
            every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
            every { networkConnection.isWifiConnected() } returns true
            coEvery { workerController.saveModels(any()) } returns Unit
            coEvery { downloadDao.getAllDataFlow() } returns flow {
                emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
            }
            every { coreAnalytics.logEvent(any(), any()) } returns Unit
            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }

            viewModel.saveDownloadModels("", "", "")
            advanceUntilIdle()

            assert(message.await()?.message.isNullOrEmpty())
        }

    @Test
    fun `saveDownloadModels only wifi download, without connection`() =
        runTest(UnconfinedTestDispatcher()) {
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
            every { preferencesManager.videoSettings } returns VideoSettings.default
            every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
            coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
            val viewModel = CourseVideoViewModel(
                "",
                config,
                interactor,
                resourceManager,
                networkConnection,
                preferencesManager,
                courseNotifier,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                courseAnalytics,
                videoPreviewHelper,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )
            every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
            every { networkConnection.isWifiConnected() } returns false
            every { networkConnection.isOnline() } returns false
            coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure
            coEvery { downloadDao.getAllDataFlow() } returns flow { emit(listOf(downloadModelEntity)) }
            coEvery { workerController.saveModels(any()) } returns Unit
            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }

            viewModel.saveDownloadModels("", "", "")

            advanceUntilIdle()

            assert(message.await()?.message.isNullOrEmpty())
        }
}
