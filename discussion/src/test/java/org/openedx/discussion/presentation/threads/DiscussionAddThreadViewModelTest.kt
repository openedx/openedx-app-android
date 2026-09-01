package org.openedx.discussion.presentation.threads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.discussion.DiscussionMocks
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionAddThreadViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val notifier = mockk<DiscussionNotifier>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    //region mockThread
    //endregion

    //region mockTopic
    //endregion

    val topics = listOf(
        DiscussionMocks.topic.copy(id = "0", name = "All Topics"),
        DiscussionMocks.topic.copy(id = "1", name = "All Topics"),
        DiscussionMocks.topic.copy(id = "2", name = "All Topics")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every {
            resourceManager.getString(foundationR.string.foundation_error_no_connection)
        } returns noInternet
        every {
            resourceManager.getString(foundationR.string.foundation_error_unknown_error)
        } returns somethingWrong
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun DiscussionAddThreadViewModel.lastUiMessage(): UIMessage? {
        return uiMessage.replayCache.lastOrNull()
    }

    @Test
    fun `createThread no internet connection exception`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws UnknownHostException()

        viewModel.createThread("", "", "", "", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any(), any()) }

        val message = viewModel.lastUiMessage() as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.newThread.value == null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `createThread unknown exception`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws Exception()

        viewModel.createThread("", "", "", "", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any(), any()) }

        val message = viewModel.lastUiMessage() as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.newThread.value == null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `createThread success`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery {
            interactor.createThread(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns DiscussionMocks.thread

        viewModel.createThread("", "", "", "", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any(), any()) }

        assert(viewModel.lastUiMessage() == null)
        assert(viewModel.newThread.value != null)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `sendThreadAdded notifier test`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { notifier.send(mockk<DiscussionThreadAdded>()) }
        viewModel.sendThreadAdded()
        advanceUntilIdle()
    }

    @Test
    fun `getHandledTopicById existed id`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.getCachedTopics(any()) } returns topics

        advanceUntilIdle()

        assert(viewModel.getHandledTopicById("1").second == "1")
    }

    @Test
    fun `getHandledTopicById  no existed id`() = runTest {
        val viewModel = DiscussionAddThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.getCachedTopics(any()) } returns topics

        advanceUntilIdle()

        assert(viewModel.getHandledTopicById("10").second == "0")
    }
}
