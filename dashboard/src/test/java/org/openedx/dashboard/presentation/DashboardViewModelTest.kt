package org.openedx.dashboard.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.Pagination
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DashboardViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DashboardInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = mockk<CourseNotifier>()
    private val analytics = mockk<DashboardAnalytics>()

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourses no internet connection`() = runTest {
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } throws UnknownHostException()

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `getCourses unknown error`() = runTest {
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } throws Exception()

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `getCourses from network`() = runTest {
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        coEvery { interactor.getEnrolledCoursesFromCache() } returns listOf(mockk())

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `getCourses from network with next page`() = runTest {
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)
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

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `getCourses from cache`() = runTest {
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getEnrolledCoursesFromCache() } returns listOf(mockk())

        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCoursesFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `updateCourses no internet error`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        coEvery { interactor.getEnrolledCourses(any()) } throws UnknownHostException()
        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `updateCourses unknown exception`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList

        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        coEvery { interactor.getEnrolledCourses(any()) } throws Exception()
        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Loading)
    }

    @Test
    fun `updateCourses success`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `updateCourses success with next page`() = runTest {
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getEnrolledCourses(any()) } returns dashboardCourseList.copy(Pagination(10,"2",2,""))
        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        viewModel.updateCourses()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getEnrolledCourses(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCoursesFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DashboardUIState.Courses)
    }

    @Test
    fun `CourseDashboardUpdate notifier test`() = runTest {
        coEvery { notifier.notifier } returns flow { emit(CourseDashboardUpdate()) }

        val viewModel = DashboardViewModel(networkConnection, interactor, resourceManager, notifier, analytics)

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getEnrolledCourses(any()) }
    }


}