package org.openedx.course.presentation.outline

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.model.DateType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.CoreAnalyticsEvent
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import java.net.UnknownHostException
import java.util.Date

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
    private val coreAnalytics = mockk<CoreAnalytics>()
    private val courseRouter = mockk<CourseRouter>()
    private val fileUtil = mockk<FileUtil>()
    private val downloadDialogManager = mockk<DownloadDialogManager>()
    private val downloadHelper = mockk<DownloadHelper>()

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

    private val dateBlock = CourseDateBlock(
        complete = false,
        date = Date(),
        dateType = DateType.TODAY_DATE,
        description = "Mocked Course Date Description"
    )
    private val mockDateBlocks = linkedMapOf(
        Pair(
            DatesSection.COMPLETED,
            listOf(dateBlock, dateBlock)
        ),
        Pair(
            DatesSection.PAST_DUE,
            listOf(dateBlock, dateBlock)
        ),
        Pair(
            DatesSection.TODAY,
            listOf(dateBlock, dateBlock)
        )
    )
    private val mockCourseDatesBannerInfo = CourseDatesBannerInfo(
        missedDeadlines = true,
        missedGatedContent = false,
        verifiedUpgradeLink = "",
        contentTypeGatingEnabled = false,
        hasEnded = true,
    )
    private val mockedCourseDatesResult = CourseDatesResult(
        datesSection = mockDateBlocks,
        courseBanner = mockCourseDatesBannerInfo,
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
        every { config.getApiHostURL() } returns "http://localhost:8000"
        every { downloadDialogManager.showDownloadFailedPopup(any(), any()) } returns Unit
        every { preferencesManager.isRelativeDatesEnabled } returns true

        coEvery { interactor.getCourseDates(any()) } returns mockedCourseDatesResult
        coEvery { interactor.getCourseDatesFlow(any()) } returns flowOf(mockedCourseDatesResult)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseDataInternal no internet connection exception`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
                courseStructure
            )
            every { networkConnection.isOnline() } returns true
            every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
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
            coEvery { interactor.getCourseStatusFlow(any()) } returns flow { throw UnknownHostException() }

            val viewModel = CourseContentAllViewModel(
                "",
                "",
                config,
                interactor,
                resourceManager,
                notifier,
                networkConnection,
                preferencesManager,
                analytics,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )

            val message = async {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
            viewModel.getCourseData()
            advanceUntilIdle()

            coVerify(exactly = 2) { interactor.getCourseStructureFlow(any(), any()) }
            coVerify(exactly = 2) { interactor.getCourseStatusFlow(any()) }

            assertEquals(noInternet, message.await()?.message)
            assert(viewModel.uiState.value is CourseContentAllUIState.Error)
        }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `getCourseDataInternal unknown exception`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(courseStructure)
        every { networkConnection.isOnline() } returns true
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        coEvery { interactor.getCourseStatusFlow(any()) } returns flow { throw Exception() }
        val viewModel = CourseContentAllViewModel(
            "",
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )

        val message = async {
            viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
        }
        viewModel.getCourseData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCourseStructureFlow(any(), any()) }
        coVerify(exactly = 2) { interactor.getCourseStatusFlow(any()) }

        assertEquals(somethingWrong, message.await()?.message)
        assert(viewModel.uiState.value is CourseContentAllUIState.Error)
    }

    @Test
    fun `getCourseDataInternal success with internet connection`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
                courseStructure
            )
            every { networkConnection.isOnline() } returns true
            coEvery { downloadDao.getAllDataFlow() } returns flow {
                emit(
                    listOf(
                        DownloadModelEntity.createFrom(
                            downloadModel
                        )
                    )
                )
            }
            coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false

            val viewModel = CourseContentAllViewModel(
                "",
                "",
                config,
                interactor,
                resourceManager,
                notifier,
                networkConnection,
                preferencesManager,
                analytics,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )

            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }

            viewModel.getCourseData()
            advanceUntilIdle()

            coVerify(exactly = 2) { interactor.getCourseStructureFlow(any(), any()) }
            coVerify(exactly = 2) { interactor.getCourseStatusFlow(any()) }

            assert(message.await() == null)
            assert(viewModel.uiState.value is CourseContentAllUIState.CourseData)
        }

    @Test
    fun `getCourseDataInternal success without internet connection`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
                courseStructure
            )
            every { networkConnection.isOnline() } returns false
            coEvery { downloadDao.getAllDataFlow() } returns flow {
                emit(
                    listOf(
                        DownloadModelEntity.createFrom(
                            downloadModel
                        )
                    )
                )
            }
            coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false

            val viewModel = CourseContentAllViewModel(
                "",
                "",
                config,
                interactor,
                resourceManager,
                notifier,
                networkConnection,
                preferencesManager,
                analytics,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )

            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }
            viewModel.getCourseData()
            advanceUntilIdle()

            coVerify(exactly = 2) { interactor.getCourseStructureFlow(any(), any()) }
            coVerify(exactly = 2) { interactor.getCourseStatusFlow(any()) }

            assert(message.await() == null)
            assert(viewModel.uiState.value is CourseContentAllUIState.CourseData)
        }

    @Test
    fun `updateCourseData success with internet connection`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
                courseStructure
            )
            every { networkConnection.isOnline() } returns true
            coEvery { downloadDao.getAllDataFlow() } returns flow {
                emit(
                    listOf(
                        DownloadModelEntity.createFrom(
                            downloadModel
                        )
                    )
                )
            }
            coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false

            val viewModel = CourseContentAllViewModel(
                "",
                "",
                config,
                interactor,
                resourceManager,
                notifier,
                networkConnection,
                preferencesManager,
                analytics,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )

            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }
            viewModel.getCourseData()
            advanceUntilIdle()

            coVerify(exactly = 2) { interactor.getCourseStructureFlow(any(), any()) }
            coVerify(exactly = 2) { interactor.getCourseStatusFlow(any()) }

            assert(message.await() == null)
            assert(viewModel.uiState.value is CourseContentAllUIState.CourseData)
        }

    @Test
    fun `CourseStructureUpdated notifier test`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(courseStructure)
        coEvery { notifier.notifier } returns flow { emit(CourseStructureUpdated("")) }
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))

        val viewModel = CourseContentAllViewModel(
            "",
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.getCourseData()
        advanceUntilIdle()

        coVerify(exactly = 3) { interactor.getCourseStructureFlow(any(), any()) }
        coVerify(exactly = 3) { interactor.getCourseStatusFlow(any()) }
    }

    @Test
    fun `saveDownloadModels test`() = runTest(UnconfinedTestDispatcher()) {
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(courseStructure)
        every { networkConnection.isWifiConnected() } returns true
        every { networkConnection.isOnline() } returns true
        every {
            coreAnalytics.logEvent(
                CoreAnalyticsEvent.VIDEO_DOWNLOAD_SUBSECTION.eventName,
                any()
            )
        } returns Unit
        coEvery { workerController.saveModels(any()) } returns Unit
        coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")
        coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))
        coEvery { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false

        val viewModel = CourseContentAllViewModel(
            "",
            "",
            config,
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            preferencesManager,
            analytics,
            downloadDialogManager,
            fileUtil,
            courseRouter,
            coreAnalytics,
            downloadDao,
            workerController,
            downloadHelper,
        )
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        viewModel.saveDownloadModels("", "", "")
        advanceUntilIdle()
        verify(exactly = 1) {
            coreAnalytics.logEvent(
                CoreAnalyticsEvent.VIDEO_DOWNLOAD_SUBSECTION.eventName,
                any()
            )
        }

        assert(message.await()?.message.isNullOrEmpty())
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { interactor.getCourseStructure(any()) } returns courseStructure
            coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
                courseStructure
            )
            coEvery { interactor.getCourseStatus(any()) } returns CourseComponentStatus("id")
            coEvery { interactor.getCourseStatusFlow(any()) } returns flowOf(CourseComponentStatus("id"))
            every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
            every { networkConnection.isWifiConnected() } returns true
            every { networkConnection.isOnline() } returns true
            coEvery { workerController.saveModels(any()) } returns Unit
            coEvery { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
            every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns false
            every { coreAnalytics.logEvent(any(), any()) } returns Unit

            val viewModel = CourseContentAllViewModel(
                "",
                "",
                config,
                interactor,
                resourceManager,
                notifier,
                networkConnection,
                preferencesManager,
                analytics,
                downloadDialogManager,
                fileUtil,
                courseRouter,
                coreAnalytics,
                downloadDao,
                workerController,
                downloadHelper,
            )
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
