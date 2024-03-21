package org.openedx.discussion.presentation.topics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.openedx.core.UIMessage
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionTopicsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val analytics = mockk<DiscussionAnalytics>()
    private val networkConnection = mockk<NetworkConnection>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

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
    fun `getCourseTopics no internet exception`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Loading)
    }

    @Test
    fun `getCourseTopics unknown exception`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Loading)
    }

    @Test
    fun `getCourseTopics success`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
    }

    @Test
    fun `updateCourseTopics no internet exception`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        viewModel.updateCourseTopics(withSwipeRefresh = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `updateCourseTopics unknown exception`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        viewModel.updateCourseTopics(withSwipeRefresh = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `updateCourseTopics success`() = runTest {
        val viewModel =
            DiscussionTopicsViewModel(interactor, resourceManager, analytics, networkConnection, "")

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
        viewModel.updateCourseTopics(withSwipeRefresh = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
        assert(viewModel.isUpdating.value == false)
    }

}