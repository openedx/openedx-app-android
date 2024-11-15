package org.openedx.discussion.presentation.responses

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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Pagination
import org.openedx.core.extension.LinkedImageText
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.CommentsData
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionResponsesViewModelTest {

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

    val mockThread = org.openedx.discussion.domain.model.Thread(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        LinkedImageText("", emptyMap(), emptyMap(), emptyList()),
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

    //region mockComment

    private val mockComment = DiscussionComment(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        LinkedImageText("", emptyMap(), emptyMap(), emptyList()),
        false,
        true,
        20,
        emptyList(),
        false,
        "",
        "",
        false,
        "",
        "",
        "",
        21,
        emptyList(),
        null,
        emptyMap()
    )

    //endregion

    private val comments = listOf(
        mockComment.copy(id = "0"),
        mockComment.copy(id = "1")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
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
    fun `loadCommentResponses no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } throws UnknownHostException()

        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Loading)
    }

    @Test
    fun `loadCommentResponses unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } throws Exception()

        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Loading)
    }

    @Test
    fun `loadCommentResponses success with next page`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `loadCommentResponses success without next page`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `fetchMore not load`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `fetchMore load success`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.getCommentsResponses(any(), eq(2)) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCommentsResponses(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } throws UnknownHostException()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
    }

    @Test
    fun `setCommentUpvoted unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } throws Exception()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
    }

    @Test
    fun `setCommentUpvoted success without comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } returns mockComment.copy(id = "0")
        viewModel.updateCommentResponses()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentUpvoted success with comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentVoted(any(), any()) } returns mockComment.copy(id = "2")
        viewModel.updateCommentResponses()
        viewModel.setCommentUpvoted("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentReported no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } throws UnknownHostException()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
    }

    @Test
    fun `setCommentReported unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } throws Exception()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
    }

    @Test
    fun `setCommentReported success without comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } returns mockComment.copy(id = "0")
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `setCommentReported success with comments`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.setCommentFlagged(any(), any()) } returns mockComment.copy(id = "0")

        viewModel.updateCommentResponses()
        viewModel.setCommentReported("", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `createComment no internet connection exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any()) } throws UnknownHostException()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(noInternet, message?.message)
    }

    @Test
    fun `createComment unknown exception`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any()) } throws Exception()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `createComment success`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        assert(viewModel.uiMessage.value != null)
        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionCommentAdded`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded`() = runTest {
        coEvery { interactor.getCommentsResponses(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel = DiscussionResponsesViewModel(
            interactor,
            resourceManager,
            notifier,
            mockComment.copy(id = "0")
        )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.user?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionResponsesUIState.Success)
    }
}
