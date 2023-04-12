package com.raccoongang.course.presentation.units

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.raccoongang.core.BlockType
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.*
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.module.db.FileType
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseUnitsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val workerController = mockk<DownloadWorkerController>()
    private val networkConnection = mockk<NetworkConnection>()
    private val preferencesManager = mockk<PreferencesManager>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val cantDownload = "You can download content only from Wi-fi"


    private val blocks = listOf(
        Block(
            id = "id",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.HTML,
            displayName = "Block",
            graded = false,
            studentViewData = StudentViewData(
                false,
                encodedVideos = EncodedVideos(
                    mobileHigh = VideoInfo("", 0),
                    youtube = null,
                    hls = null,
                    fallback = null,
                    desktopMp4 = null,
                    mobileLow = null
                ),
                duration = 0,
                topicId = "",
                transcripts = null
            ),
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

    private val courseStructure = CourseStructure(
        "", blocks, "", "", "", "",
        null, "", "", null,
        CoursewareAccess(false, "", "", "", "", ""),
        null,
        null,
        false
    )

    private val downloadModel = DownloadModel(
        "id",
        "title",
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
        every { resourceManager.getString(com.raccoongang.course.R.string.course_can_download_only_with_wifi) } returns cantDownload
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getBlocks no internet connection exception`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } throws UnknownHostException()
        every { interactor.getCourseStructureForVideos() } throws UnknownHostException()

        viewModel.getBlocks("", CourseViewMode.FULL)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseUnitsUIState.Loading)
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } throws Exception()
        every { interactor.getCourseStructureForVideos() } throws Exception()

        viewModel.getBlocks("", CourseViewMode.FULL)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseUnitsUIState.Loading)
    }

    @Test
    fun `getBlocks success`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { interactor.getDownloadModels() } returns flow {
            emit(listOf(downloadModel))
        }

        viewModel.getBlocks("id", CourseViewMode.FULL)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseUnitsUIState.Blocks)
    }

    @Test
    fun `getBlocks success videos`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { interactor.getDownloadModels() } returns flow {
            emit(listOf(downloadModel))
        }

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseUnitsUIState.Blocks)
    }

    @Test
    fun `getDownloadModels observe`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { interactor.getDownloadModels() } returns flow {
            repeat(5) {
                delay(10000)
                emit(listOf(downloadModel))
            }
        }

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()


        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseUnitsUIState.Blocks)
    }

    @Test
    fun `removeDownload success`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        coEvery { interactor.removeDownloadModel(any()) } returns Unit

        viewModel.removeDownloadedModel("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.removeDownloadModel(any()) }
    }

    @Test
    fun `cancelWork success`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        coEvery { workerController.cancelWork(any()) } returns Unit

        viewModel.cancelWork("")
        advanceUntilIdle()

        coVerify(exactly = 1) { workerController.cancelWork(any()) }
    }

    @Test
    fun `saveDownloadModel test`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { preferencesManager.videoSettings.videoQuality } returns VideoQuality.AUTO
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModel("", blocks[0])
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModel only wifi download, with connection`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { preferencesManager.videoSettings.videoQuality } returns VideoQuality.AUTO
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModel("", blocks[0])
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModel only wifi download, without connection`() = runTest {
        val viewModel = CourseUnitsViewModel(
            interactor,
            resourceManager,
            preferencesManager,
            networkConnection,
            workerController
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { preferencesManager.videoSettings.videoQuality } returns VideoQuality.AUTO
        every { networkConnection.isWifiConnected() } returns false
        every { networkConnection.isOnline() } returns false
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModel("", blocks[0])

        advanceUntilIdle()

        assert(viewModel.uiMessage.value != null)
    }

}