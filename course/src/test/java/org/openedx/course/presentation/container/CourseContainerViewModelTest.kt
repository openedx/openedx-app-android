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
import org.openedx.core.ImageProcessor
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.CourseStructureModel
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarManager
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val calendarManager = mockk<CalendarManager>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = spyk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val corePreferences = mockk<CorePreferences>()
    private val coursePreferences = mockk<CoursePreferences>()
    private val mockBitmap = mockk<Bitmap>()
    private val imageProcessor = mockk<ImageProcessor>()
    private val courseRouter = mockk<CourseRouter>()
    private val courseApi = mockk<CourseApi>()

    private val openEdx = "OpenEdx"
    private val calendarTitle = "OpenEdx - Abc"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val user = User(
        id = 0,
        username = "",
        email = "",
        name = "",
    )
    private val appConfig = AppConfig(
        CourseDatesCalendarSync(
            isEnabled = true,
            isSelfPacedEnabled = true,
            isInstructorPacedEnabled = true,
            isDeepLinkEnabled = false,
        )
    )
    private val courseStructure = CourseStructure(
        root = "",
        blockData = listOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(0),
        startDisplay = "",
        startType = "",
        end = null,
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

    private val courseStructureModel = CourseStructureModel(
        root = "",
        blockData = mapOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = "",
        startDisplay = "",
        startType = "",
        end = null,
        coursewareAccess = null,
        media = null,
        certificate = null,
        isSelfPaced = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(id = R.string.platform_name) } returns openEdx
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { corePreferences.user } returns user
        every { corePreferences.appConfig } returns appConfig
        every { notifier.notifier } returns emptyFlow()
        every { calendarManager.getCourseCalendarTitle(any()) } returns calendarTitle
        every { config.getApiHostURL() } returns "baseUrl"
        every { imageProcessor.loadImage(any(), any(), any()) } returns Unit
        every { imageProcessor.applyBlur(any(), any()) } returns mockBitmap
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseStructure internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStructure(any()) } throws UnknownHostException()
        every { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) } returns Unit
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        verify(exactly = 1) { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) }

        val message = viewModel.errorMessage.value
        assertEquals(noInternet, message)
        assert(!viewModel.refreshing.value)
        assert(viewModel.dataReady.value == null)
    }

    @Test
    fun `getCourseStructure unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStructure(any()) } throws Exception()
        every { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) } returns Unit
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        verify(exactly = 1) { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) }

        val message = viewModel.errorMessage.value
        assertEquals(somethingWrong, message)
        assert(!viewModel.refreshing.value)
        assert(viewModel.dataReady.value == null)
    }

    @Test
    fun `getCourseStructure success with internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        every { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) } returns Unit
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        verify(exactly = 1) { analytics.logEvent(CourseAnalyticsEvent.DASHBOARD.eventName, any()) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.dataReady.value != null)
    }

    @Test
    fun `getCourseStructure success without internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery {
            courseApi.getCourseStructure(any(), any(), any(), any())
        } returns courseStructureModel
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 0) { courseApi.getCourseStructure(any(), any(), any(), any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
        assert(viewModel.dataReady.value != null)
    }

    @Test
    fun `updateData no internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        coEvery { interactor.getCourseStructure(any(), true) } throws UnknownHostException()
        coEvery { notifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        val message = viewModel.errorMessage.value
        assertEquals(noInternet, message)
        assert(!viewModel.refreshing.value)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            "",
            "",
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        coEvery { interactor.getCourseStructure(any(), true) } throws Exception()
        coEvery { notifier.send(CourseStructureUpdated("")) } returns Unit
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
            "",
            config,
            interactor,
            calendarManager,
            resourceManager,
            notifier,
            networkConnection,
            corePreferences,
            coursePreferences,
            analytics,
            imageProcessor,
            courseRouter
        )
        coEvery { interactor.getCourseStructure(any(), true) } returns courseStructure
        coEvery { notifier.send(CourseStructureUpdated("")) } returns Unit
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any(), true) }

        assert(viewModel.errorMessage.value == null)
        assert(!viewModel.refreshing.value)
    }
}
