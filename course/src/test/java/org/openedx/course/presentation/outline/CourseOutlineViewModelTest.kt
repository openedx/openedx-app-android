package org.openedx.course.presentation.outline

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.*
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.*
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import java.net.UnknownHostException
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class CourseOutlineViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val preferencesManager = mockk<CorePreferences>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = spyk<CourseNotifier>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()
    private val analytics = mockk<CourseAnalytics>()

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
            descendantsType = BlockType.HTML,
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
            descendantsType = BlockType.HTML,
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
            descendantsType = BlockType.HTML,
            completion = 0.0
        )
    )

    val courseStructure = CourseStructure(
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

    private val mockCourseDatesBannerInfo = CourseDatesBannerInfo(
        missedDeadlines = true,
        missedGatedContent = false,
        verifiedUpgradeLink = "",
        contentTypeGatingEnabled = false,
        hasEnded = true,
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
        every { resourceManager.getString(org.openedx.course.R.string.course_can_download_only_with_wifi) } returns cantDownload
        every { config.getApiHostURL() } returns "http://localhost:8000"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseDataInternal no internet connection exception`() = runTest {
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatus(any()) } throws UnknownHostException()

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

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
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatus(any()) } throws Exception()
        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

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
        every { interactor.getCourseStructureFromCache() } returns courseStructure
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
        every { config.isCourseNestedListEnabled() } returns false
        coEvery { interactor.getDatesBannerInfo(any()) } returns mockCourseDatesBannerInfo

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `getCourseDataInternal success without internet connection`() = runTest {
        every { interactor.getCourseStructureFromCache() } returns courseStructure
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
        every { config.isCourseNestedListEnabled() } returns false

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 0) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `updateCourseData success with internet connection`() = runTest {
        every { interactor.getCourseStructureFromCache() } returns courseStructure
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
        every { config.isCourseNestedListEnabled() } returns false
        coEvery { interactor.getDatesBannerInfo(any()) } returns mockCourseDatesBannerInfo

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        viewModel.updateCourseData(false)
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 2) { interactor.getCourseStatus(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is CourseOutlineUIState.CourseData)
    }

    @Test
    fun `CourseStructureUpdated notifier test`() = runTest {
        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )
        coEvery { notifier.notifier } returns flow { emit(CourseStructureUpdated("", false)) }
        every { interactor.getCourseStructureFromCache() } returns courseStructure
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

        coVerify(exactly = 2) { interactor.getCourseStructureFromCache() }
        coVerify(exactly = 1) { interactor.getCourseStatus(any()) }
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { networkConnection.isWifiConnected() } returns true
        every { networkConnection.isOnline() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")
        coEvery { downloadDao.readAllData() } returns flow { emit(emptyList()) }
        every { config.isCourseNestedListEnabled() } returns false
        coEvery { interactor.getDatesBannerInfo(any()) } returns mockCourseDatesBannerInfo

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        viewModel.saveDownloadModels("", "")
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() = runTest {
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns true
        every { networkConnection.isOnline() } returns true
        coEvery { downloadDao.readAllData() } returns mockk()
        coEvery { workerController.saveModels(any()) } returns Unit
        coEvery { downloadDao.readAllData() } returns flow { emit(emptyList()) }
        every { config.isCourseNestedListEnabled() } returns false
        coEvery { interactor.getDatesBannerInfo(any()) } returns mockCourseDatesBannerInfo

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        viewModel.saveDownloadModels("", "")
        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, without connection`() = runTest {
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns false
        every { networkConnection.isOnline() } returns false
        coEvery { workerController.saveModels(any()) } returns Unit
        coEvery { downloadDao.readAllData() } returns flow { emit(emptyList()) }
        every { config.isCourseNestedListEnabled() } returns false

        val viewModel = CourseOutlineViewModel(
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDao,
            workerController
        )

        viewModel.saveDownloadModels("", "")

        advanceUntilIdle()

        assert(viewModel.uiMessage.value != null)
        assert(!viewModel.hasInternetConnection)
    }

}
