package com.raccoongang.course.presentation.outline

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
import com.raccoongang.core.domain.model.CourseComponentStatus
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.*
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.*
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

@OptIn(ExperimentalCoroutinesApi::class)
class CourseOutlineViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = spyk<CourseNotifier>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()

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
    fun `getCourseDataInternal no internet connection exception`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns emptyList()
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatus(any()) } throws UnknownHostException()

        viewModel.getCourseData()
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.Loading)
    }

    @Test
    fun `getCourseDataInternal unknown exception`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns emptyList()
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatus(any()) } throws Exception()

        viewModel.getCourseData()
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.Loading)
    }

    @Test
    fun `getCourseDataInternal success with internet connection`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns blocks
        every { networkConnection.isOnline() } returns true
        coEvery { downloadDao.readAllData() } returns flow {
            emit(
                listOf(
                    DownloadModelEntity.createFrom(
                        downloadModel
                    )
                )
            )
        }
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")

        viewModel.getCourseData()
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `getCourseDataInternal success without internet connection`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns blocks
        every { networkConnection.isOnline() } returns false
        coEvery { downloadDao.readAllData() } returns flow {
            emit(
                listOf(
                    DownloadModelEntity.createFrom(
                        downloadModel
                    )
                )
            )
        }
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")

        viewModel.getCourseData()
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 0) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `updateCourseData success with internet connection`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        every { interactor.getCourseStructureFromCache() } returns blocks
        every { networkConnection.isOnline() } returns true
        coEvery { downloadDao.readAllData() } returns flow {
            emit(
                listOf(
                    DownloadModelEntity.createFrom(
                        downloadModel
                    )
                )
            )
        }
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")

        viewModel.updateCourseData(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `CourseStructureUpdated notifier test`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            downloadDao,
            workerController
        )
        coEvery { notifier.notifier } returns flow { emit(CourseStructureUpdated("", false)) }
        every { interactor.getCourseStructureFromCache() } returns blocks
        every { downloadDao.readAllData() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.setIsUpdating()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
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
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
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
    fun `saveDownloadModels only wifi download, without connection`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
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