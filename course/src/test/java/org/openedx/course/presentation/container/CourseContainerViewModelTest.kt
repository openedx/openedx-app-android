package org.openedx.course.presentation.container

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import org.openedx.core.CoreMocks
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.utils.ImageProcessor
import org.openedx.foundation.system.ResourceManager

@OptIn(ExperimentalCoroutinesApi::class)
class CourseContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val courseNotifier = spyk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val corePreferences = mockk<CorePreferences>()
    private val mockBitmap = mockk<Bitmap>()
    private val imageProcessor = mockk<ImageProcessor>()
    private val courseRouter = mockk<CourseRouter>()
    private val courseApi = mockk<CourseApi>()
    private val calendarSyncScheduler = mockk<CalendarSyncScheduler>()

    private val openEdx = "OpenEdx"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(id = R.string.platform_name) } returns openEdx
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { corePreferences.user } returns CoreMocks.mockUser
        every { corePreferences.appConfig } returns CoreMocks.mockAppConfig
        every { courseNotifier.notifier } returns emptyFlow()
        every { config.getApiHostURL() } returns "baseUrl"
        coEvery { interactor.getEnrollmentDetails(any()) } returns CoreMocks.mockCourseEnrollmentDetails
        every { imageProcessor.loadImage(any(), any(), any()) } returns Unit
        every { imageProcessor.applyBlur(any(), any()) } returns mockBitmap
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `getCourseEnrollmentDetails unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            config,
            interactor,
            resourceManager,
            courseNotifier,
            networkConnection,
            corePreferences,
            analytics,
            imageProcessor,
            calendarSyncScheduler,
            courseRouter
        )
        every { networkConnection.isOnline() } returns true
        coEvery {
            interactor.getCourseStructureFlow(any(), any())
        } returns flowOf(null)
        coEvery {
            interactor.getEnrollmentDetailsFlow(any())
        } returns flow { throw Exception() }
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetailsFlow(any()) }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value == CourseAccessError.UNKNOWN)
    }

    @Test
    fun `getCourseEnrollmentDetails success with internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            config,
            interactor,
            resourceManager,
            courseNotifier,
            networkConnection,
            corePreferences,
            analytics,
            imageProcessor,
            calendarSyncScheduler,
            courseRouter
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
            CoreMocks.mockCourseStructure
        )
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CoreMocks.mockCourseEnrollmentDetails
        )
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrollmentDetailsFlow(any()) }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }
        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value != null)
    }

    @Test
    fun `getCourseEnrollmentDetails success without internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            config,
            interactor,
            resourceManager,
            courseNotifier,
            networkConnection,
            corePreferences,
            analytics,
            imageProcessor,
            calendarSyncScheduler,
            courseRouter
        )
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCourseStructureFlow(any(), any()) } returns flowOf(
            CoreMocks.mockCourseStructure
        )
        coEvery { interactor.getEnrollmentDetailsFlow(any()) } returns flowOf(
            CoreMocks.mockCourseEnrollmentDetails
        )
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        } returns Unit
        every {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        } returns Unit
        viewModel.fetchCourseDetails()
        advanceUntilIdle()
        coVerify(exactly = 0) { courseApi.getEnrollmentDetails(any()) }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.DASHBOARD.eventName,
                any()
            )
        }
        verify(exactly = 1) {
            analytics.logScreenEvent(
                CourseAnalyticsEvent.HOME_TAB.eventName,
                any()
            )
        }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.courseAccessStatus.value != null)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            config,
            interactor,
            resourceManager,
            courseNotifier,
            networkConnection,
            corePreferences,
            analytics,
            imageProcessor,
            calendarSyncScheduler,
            courseRouter
        )
        coEvery { interactor.getCourseStructure(any(), true) } throws Exception()
        coEvery { courseNotifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        val message = viewModel.errorMessage.value
        assertEquals(somethingWrong, message)
        assert(!viewModel.refreshing.value)
    }

    @Test
    fun `updateData success`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            config,
            interactor,
            resourceManager,
            courseNotifier,
            networkConnection,
            corePreferences,
            analytics,
            imageProcessor,
            calendarSyncScheduler,
            courseRouter
        )
        coEvery { interactor.getEnrollmentDetails(any()) } returns CoreMocks.mockCourseEnrollmentDetails
        coEvery { interactor.getCourseStructure(any(), true) } returns CoreMocks.mockCourseStructure
        coEvery { courseNotifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
    }
}
