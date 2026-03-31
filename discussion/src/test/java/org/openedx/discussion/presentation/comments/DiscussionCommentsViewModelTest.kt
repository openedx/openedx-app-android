package org.openedx.discussion.presentation.comments

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
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Pagination
import org.openedx.discussion.DiscussionMocks
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.CommentsData
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.system.notifier.DiscussionCommentAdded
import org.openedx.discussion.system.notifier.DiscussionCommentDataChanged
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.captureUiMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@Suppress("LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionCommentsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val preferencesManager = mockk<CorePreferences>()
    private val notifier = mockk<DiscussionNotifier>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val commentAddedSuccessfully = "Comment Successfully added"

    //region mockThread
    //endregion

    //region mockComment
    // endregion

    private val comments = listOf(
        DiscussionMocks.comment.copy(id = "0"),
        DiscussionMocks.comment.copy(id = "1")
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
        every {
            resourceManager.getString(org.openedx.discussion.R.string.discussion_comment_added)
        } returns commentAddedSuccessfully
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `getThreadComments no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } throws UnknownHostException()
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Loading)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `getThreadComments unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } throws Exception()
        every { resourceManager.getString(eq(DiscussionType.QUESTION.resId)) } returns ""
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread.copy(type = DiscussionType.QUESTION)
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 1) { interactor.getThreadQuestionComments(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Loading)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `getThreadComments success with next page`() = runTest {
        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionType.QUESTION.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread.copy(type = DiscussionType.QUESTION)
            )

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 1) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `getThreadComments success without next page`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `updateThreadComments success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        viewModel.updateThreadComments()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `fetchMore failure`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        delay(100)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `fetchMore success`() = runTest {
        coEvery { interactor.getThreadComments(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.getThreadComments(any(), eq(2)) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        delay(100)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `setThreadUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadVoted(any(), any()) } throws UnknownHostException()

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadUpvoted unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.setThreadVoted(any(), any()) } throws Exception()

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadUpvoted success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.setThreadVoted(any(), any()) } returns DiscussionMocks.thread

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.setCommentFlagged(any(), any()) } throws UnknownHostException()

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(noInternet == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )

        coEvery { interactor.setCommentFlagged(any(), any()) } throws Exception()

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(somethingWrong == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setCommentFlagged(any(), any()) } returns DiscussionMocks.comment.copy(
            id = "0"
        )

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setCommentVoted(any(), any()) } throws UnknownHostException()

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(noInternet == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentUpvoted unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setCommentVoted(any(), any()) } throws Exception()

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(somethingWrong == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentUpvoted success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery {
            interactor.setCommentVoted(
                any(),
                any()
            )
        } returns DiscussionMocks.comment.copy(id = "0")

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadFlagged(any(), any()) } throws UnknownHostException()

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(noInternet == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadFlagged(any(), any()) } throws Exception()

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(somethingWrong == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadFlagged(any(), any()) } returns DiscussionMocks.thread

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadFollowed no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        coEvery { interactor.setThreadFollowed(any(), any()) } throws UnknownHostException()

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(noInternet == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadFollowed unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadFollowed(any(), any()) } throws Exception()

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(somethingWrong == (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadFollowed success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { interactor.setThreadFollowed(any(), any()) } returns DiscussionMocks.thread

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `DiscussionCommentAdded notifier test all comments loaded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentAdded(DiscussionMocks.comment))
        }
        coEvery { notifier.send(DiscussionThreadDataChanged(DiscussionMocks.thread)) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `DiscussionCommentAdded notifier test all comments not loaded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentAdded(DiscussionMocks.comment))
        }
        coEvery { notifier.send(DiscussionThreadDataChanged(mockk())) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        val message = captureUiMessage(viewModel)
        assertEquals(
            commentAddedSuccessfully,
            (message.await() as? UIMessage.ToastMessage)?.message
        )
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `DiscussionCommentDataChanged notifier test `() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentDataChanged(DiscussionMocks.comment.copy(id = "0")))
        }
        coEvery { notifier.send(DiscussionCommentDataChanged(DiscussionMocks.comment)) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `createComment no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )
        coEvery { interactor.createComment(any(), any(), any()) } throws UnknownHostException()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `createComment unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )
        coEvery { interactor.createComment(any(), any(), any()) } throws Exception()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `createComment success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                notifier,
                DiscussionMocks.thread
            )
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(null, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionCommentAdded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()
    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded without next page`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns DiscussionMocks.thread
        every { resourceManager.getString(eq(DiscussionMocks.thread.type.resId)) } returns ""

        val viewModel = DiscussionCommentsViewModel(
            interactor,
            resourceManager,
            notifier,
            DiscussionMocks.thread
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns DiscussionMocks.comment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }
}
