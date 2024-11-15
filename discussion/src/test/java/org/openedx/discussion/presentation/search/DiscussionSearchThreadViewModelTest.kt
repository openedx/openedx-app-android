package org.openedx.discussion.presentation.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionSearchThreadViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val notifier = mockk<DiscussionNotifier>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    //region thread

    private val mockThread = org.openedx.discussion.domain.model.Thread(
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
    fun `search empty query`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")

        viewModel.searchThreads("")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value as DiscussionSearchThreadUIState.Threads

        assert(uiState.data.isEmpty())
        assert(uiState.count == 0)
        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `search query no internet connection exception`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), any()) } throws UnknownHostException()

        viewModel.searchThreads("course")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.searchThread(any(), any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Loading)
        assert(message.message == noInternet)
    }

    @Test
    fun `search query unknown exception`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), any()) } throws Exception()

        viewModel.searchThreads("course")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.searchThread(any(), any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Loading)
        assert(message.message == somethingWrong)
    }

    @Test
    fun `search query success without next page`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), any()) } returns ThreadsData(
            emptyList(),
            "",
            Pagination(
                10,
                "",
                5,
                ""
            )
        )

        viewModel.searchThreads("course")
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.searchThread(any(), any(), any()) }

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `search query success with next page and fetch`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), eq(1)) } returns ThreadsData(
            listOf(mockThread, mockThread),
            "",
            Pagination(
                10,
                "2",
                5,
                ""
            )
        )
        coEvery {
            interactor.searchThread(
                any(),
                any(),
                not(1)
            )
        } returns ThreadsData(listOf(mockThread), "", Pagination(10, "", 5, ""))

        viewModel.searchThreads("course")
        delay(1000)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.searchThread(any(), any(), any()) }

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert((viewModel.uiState.value as DiscussionSearchThreadUIState.Threads).data.size == 3)
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `search query success with next page and fetch, update`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), eq(1)) } returns ThreadsData(
            listOf(mockThread, mockThread),
            "",
            Pagination(
                10,
                "2",
                5,
                ""
            )
        )
        coEvery {
            interactor.searchThread(
                any(),
                any(),
                not(1)
            )
        } returns ThreadsData(listOf(mockThread), "", Pagination(10, "0", 5, ""))

        viewModel.searchThreads("course")
        delay(1000)
        viewModel.fetchMore()
        viewModel.updateSearchQuery()
        advanceUntilIdle()

        coVerify(exactly = 3) { interactor.searchThread(any(), any(), any()) }

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert((viewModel.uiState.value as DiscussionSearchThreadUIState.Threads).data.size == 2)
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `search query update in empty state`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), eq(1)) } returns ThreadsData(
            listOf(mockThread, mockThread),
            "",
            Pagination(
                10,
                "2",
                5,
                ""
            )
        )

        viewModel.updateSearchQuery()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.searchThread(any(), any(), any()) }

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert((viewModel.uiState.value as DiscussionSearchThreadUIState.Threads).data.isEmpty())
        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == null)
    }

    @Test
    fun `notifier DiscussionThreadDataChanged with empty list`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")

        coEvery {
            notifier.notifier
        } returns flow {
            delay(100)
            emit(DiscussionThreadDataChanged(mockThread.copy(id = "1")))
        }

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert((viewModel.uiState.value as DiscussionSearchThreadUIState.Threads).data.isEmpty())
    }

    @Test
    fun `notifier DiscussionThreadDataChanged with list`() = runTest {
        val viewModel = DiscussionSearchThreadViewModel(interactor, resourceManager, notifier, "")
        coEvery { interactor.searchThread(any(), any(), any()) } returns ThreadsData(
            listOf(mockThread.copy(id = "id")),
            "",
            Pagination(
                10,
                "",
                5,
                ""
            )
        )

        coEvery {
            notifier.notifier
        } returns flow {
            delay(1000)
            emit(DiscussionThreadDataChanged(mockThread.copy(id = "id")))
        }

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.searchThreads("course")
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.searchThread(any(), any(), any()) }

        assert(viewModel.uiState.value is DiscussionSearchThreadUIState.Threads)
        assert((viewModel.uiState.value as DiscussionSearchThreadUIState.Threads).data.size == 1)
    }
}
