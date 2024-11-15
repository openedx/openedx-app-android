package org.openedx.discovery.presentation.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Media
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalyticsEvent
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val preferencesManager = mockk<CorePreferences>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscoveryInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = spyk<DiscoveryNotifier>()
    private val analytics = mockk<DiscoveryAnalytics>()
    private val calendarSyncScheduler = mockk<CalendarSyncScheduler>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val mockCourse = Course(
        id = "id",
        blocksUrl = "blocksUrl",
        courseId = "courseId",
        effort = "effort",
        enrollmentStart = null,
        enrollmentEnd = null,
        hidden = false,
        invitationOnly = false,
        media = Media(),
        mobileAvailable = true,
        name = "Test course",
        number = "number",
        org = "EdX",
        pacing = "pacing",
        shortDescription = "shortDescription",
        start = "start",
        end = "end",
        startDisplay = "startDisplay",
        startType = "startType",
        overview = "",
        isEnrolled = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { config.getApiHostURL() } returns "http://localhost:8000"
        every { calendarSyncScheduler.requestImmediateSync(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseDetails no internet connection exception`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.Loading)
    }

    @Test
    fun `getCourseDetails unknown exception`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.Loading)
    }

    @Test
    fun `getCourseDetails success with internet`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { preferencesManager.user } returns null
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockk()

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `getCourseDetails success without internet`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { preferencesManager.user } returns null
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCourseDetailsFromCache(any()) } returns mockk()

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseDetails(any()) }
        coVerify(exactly = 1) { interactor.getCourseDetailsFromCache(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse internet connection error`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { preferencesManager.user } returns null
        coEvery { interactor.enrollInACourse(any()) } throws UnknownHostException()
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockCourse
        every { analytics.logEvent(any(), any()) } returns Unit

        viewModel.enrollInACourse("", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }
        verify { analytics.logEvent(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse unknown exception`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { preferencesManager.user } returns null
        coEvery { interactor.enrollInACourse(any()) } throws Exception()
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockCourse
        every {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED.eventName,
                any()
            )
        } returns Unit

        viewModel.enrollInACourse("", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }
        verify(exactly = 1) {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED.eventName,
                any()
            )
        }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse success`() = runTest {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        every { config.isPreLoginExperienceEnabled() } returns false
        every { preferencesManager.user } returns null
        every {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_SUCCESS.eventName,
                any()
            )
        } returns Unit
        coEvery { interactor.enrollInACourse(any()) } returns Unit
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockCourse

        delay(200)
        viewModel.enrollInACourse("", "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }
        verify(exactly = 1) {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logEvent(
                DiscoveryAnalyticsEvent.COURSE_ENROLL_SUCCESS.eventName,
                any()
            )
        }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `getCourseAboutBody contains black`() {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        val overview = viewModel.getCourseAboutBody(ULong.MAX_VALUE, ULong.MIN_VALUE)
        val count = overview.contains("black")
        assert(count)
    }

    @Test
    fun `getCourseAboutBody don't contains black`() {
        val viewModel = CourseDetailsViewModel(
            "",
            config,
            preferencesManager,
            networkConnection,
            interactor,
            resourceManager,
            notifier,
            analytics,
            calendarSyncScheduler,
        )
        val overview = viewModel.getCourseAboutBody(ULong.MAX_VALUE, ULong.MAX_VALUE)
        val count = overview.contains("black")
        assert(!count)
    }
}
