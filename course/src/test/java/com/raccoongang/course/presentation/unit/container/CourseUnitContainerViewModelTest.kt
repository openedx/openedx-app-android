package com.raccoongang.course.presentation.unit.container

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.BlockCounts
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.domain.model.CoursewareAccess
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.course.domain.interactor.CourseInteractor
import com.raccoongang.course.presentation.CourseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class CourseUnitContainerViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val interactor = mockk<CourseInteractor>()
    private val notifier = mockk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()

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
            completion = 0.0
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
            completion = 0.0
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
            completion = 0.0
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getBlocks no internet connection exception`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")

        every { interactor.getCourseStructureFromCache() } throws UnknownHostException()
        every { interactor.getCourseStructureForVideos() } throws UnknownHostException()

        viewModel.loadBlocks(CourseViewMode.FULL)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")

        every { interactor.getCourseStructureFromCache() } throws UnknownHostException()
        every { interactor.getCourseStructureForVideos() } throws UnknownHostException()

        viewModel.loadBlocks(CourseViewMode.FULL)
        advanceUntilIdle()

        verify(exactly = 1) { interactor.getCourseStructureFromCache() }
    }

    @Test
    fun `getBlocks unknown success`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")

        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
    }

    @Test
    fun `setupCurrentIndex`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id")
        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
    }

    @Test
    fun `getCurrentBlock test`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
        assert(viewModel.getCurrentBlock().id == "id")
    }

    @Test
    fun `moveToPrevBlock null`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
        assert(viewModel.moveToPrevBlock() == null)
    }

    @Test
    fun `moveToPrevBlock not null`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id3")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
        assert(viewModel.moveToPrevBlock() == null)
    }

    @Test
    fun `moveToNextBlock null`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id3")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
        assert(viewModel.moveToNextBlock() == null)
    }

    @Test
    fun `moveToNextBlock not null`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
        assert(viewModel.moveToNextBlock() != null)
    }

    @Test
    fun `currentIndex isLastIndex`() = runTest {
        val viewModel = CourseUnitContainerViewModel(interactor, notifier, analytics, "")
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        every { interactor.getCourseStructureForVideos() } returns courseStructure

        viewModel.loadBlocks(CourseViewMode.VIDEOS)
        viewModel.setupCurrentIndex("id3")

        advanceUntilIdle()

        verify(exactly = 0) { interactor.getCourseStructureFromCache() }
        verify(exactly = 1) { interactor.getCourseStructureForVideos() }
    }

}