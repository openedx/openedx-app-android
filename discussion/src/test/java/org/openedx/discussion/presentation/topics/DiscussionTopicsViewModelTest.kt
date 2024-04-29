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
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDataReady
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionRouter
import java.net.UnknownHostException
import java.util.Date

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
    private val somethingWrong = "Something went wrong"

    private val blocks = listOf(
        Block(
            id = "id",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.CHAPTER,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = listOf("1", "id1"),
            descendantsType = BlockType.HTML,
            completion = 0.0
        ),
        Block(
            id = "id1",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.HTML,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = listOf("id2"),
            descendantsType = BlockType.HTML,
            completion = 0.0
        ),
        Block(
            id = "id2",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.HTML,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = emptyList(),
            descendantsType = BlockType.HTML,
            completion = 0.0
        )
    )
    private val courseStructure = CourseStructure(
        root = "",
        blockData = blocks,
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        certificate = null,
        isSelfPaced = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { courseNotifier.notifier } returns flowOf(CourseDataReady(courseStructure))
        coEvery { courseNotifier.send(any<CourseLoading>()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseTopics no internet exception`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

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
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assertEquals(somethingWrong, message.await()?.message)
    }

    @Test
    fun `getCourseTopics success`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
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
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

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
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

        coEvery { interactor.getCourseTopics(any()) } throws Exception()
        val message = async {
            withTimeoutOrNull(5000) {
                viewModel.uiMessage.first() as? UIMessage.SnackBarMessage
            }
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseTopics(any()) }

        assertEquals(somethingWrong, message.await()?.message)
    }

    @Test
    fun `updateCourseTopics success`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = DiscussionTopicsViewModel(interactor, resourceManager, analytics, courseNotifier, router)

        coEvery { interactor.getCourseTopics(any()) } returns mockk()
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
