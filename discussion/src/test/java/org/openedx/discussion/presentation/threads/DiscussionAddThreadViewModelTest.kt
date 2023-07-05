package org.openedx.discussion.presentation.threads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.data.storage.PreferencesManager
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.core.system.ResourceManager
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionProfile
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionAddThreadViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val preferencesManager = mockk<PreferencesManager>()
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

    //region mockComment

    private val mockComment = DiscussionComment(
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
        false,
        "",
        "",
        "",
        21,
        emptyList(),
        null,
        emptyMap()
    )

    private val mockCommentAdded = DiscussionComment(
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
        false,
        "",
        "",
        "",
        21,
        emptyList(),
        null,
        mapOf("" to DiscussionProfile(ProfileImage("", "", "", "", false)))
    )

    //endregion

    //region mockTopic

    private val mockTopic = Topic(
        id = "",
        name = "All Topics",
        threadListUrl = "",
        children = emptyList()
    )

    val topics = listOf(
        mockTopic.copy(id = "0"),
        mockTopic.copy(id = "1"),
        mockTopic.copy(id = "2")
    )

    //endregion

    private val comments = listOf(
        mockComment.copy(id = "0"), mockComment.copy(id = "1")
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

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
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

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
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
        } returns mockThread

        viewModel.createThread("", "", "", "", false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createThread(any(), any(), any(), any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
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