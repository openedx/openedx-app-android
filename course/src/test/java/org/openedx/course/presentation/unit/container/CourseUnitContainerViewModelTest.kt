package org.openedx.course.presentation.unit.container

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.BlockType
import org.openedx.core.config.Config
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseUnitContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val notifier = mockk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val networkConnection = mockk<NetworkConnection>()

    private val assignmentProgress = AssignmentProgress(
        assignmentType = "Homework",
        numPointsEarned = 1f,
        numPointsPossible = 3f,
        shortLabel = "HW1",
    )

    private val blocks = listOf(
        Block(
            id = "id",
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
            descendants = listOf("id2", "id1"),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        ),
        Block(
            id = "id1",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.VERTICAL,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = listOf("id2", "id"),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        ),
        Block(
            id = "id2",
            blockId = "blockId",
            lmsWebUrl = "lmsWebUrl",
            legacyWebUrl = "legacyWebUrl",
            studentViewUrl = "studentViewUrl",
            type = BlockType.SEQUENTIAL,
            displayName = "Block",
            graded = false,
            studentViewData = null,
            studentViewMultiDevice = false,
            blockCounts = BlockCounts(0),
            descendants = emptyList(),
            descendantsType = BlockType.HTML,
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
        ),
        Block(
            id = "id3",
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
            completion = 0.0,
            assignmentProgress = assignmentProgress,
            due = Date(),
            offlineDownload = null,
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
        isSelfPaced = false,
        progress = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getBlocks no internet connection exception`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)

        coEvery { interactor.getCourseStructure(any()) } throws UnknownHostException()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws UnknownHostException()

        viewModel.loadBlocks(CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)

        coEvery { interactor.getCourseStructure(any()) } throws UnknownHostException()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws UnknownHostException()

        viewModel.loadBlocks(CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
    }

    @Test
    fun `getBlocks unknown success`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)

        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }
    }

    @Test
    fun setupCurrentIndex() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id")
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }
    }

    @Test
    fun `getCurrentBlock test`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure("") }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos("") }
        assert(viewModel.getCurrentBlock().id == "id")
    }

    @Test
    fun `moveToPrevBlock null`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure("") }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos("") }
        assert(viewModel.moveToPrevBlock() == null)
    }

    @Test
    fun `moveToPrevBlock not null`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "id", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id1")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure("") }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos("") }
        assert(viewModel.moveToPrevBlock() != null)
    }

    @Test
    fun `moveToNextBlock null`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id3")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure("") }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos("") }
        assert(viewModel.moveToNextBlock() == null)
    }

    @Test
    fun `moveToNextBlock not null`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "id", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure("") } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos("") } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure("") }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos("") }
        assert(viewModel.moveToNextBlock() != null)
    }

    @Test
    fun `currentIndex isLastIndex`() = runTest {
        every { notifier.notifier } returns MutableSharedFlow()
        val viewModel =
            CourseUnitContainerViewModel("", "", config, interactor, notifier, analytics, networkConnection)
        coEvery { interactor.getCourseStructure(any()) } returns courseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS, "id3")

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }
    }
}
