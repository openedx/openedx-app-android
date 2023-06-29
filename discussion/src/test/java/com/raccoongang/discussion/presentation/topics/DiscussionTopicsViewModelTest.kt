package com.raccoongang.discussion.presentation.topics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.discussion.domain.interactor.DiscussionInteractor
import com.raccoongang.discussion.presentation.DiscussionAnalytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionTopicsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val analytics = mockk<DiscussionAnalytics>()

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
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        viewModel.getCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Loading)
    }

    @Test
    fun `getCourseTopics unknown exception`() = runTest {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        viewModel.getCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Loading)
    }

    @Test
    fun `getCourseTopics success`() = runTest {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
        viewModel.getCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
    }

    @Test
    fun `updateCourseTopics no internet exception`() = runTest {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `updateCourseTopics unknown exception`() = runTest {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `updateCourseTopics success`() = runTest {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics,"")

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
        viewModel.updateCourseTopics()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
        assert(viewModel.isUpdating.value == false)
    }

}