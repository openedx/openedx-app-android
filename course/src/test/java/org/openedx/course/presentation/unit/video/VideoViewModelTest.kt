package org.openedx.course.presentation.unit.video

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val courseRepository = mockk<CourseRepository>()
    private val notifier = mockk<CourseNotifier>()
    private val preferenceManager = mockk<CorePreferences>()
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
    fun `sendTime test`() = runTest {
        val viewModel =
            VideoViewModel("", courseRepository, notifier, preferenceManager, courseAnalytics)
        coEvery { notifier.send(CourseVideoPositionChanged("", 0, 0L, false)) } returns Unit
        viewModel.sendTime()
        advanceUntilIdle()

        coVerify(exactly = 1) { notifier.send(CourseVideoPositionChanged("", 0, 0L, false)) }
    }

    @Test
    fun `markBlockCompleted exception`() = runTest {
        val viewModel =
            VideoViewModel("", courseRepository, notifier, preferenceManager, courseAnalytics)
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
        val viewModel =
            VideoViewModel("", courseRepository, notifier, preferenceManager, courseAnalytics)
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
}
