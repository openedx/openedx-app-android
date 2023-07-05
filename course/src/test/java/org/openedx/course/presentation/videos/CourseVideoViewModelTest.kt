package org.openedx.course.presentation.videos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.openedx.core.BlockType
import org.openedx.core.data.storage.PreferencesManager
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
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
import java.util.*

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

    private val downloadModelEntity =
        DownloadModelEntity("", "", 1, "", "", "VIDEO", "DOWNLOADED", null)


    @Before
    fun setUp() {
        every { resourceManager.getString(R.string.course_does_not_include_videos) } returns ""
        every { resourceManager.getString(org.openedx.course.R.string.course_can_download_only_with_wifi) } returns cantDownload
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getVideos empty list`() = runTest {
        every { interactor.getCourseStructureForVideos() } returns courseStructure.copy(blockData = emptyList())
        every { downloadDao.readAllData() } returns flow { emit(emptyList()) }

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

        viewModel.getVideos()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiState.value is CourseVideosUIState.Empty)
    }

    @Test
    fun `getVideos success`() = runTest {
        every { interactor.getCourseStructureForVideos() } returns courseStructure
        every { downloadDao.readAllData() } returns flow { emit(emptyList()) }
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


        viewModel.getVideos()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureForVideos() }

        assert(viewModel.uiState.value is CourseVideosUIState.CourseData)
    }

    @Test
    fun `updateVideos success`() = runTest {
        every { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { notifier.notifier } returns flow { emit(CourseStructureUpdated("", false)) }
        every { downloadDao.readAllData() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
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

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureForVideos() }

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
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { downloadDao.readAllData() } returns flow { emit(listOf(downloadModelEntity)) }
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
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { downloadDao.readAllData() } returns flow { emit(listOf(downloadModelEntity)) }
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
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { downloadDao.readAllData() } returns flow { emit(listOf(downloadModelEntity)) }
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
        coEvery { interactor.getCourseStructureForVideos() } returns courseStructure
        coEvery { downloadDao.readAllData() } returns flow { emit(listOf(downloadModelEntity)) }
        coEvery { workerController.saveModels(*anyVararg()) } returns Unit

        viewModel.saveDownloadModels("", "")

        advanceUntilIdle()

        assert(viewModel.uiMessage.value != null)
        assert(!viewModel.hasInternetConnection)
    }


}