package org.openedx.course.presentation.unit.video

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

@OptIn(ExperimentalCoroutinesApi::class)
class VideoUnitViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val courseRepository = mockk<CourseRepository>()
    private val notifier = mockk<CourseNotifier>()
    private val networkConnection = mockk<NetworkConnection>()
    private val transcriptManager = mockk<TranscriptManager>()
    private val courseAnalytics = mockk<CourseAnalytics>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markBlockCompleted exception`() = runTest {
        val viewModel = VideoUnitViewModel(
            "",
            "",
            "",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics
        )
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } throws Exception()
        every {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        } returns Unit
        viewModel.markBlockCompleted("", "")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        }
    }

    @Test
    fun `markBlockCompleted success`() = runTest {
        val viewModel = VideoUnitViewModel(
            "",
            "",
            "",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics,
        )
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } returns Unit
        every {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        } returns Unit
        viewModel.markBlockCompleted("", "")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            courseAnalytics.logEvent(
                CourseAnalyticsEvent.VIDEO_COMPLETED.eventName,
                any()
            )
        }
    }

    @Test
    fun `CourseVideoPositionChanged notifier test`() = runTest {
        val viewModel = VideoUnitViewModel(
            "",
            "",
            "",
            courseRepository,
            notifier,
            networkConnection,
            transcriptManager,
            courseAnalytics,
        )
        coEvery { notifier.notifier } returns flow {
            emit(
                CourseVideoPositionChanged(
                    "",
                    10,
                    10000L,
                    false,
                )
            )
        }
        coEvery { courseRepository.saveVideoProgress(any(), any(), any(), any()) } returns Unit
        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        assert(viewModel.currentVideoTime.value == 10L)
        assert(viewModel.isUpdated.value == true)
    }
}
