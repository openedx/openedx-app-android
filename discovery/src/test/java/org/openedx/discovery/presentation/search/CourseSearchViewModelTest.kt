package org.openedx.discovery.presentation.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Pagination
import org.openedx.discovery.DiscoveryMocks
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.CourseList
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.captureUiMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@OptIn(ExperimentalCoroutinesApi::class)
class CourseSearchViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val config = mockk<Config>()
    private val corePreferences = mockk<CorePreferences>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscoveryInteractor>()
    private val analytics = mockk<DiscoveryAnalytics>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(foundationR.string.foundation_error_no_connection) } returns noInternet
        every { resourceManager.getString(foundationR.string.foundation_error_unknown_error) } returns somethingWrong
        every { config.getApiHostURL() } returns "http://localhost:8000"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search empty query`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)

        viewModel.search("")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value as CourseSearchUIState.Courses

        assert(uiState.courses.isEmpty())
        assert(uiState.numCourses == 0)
        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
    }

    @Test
    fun `search query no internet connection exception`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), any()) } throws UnknownHostException()

        viewModel.search("course")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesListByQuery(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(viewModel.uiState.value is CourseSearchUIState.Loading)
        assert((message.await() as UIMessage.SnackBarMessage).message == noInternet)
    }

    @Test
    fun `search query unknown exception`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), any()) } throws Exception()

        viewModel.search("course")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesListByQuery(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(viewModel.uiState.value is CourseSearchUIState.Loading)
        assert((message.await() as UIMessage.SnackBarMessage).message == somethingWrong)
    }

    @Test
    fun `search query success without next page`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), any()) } returns CourseList(
            Pagination(
                10,
                "",
                5,
                ""
            ),
            emptyList()
        )
        every { analytics.discoveryCourseSearchEvent(any(), any()) } returns Unit

        viewModel.search("course")
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesListByQuery(any(), any()) }
        verify(exactly = 1) { analytics.discoveryCourseSearchEvent(any(), any()) }

        assert(viewModel.uiState.value is CourseSearchUIState.Courses)
        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `search query success with next page and fetch`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), eq(1)) } returns CourseList(
            Pagination(
                10,
                "2",
                5,
                ""
            ),
            DiscoveryMocks.courses(2)
        )
        coEvery {
            interactor.getCoursesListByQuery(
                any(),
                not(1)
            )
        } returns CourseList(Pagination(10, "", 5, ""), listOf(DiscoveryMocks.course))
        every { analytics.discoveryCourseSearchEvent(any(), any()) } returns Unit

        viewModel.search("course")
        delay(1000)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesListByQuery(any(), any()) }
        verify(exactly = 2) { analytics.discoveryCourseSearchEvent(any(), any()) }

        assert(viewModel.uiState.value is CourseSearchUIState.Courses)
        assert((viewModel.uiState.value as CourseSearchUIState.Courses).courses.size == 3)
        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `search query success with next page and fetch, update`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), eq(1)) } returns CourseList(
            Pagination(
                10,
                "2",
                5,
                ""
            ),
            DiscoveryMocks.courses(2)
        )
        coEvery {
            interactor.getCoursesListByQuery(
                any(),
                not(1)
            )
        } returns CourseList(Pagination(10, "0", 5, ""), listOf(DiscoveryMocks.course))
        every { analytics.discoveryCourseSearchEvent(any(), any()) } returns Unit

        viewModel.search("course")
        delay(1000)
        viewModel.fetchMore()
        viewModel.updateSearchQuery()
        advanceUntilIdle()

        coVerify(exactly = 3) { interactor.getCoursesListByQuery(any(), any()) }
        verify(exactly = 3) { analytics.discoveryCourseSearchEvent(any(), any()) }

        assert(viewModel.uiState.value is CourseSearchUIState.Courses)
        assert((viewModel.uiState.value as CourseSearchUIState.Courses).courses.size == 2)
        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `search query update in empty state`() = runTest {
        val viewModel =
            CourseSearchViewModel(config, corePreferences, interactor, resourceManager, analytics)
        coEvery { interactor.getCoursesListByQuery(any(), eq(1)) } returns CourseList(
            Pagination(
                10,
                "2",
                5,
                ""
            ),
            DiscoveryMocks.courses(2)
        )

        viewModel.updateSearchQuery()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCoursesListByQuery(any(), any()) }

        assert(viewModel.uiState.value is CourseSearchUIState.Courses)
        assert((viewModel.uiState.value as CourseSearchUIState.Courses).courses.isEmpty())
        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.isUpdating.value == null)
    }
}
