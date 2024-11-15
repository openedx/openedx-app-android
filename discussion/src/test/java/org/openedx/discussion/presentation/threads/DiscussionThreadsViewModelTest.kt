package org.openedx.discussion.presentation.threads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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
import org.openedx.core.R
import org.openedx.core.domain.model.Pagination
import org.openedx.core.extension.TextConverter
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.ThreadsData
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionThreadsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val notifier = mockk<DiscussionNotifier>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    //region mockThread

    val mockThread = org.openedx.discussion.domain.model.Thread(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        TextConverter.textToLinkedImageText(""),
        false,
        true,
        20,
        emptyList(),
        false,
        "",
        "",
        "",
        "",
        DiscussionType.DISCUSSION,
        "",
        "",
        "Discussion title long Discussion title long good item",
        true,
        false,
        true,
        21,
        4,
        false,
        false,
        mapOf(),
        0,
        false,
        false
    )

    //endregion

    private val threads = listOf<org.openedx.discussion.domain.model.Thread>(
        mockThread.copy(id = "0"),
        mockThread.copy(id = "1"),
        mockThread.copy(id = "2")
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
        clearAllMocks()
    }

    @Test
    fun `getThreadByType AllThreads no internet connection`() = runTest {
        coEvery {
            interactor.getAllThreads(
                any(),
                any(),
                any(),
                any()
            )
        } throws UnknownHostException()
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.ALL_POSTS
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAllThreads(any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType AllThreads unknown exception`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.ALL_POSTS
        )
        coEvery { interactor.getAllThreads(any(), any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAllThreads(any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType AllThreads success`() = runTest {
        coEvery { interactor.getAllThreads("", any(), null, range(1, 2)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.getAllThreads("", any(), null, eq(3)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.ALL_POSTS
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAllThreads(any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `getThreadByType FollowingPosts no internet connection`() = runTest {
        coEvery {
            interactor.getFollowingThreads(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws UnknownHostException()
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.FOLLOWING_POSTS
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getFollowingThreads(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType FollowingPosts unknown exception`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.FOLLOWING_POSTS
        )
        coEvery {
            interactor.getFollowingThreads(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getFollowingThreads(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType FollowingPosts success`() = runTest {
        coEvery {
            interactor.getFollowingThreads(
                "",
                any(),
                any(),
                null,
                range(1, 2)
            )
        } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery {
            interactor.getFollowingThreads(
                "",
                any(),
                any(),
                null,
                eq(3)
            )
        } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.FOLLOWING_POSTS
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getFollowingThreads(any(), any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `getThreadByType Topic no internet connection`() = runTest {
        coEvery {
            interactor.getThreads(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws UnknownHostException()
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreads(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType Topic unknown exception`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        coEvery { interactor.getThreads(any(), any(), any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreads(any(), any(), any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Loading)
    }

    @Test
    fun `getThreadByType Topic success`() = runTest {
        coEvery { interactor.getThreads("", any(), any(), null, range(1, 2)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.getThreads("", any(), any(), null, eq(3)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreads(any(), any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `filterThreads All posts`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        coEvery { interactor.getThreads(any(), any(), any(), any(), any()) } returns ThreadsData(
            threads,
            "",
            pagination = Pagination(10, "", 2, "")
        )
        viewModel.filterThreads(FilterType.ALL_POSTS.value)
        advanceUntilIdle()
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `filterThreads UNREAD`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        coEvery { interactor.getThreads(any(), any(), any(), any(), any()) } returns ThreadsData(
            threads,
            "",
            pagination = Pagination(10, "", 2, "")
        )
        viewModel.filterThreads(FilterType.UNREAD.value)
        advanceUntilIdle()
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `filterThreads UNANSWERED`() = runTest {
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        coEvery { interactor.getThreads(any(), any(), any(), any(), any()) } returns ThreadsData(
            threads,
            "",
            pagination = Pagination(10, "", 2, "")
        )
        viewModel.filterThreads(FilterType.UNANSWERED.value)
        advanceUntilIdle()
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `updateThread Topic success`() = runTest {
        coEvery { interactor.getThreads("", any(), any(), null, range(1, 2)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.getThreads("", any(), any(), null, eq(3)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )
        viewModel.updateThread("")
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreads(any(), any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionThreadsUIState.Threads)
    }

    @Test
    fun `DiscussionThreadAdded notifier test`() = runTest {
        coEvery { interactor.getThreads("", any(), any(), null, range(1, 2)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.getThreads("", any(), any(), null, eq(3)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        coEvery {
            notifier.notifier
        } returns flow {
            delay(100)
            emit(DiscussionThreadAdded())
        }
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.updateThread("date")
        advanceUntilIdle()

        coVerify(exactly = 3) { interactor.getThreads(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `DiscussionThreadDataChanged notifier test`() = runTest {
        coEvery { interactor.getThreads("", any(), any(), null, range(1, 2)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.getThreads("", any(), any(), null, eq(3)) } returns ThreadsData(
            threads,
            "",
            Pagination(10, "", 4, "1")
        )
        coEvery {
            notifier.notifier
        } returns flow {
            delay(100)
            emit(DiscussionThreadDataChanged(mockThread.copy(id = "1")))
        }
        val viewModel = DiscussionThreadsViewModel(
            interactor,
            resourceManager,
            notifier,
            "",
            "",
            DiscussionTopicsViewModel.TOPIC
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.updateThread("date")
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreads(any(), any(), any(), any(), any()) }
    }
}
