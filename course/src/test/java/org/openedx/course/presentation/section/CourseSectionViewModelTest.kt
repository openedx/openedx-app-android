package org.openedx.course.presentation.section

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseSectionViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()
    private val networkConnection = mockk<NetworkConnection>()
    private val preferencesManager = mockk<CorePreferences>()
    private val notifier = mockk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val coreAnalytics = mockk<CoreAnalytics>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
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
            type = BlockType.SEQUENTIAL,
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
            type = BlockType.VERTICAL,
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
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every {
            resourceManager.getString(org.openedx.course.R.string.course_can_download_only_with_wifi)
        } returns cantDownload
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getBlocks no internet connection exception`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { interactor.getCourseStructure(any()) } throws UnknownHostException()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws UnknownHostException()

        viewModel.getBlocks("", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { interactor.getCourseStructure(any()) } throws Exception()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws Exception()

        viewModel.getBlocks("id2", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks success`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
        }
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit

        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit

        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `updateVideos success`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { notifier.notifier } returns flow { }
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)
    }
}
