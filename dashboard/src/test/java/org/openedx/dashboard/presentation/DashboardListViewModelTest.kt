package org.openedx.dashboard.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
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
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.Pagination
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardListViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DashboardInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val discoveryNotifier = mockk<DiscoveryNotifier>()
    private val analytics = mockk<DashboardAnalytics>()
    private val appNotifier = mockk<AppNotifier>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val dashboardCourseList = DashboardCourseList(
        Pagination(10, "", 3, ""),
        listOf(mockk())
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { appNotifier.notifier } returns emptyFlow()
        every { config.getApiHostURL() } returns "http://localhost:8000"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourses no internet connection`() = runTest {
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `getCourses unknown error`() = runTest {
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `getCourses from network`() = runTest {
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        coEvery { interactor.getEnrolledCoursesFromCache() } returns listOf(mockk())
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `getCourses from network with next page`() = runTest {
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList.copy(
            Pagination(
                10,
                "2",
                2,
                ""
            )
        )
        coEvery { interactor.getEnrolledCoursesFromCache() } returns listOf(mockk())

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `getCourses from cache`() = runTest {
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getEnrolledCoursesFromCache() } returns listOf(mockk())
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `updateCourses no internet error`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        coEvery { interactor.getEnrolledCourses(any()) } throws UnknownHostException()
        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `updateCourses unknown exception`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        coEvery { interactor.getEnrolledCourses(any()) } throws Exception()
        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `updateCourses success`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `updateCourses success with next page`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList.copy(
            Pagination(
                10,
                "2",
                2,
                ""
            )
        )
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }
        verify(exactly = 1) { appNotifier.notifier }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `CourseDashboardUpdate notifier test`() = runTest {
        coEvery { discoveryNotifier.notifier } returns flow { emit(CourseDashboardUpdate()) }
        val viewModel = DashboardListViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            discoveryNotifier,
            analytics,
            appNotifier
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        verify(exactly = 1) { appNotifier.notifier }
    }
}
