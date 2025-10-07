package org.openedx.course.presentation.home

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.Mock
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseProgressLoaded
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import java.net.UnknownHostException
import org.openedx.course.R as courseR

@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
class CourseHomeViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private val courseId = "test-course-id"
    private val courseTitle = "Test Course"
    private val context = mockk<Context>()
    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val resourceManager = mockk<ResourceManager>()
    private val courseNotifier = mockk<CourseNotifier>()
    private val networkConnection = mockk<NetworkConnection>()
    private val preferencesManager = mockk<CorePreferences>()
    private val analytics = mockk<CourseAnalytics>()
    private val downloadDialogManager = mockk<DownloadDialogManager>()
    private val fileUtil = mockk<FileUtil>()
    private val courseRouter = mockk<CourseRouter>()
    private val coreAnalytics = mockk<CoreAnalytics>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()
    private val downloadHelper = mockk<DownloadHelper>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val cantDownload = "You can download content only from Wi-fi"

    private val courseStructure = Mock.mockCourseStructure.copy(
        id = courseId,
        name = courseTitle
    )
    private val courseComponentStatus = Mock.mockCourseComponentStatus
    private val courseDatesResult = Mock.mockCourseDatesResult
    private val courseProgress = Mock.mockCourseProgress
    private val videoProgress = Mock.mockVideoProgress
    private val resetCourseDates = Mock.mockResetCourseDates

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every {
            resourceManager.getString(courseR.string.course_can_download_only_with_wifi)
        } returns cantDownload
        every {
            resourceManager.getString(R.string.core_dates_shift_dates_unsuccessful_msg)
        } returns "Failed to shift dates"

        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns true
        every { config.getCourseUIConfig().isCourseDownloadQueueEnabled } returns true

        every { preferencesManager.isRelativeDatesEnabled } returns true
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false

        every { networkConnection.isWifiConnected() } returns true
        every { networkConnection.isOnline() } returns true

        every { fileUtil.getExternalAppDir().path } returns "/test/path"

        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }

        every { courseNotifier.notifier } returns flow { }
        coEvery { courseNotifier.send(any<CourseDatesShifted>()) } returns Unit

        every { analytics.logEvent(any(), any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit

        every {
            downloadDialogManager.showPopup(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Unit

        coEvery { workerController.saveModels(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseData success`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }
        coEvery { interactor.getVideoProgress("video1") } returns videoProgress

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        coVerify { interactor.getCourseStructureFlow(courseId, false) }
        coVerify { interactor.getCourseStatusFlow(courseId) }
        coVerify { interactor.getCourseDatesFlow(courseId) }
        coVerify { interactor.getCourseProgress(courseId, false, true) }

        assertTrue(viewModel.uiState.value is CourseHomeUIState.CourseData)
        val courseData = viewModel.uiState.value as CourseHomeUIState.CourseData
        assertEquals(courseId, courseData.courseStructure.id)
        assertEquals(courseTitle, courseData.courseStructure.name)
        assertEquals(courseProgress, courseData.courseProgress)
    }

    @Test
    fun `getCourseData no internet connection error`() = runTest {
        coEvery {
            interactor.getCourseStructureFlow(
                courseId,
                false
            )
        } returns flow { throw UnknownHostException() }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value !is CourseHomeUIState.CourseData)
    }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `getCourseData unknown error`() = runTest {
        coEvery {
            interactor.getCourseStructureFlow(
                courseId,
                false
            )
        } returns flow { throw Exception() }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value !is CourseHomeUIState.CourseData)
    }

    @Test
    fun `saveDownloadModels with wifi only enabled but no wifi connection`() = runTest {
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns false

        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        viewModel.saveDownloadModels("/test/path", courseId, "test-block-id")

        coVerify(exactly = 0) { workerController.saveModels(any()) }
    }

    @Test
    fun `resetCourseDatesBanner success`() = runTest {
        coEvery { interactor.resetCourseDates(courseId) } returns resetCourseDates
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        var resetResult: Boolean? = null

        viewModel.resetCourseDatesBanner { success ->
            resetResult = success
        }

        advanceUntilIdle()

        coVerify { interactor.resetCourseDates(courseId) }
        coVerify { courseNotifier.send(CourseDatesShifted) }
        assertEquals(true, resetResult)
    }

    @Test
    fun `resetCourseDatesBanner with internet error`() = runTest {
        coEvery { interactor.resetCourseDates(courseId) } throws UnknownHostException()
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        var resetResult: Boolean? = null

        viewModel.resetCourseDatesBanner { success ->
            resetResult = success
        }

        advanceUntilIdle()

        coVerify { interactor.resetCourseDates(courseId) }
        coVerify(exactly = 0) { courseNotifier.send(CourseDatesShifted) }
        assertEquals(false, resetResult)
    }

    @Test
    fun `logVideoClick analytics event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        viewModel.logVideoClick("video1")

        verify {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_VIDEO_CLICK.eventName,
                match {
                    it[CourseAnalyticsKey.NAME.key] == CourseAnalyticsEvent.COURSE_HOME_VIDEO_CLICK.biValue &&
                            it[CourseAnalyticsKey.COURSE_ID.key] == courseId &&
                            it[CourseAnalyticsKey.COURSE_NAME.key] == courseTitle &&
                            it[CourseAnalyticsKey.BLOCK_ID.key] == "video1"
                }
            )
        }
    }

    @Test
    fun `logAssignmentClick analytics event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        viewModel.logAssignmentClick("assignment1")

        verify {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_ASSIGNMENT_CLICK.eventName,
                match {
                    it[CourseAnalyticsKey.NAME.key] == CourseAnalyticsEvent.COURSE_HOME_ASSIGNMENT_CLICK.biValue &&
                            it[CourseAnalyticsKey.COURSE_ID.key] == courseId &&
                            it[CourseAnalyticsKey.COURSE_NAME.key] == courseTitle &&
                            it[CourseAnalyticsKey.BLOCK_ID.key] == "assignment1"
                }
            )
        }
    }

    @Test
    fun `viewCertificateTappedEvent analytics event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        viewModel.viewCertificateTappedEvent()

        verify {
            analytics.logEvent(
                CourseAnalyticsEvent.VIEW_CERTIFICATE.eventName,
                match {
                    it[CourseAnalyticsKey.NAME.key] == CourseAnalyticsEvent.VIEW_CERTIFICATE.biValue &&
                            it[CourseAnalyticsKey.COURSE_ID.key] == courseId
                }
            )
        }
    }

    @Test
    fun `getCourseProgress success`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        viewModel.getCourseProgress()

        coVerify { interactor.getCourseProgress(courseId, false, true) }
    }

    @Test
    fun `CourseStructureUpdated notifier event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        every { courseNotifier.notifier } returns flow { emit(CourseStructureUpdated(courseId)) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        coVerify(atLeast = 2) { interactor.getCourseStructureFlow(courseId, false) }
    }

    @Test
    fun `CourseOpenBlock notifier event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        every { courseNotifier.notifier } returns flow { emit(CourseOpenBlock("test-block-id")) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()
    }

    @Test
    fun `CourseProgressLoaded notifier event`() = runTest {
        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        every { courseNotifier.notifier } returns flow { emit(CourseProgressLoaded) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        advanceUntilIdle()

        coVerify(atLeast = 2) { interactor.getCourseProgress(courseId, false, true) }
    }

    @Test
    fun `isCourseDropdownNavigationEnabled property`() = runTest {
        every { config.getCourseUIConfig().isCourseDropdownNavigationEnabled } returns true

        coEvery { interactor.getCourseStructureFlow(courseId, false) } returns flow {
            emit(
                courseStructure
            )
        }
        coEvery { interactor.getCourseStatusFlow(courseId) } returns flow {
            emit(
                courseComponentStatus
            )
        }
        coEvery { interactor.getCourseDatesFlow(courseId) } returns flow { emit(courseDatesResult) }
        coEvery {
            interactor.getCourseProgress(
                courseId,
                false,
                true
            )
        } returns flow { emit(courseProgress) }

        val viewModel = CourseHomeViewModel(
            courseId = courseId,
            courseTitle = courseTitle,
            context = context,
            config = config,
            interactor = interactor,
            resourceManager = resourceManager,
            courseNotifier = courseNotifier,
            networkConnection = networkConnection,
            preferencesManager = preferencesManager,
            analytics = analytics,
            downloadDialogManager = downloadDialogManager,
            fileUtil = fileUtil,
            courseRouter = courseRouter,
            coreAnalytics = coreAnalytics,
            downloadDao = downloadDao,
            workerController = workerController,
            downloadHelper = downloadHelper
        )

        assertTrue(viewModel.isCourseDropdownNavigationEnabled)
    }
}
