package org.openedx.discussion.presentation.topics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionTopicsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val analytics = mockk<DiscussionAnalytics>()
    private val router = mockk<DiscussionRouter>()
    private val courseNotifier = mockk<CourseNotifier>()

    private val noInternet = "Slow or no internet connection"

    private val mockTopic = Topic(
        id = "",
        name = "All Topics",
        threadListUrl = "",
        children = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { courseNotifier.notifier } returns flowOf(CourseLoading(false))
        coEvery { courseNotifier.send(any<CourseLoading>()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseTopics no internet exception`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assertEquals(noInternet, message.await()?.message)
    }

    @Test
    fun `getCourseTopics unknown exception`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(message.await()?.message.isNullOrEmpty())
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Error)
    }

    @Test
    fun `getCourseTopics success`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } returns listOf(mockTopic, mockTopic)
        advanceUntilIdle()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(message.await()?.message.isNullOrEmpty())
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
    }

    @Test
    fun `updateCourseTopics no internet exception`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } throws UnknownHostException()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assertEquals(noInternet, message.await()?.message)
    }

    @Test
    fun `updateCourseTopics unknown exception`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(message.await()?.message.isNullOrEmpty())
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Error)
    }

    @Test
    fun `updateCourseTopics success`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(
            "id",
            "",
            interactor,
            resourceManager,
            analytics,
            courseNotifier,
            router
        )

        coEvery { interactor.getCourseTopics(any()) } returns listOf(mockTopic, mockTopic)
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assert(message.await()?.message.isNullOrEmpty())
        assert(viewModel.uiState.value is DiscussionTopicsUIState.Topics)
    }
}
