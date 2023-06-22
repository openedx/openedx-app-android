package com.raccoongang.course.presentation.section

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.raccoongang.core.BlockType
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.BlockCounts
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.domain.model.CoursewareAccess
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.*
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException
import java.util.*

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
    private val preferencesManager = mockk<PreferencesManager>()
    private val notifier = mockk<CourseNotifier>()

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
            type = BlockType.SEQUENTIAL,
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
            type = BlockType.VERTICAL,
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
        isSelfPaced = false
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
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )

        coEvery { interactor.getCourseStructureFromCache() } throws UnknownHostException()
        coEvery { interactor.getCourseStructureForVideos() } throws UnknownHostException()

        viewModel.getBlocks("", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )

        coEvery { interactor.getCourseStructureFromCache() } throws Exception()
        coEvery { interactor.getCourseStructureForVideos() } throws Exception()

        viewModel.getBlocks("id2", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks success`() = runTest {
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )

        coEvery { downloadDao.readAllData() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(downloadModel)))
        }
        coEvery { interactor.getCourseStructureFromCache() } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
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
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, without connection`() = runTest {
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns false
        every { networkConnection.isOnline() } returns false
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")

        advanceUntilIdle()

        assert(viewModel.uiMessage.value != null)
    }


    @Test
    fun `updateVideos success`() = runTest {
        val viewModel = CourseSectionViewModel(
            interactor,
            resourceManager,
            networkConnection,
            preferencesManager,
            notifier,
            workerController,
            downloadDao,
            ""
        )

        every { downloadDao.readAllData() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
        coEvery { notifier.notifier } returns flow {  }
        coEvery { interactor.getCourseStructureFromCache() } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)

    }

}