package com.raccoongang.course.presentation.videos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.raccoongang.core.BlockType
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.BlockCounts
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.R
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class CourseVideoViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val notifier = spyk<CourseNotifier>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val networkConnection = mockk<NetworkConnection>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()

    private val cantDownload = "You can download content only from Wi-fi"


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
            completion = 0.0
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
            completion = 0.0
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
            completion = 0.0
        )
    )


    @Before
    fun setUp() {
        every { resourceManager.getString(R.string.course_does_not_include_videos) } returns ""
        every { resourceManager.getString(com.raccoongang.course.R.string.course_can_download_only_with_wifi) } returns cantDownload
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getVideos empty list`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )

        every { interactor.getCourseStructureForVideos() } returns emptyList()
        viewModel.getVideos()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiState.value is CourseVideosUIState.Empty)
    }

    @Test
    fun `getVideos success`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )

        every { interactor.getCourseStructureForVideos() } returns blocks
        viewModel.getVideos()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiState.value is CourseVideosUIState.CourseData)
    }

    @Test
    fun `updateVideos success`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )

        every { interactor.getCourseStructureForVideos() } returns blocks
        coEvery { notifier.notifier } returns flow { emit(CourseStructureUpdated("", false)) }
        every { downloadDao.readAllData() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiState.value is CourseVideosUIState.CourseData)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `setIsUpdating success`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )
        viewModel.setIsUpdating()
        advanceUntilIdle()

        assert(viewModel.isUpdating.value == true)
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, without conection`() = runTest {
        val viewModel = CourseVideoViewModel(
            "",
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            downloadDao,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns false
        every { networkConnection.isOnline() } returns false
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")

        advanceUntilIdle()

        assert(viewModel.uiMessage.value != null)
        assert(!viewModel.hasInternetConnection)
    }


}