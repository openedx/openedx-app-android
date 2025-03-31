package org.openedx.dates

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.CourseDatesResponse
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.dates.domain.interactor.DatesInteractor
import org.openedx.dates.presentation.DatesAnalytics
import org.openedx.dates.presentation.DatesRouter
import org.openedx.dates.presentation.dates.DatesViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DatesViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val datesRouter = mockk<DatesRouter>(relaxed = true)
    private val networkConnection = mockk<NetworkConnection>()
    private val resourceManager = mockk<ResourceManager>()
    private val datesInteractor = mockk<DatesInteractor>()
    private val corePreferences = mockk<CorePreferences>()
    private val calendarSyncScheduler = mockk<CalendarSyncScheduler>()
    private val analytics = mockk<DatesAnalytics>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        // By default, assume we have an internet connection
        every { networkConnection.isOnline() } returns true
        every { corePreferences.isRelativeDatesEnabled } returns true
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery { datesInteractor.preloadFirstPageCachedDates() } returns null
        coEvery { datesInteractor.getUserDatesFromCache() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetchDates online with pagination`() = runTest {
        // Create a dummy CourseDate; grouping is done inside the view model so the exact grouping is not under test.
        val courseDate: CourseDate = mockk(relaxed = true)
        val courseDatesResponse = CourseDatesResponse(
            count = 10,
            next = "",
            previous = "",
            results = listOf(courseDate)
        )
        coEvery { datesInteractor.getUserDates(1) } returns courseDatesResponse

        // Instantiate the view model; fetchDates is called in init.
        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences,
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { datesInteractor.getUserDates(1) }
        // Since next is not null and page (1) != count (10), canLoadMore should be true.
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun `init fetchDates offline uses cache`() = runTest {
        every { networkConnection.isOnline() } returns false
        val cachedCourseDate: CourseDate = mockk(relaxed = true)
        coEvery { datesInteractor.getUserDatesFromCache() } returns listOf(cachedCourseDate)

        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { datesInteractor.getUserDatesFromCache() }
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun `fetchDates unknown error emits unknown error message`() =
        runTest(UnconfinedTestDispatcher()) {
            every { networkConnection.isOnline() } returns true

            val viewModel = DatesViewModel(
                datesRouter,
                networkConnection,
                resourceManager,
                datesInteractor,
                analytics,
                calendarSyncScheduler,
                corePreferences
            )
            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }
            advanceUntilIdle()

            assertEquals(somethingWrong, message.await()?.message)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `fetchDates internet error emits no connection message`() =
        runTest(UnconfinedTestDispatcher()) {
            every { networkConnection.isOnline() } returns true
            coEvery { datesInteractor.getUserDates(any()) } throws UnknownHostException()

            val viewModel = DatesViewModel(
                datesRouter,
                networkConnection,
                resourceManager,
                datesInteractor,
                analytics,
                calendarSyncScheduler,
                corePreferences
            )
            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }
            advanceUntilIdle()

            assertEquals(noInternet, message.await()?.message)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `shiftDueDate success`() = runTest {
        every { networkConnection.isOnline() } returns true
        // Prepare a dummy CourseDate that qualifies as past due and is marked as relative.
        val courseDate: CourseDate = mockk(relaxed = true) {
            every { relative } returns true
            every { courseId } returns "course-123"
            // Set dueDate to yesterday.
            every { dueDate } returns Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }
        val courseDatesResponse = CourseDatesResponse(
            count = 1,
            next = null,
            previous = null,
            results = listOf(courseDate)
        )
        coEvery { datesInteractor.getUserDates(1) } returns courseDatesResponse
        // When refreshData is triggered from shiftDueDate, return the same response.
        coEvery { datesInteractor.getUserDates(any()) } returns courseDatesResponse

        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        advanceUntilIdle()

        viewModel.shiftDueDate()
        advanceUntilIdle()

        coVerify { datesInteractor.shiftDueDate() }
        // isShiftDueDatesPressed should be reset to false after processing.
        assertFalse(viewModel.uiState.value.isShiftDueDatesPressed)
    }

    @Test
    fun `shiftDueDate error emits error message and resets flag`() =
        runTest(UnconfinedTestDispatcher()) {
            every { networkConnection.isOnline() } returns true
            val courseDate: CourseDate = mockk(relaxed = true) {
                every { relative } returns true
                every { courseId } returns "course-123"
                every { dueDate } returns Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            }
            val courseDatesResponse = CourseDatesResponse(
                count = 1,
                next = null,
                previous = null,
                results = listOf(courseDate)
            )
            coEvery { datesInteractor.getUserDates(1) } returns courseDatesResponse
            coEvery { datesInteractor.shiftDueDate() } throws Exception()

            val viewModel = DatesViewModel(
                datesRouter,
                networkConnection,
                resourceManager,
                datesInteractor,
                analytics,
                calendarSyncScheduler,
                corePreferences
            )
            advanceUntilIdle()

            viewModel.shiftDueDate()
            val message = async {
                withTimeoutOrNull(5000) {
                    viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
                }
            }
            advanceUntilIdle()

            assertEquals(somethingWrong, message.await()?.message)
            assertFalse(viewModel.uiState.value.isShiftDueDatesPressed)
        }

    @Test
    fun `onSettingsClick navigates to settings`() = runTest {
        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        val fragmentManager = mockk<FragmentManager>(relaxed = true)

        viewModel.onSettingsClick(fragmentManager)
        verify { datesRouter.navigateToSettings(fragmentManager) }
    }

    @Test
    fun `navigateToCourseOutline calls router with correct parameters`() = runTest {
        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        val fragmentManager = mockk<FragmentManager>(relaxed = true)
        val courseDate: CourseDate = mockk(relaxed = true) {
            every { courseId } returns "course-123"
            every { courseName } returns "Test Course"
            every { firstComponentBlockId } returns "block-1"
        }

        viewModel.navigateToCourseOutline(fragmentManager, courseDate)
        verify {
            datesRouter.navigateToCourseOutline(
                fm = fragmentManager,
                courseId = "course-123",
                courseTitle = "Test Course",
                openTab = "",
                resumeBlockId = "block-1"
            )
        }
    }

    @Test
    fun `fetchMore calls fetchDates when allowed`() = runTest {
        every { networkConnection.isOnline() } returns true
        val courseDate: CourseDate = mockk(relaxed = true)
        val courseDatesResponse = CourseDatesResponse(
            count = 10,
            next = "",
            previous = "",
            results = listOf(courseDate)
        )

        // Initial fetch on page 1.
        coEvery { datesInteractor.getUserDates(1) } returns courseDatesResponse
        // For subsequent fetch, we return a similar response.
        coEvery { datesInteractor.getUserDates(any()) } returns courseDatesResponse

        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        advanceUntilIdle()

        viewModel.fetchMore()
        advanceUntilIdle()

        // Expect two calls (one from init and one from fetchMore)
        coVerify(exactly = 2) { datesInteractor.getUserDates(any()) }
    }

    @Test
    fun `refreshData calls fetchDates with refresh true`() = runTest {
        every { networkConnection.isOnline() } returns true
        val courseDate: CourseDate = mockk(relaxed = true)
        val courseDatesResponse = CourseDatesResponse(
            count = 1,
            next = null,
            previous = null,
            results = listOf(courseDate)
        )
        // Initial fetch.
        coEvery { datesInteractor.getUserDates(1) } returns courseDatesResponse
        // For refresh, return the same response.
        coEvery { datesInteractor.getUserDates(any()) } returns courseDatesResponse

        val viewModel = DatesViewModel(
            datesRouter,
            networkConnection,
            resourceManager,
            datesInteractor,
            analytics,
            calendarSyncScheduler,
            corePreferences
        )
        advanceUntilIdle()

        viewModel.refreshData()
        advanceUntilIdle()

        // Two calls: one on init, one on refresh.
        coVerify(exactly = 2) { datesInteractor.getUserDates(any()) }
        // After refresh, isRefreshing should be false.
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
