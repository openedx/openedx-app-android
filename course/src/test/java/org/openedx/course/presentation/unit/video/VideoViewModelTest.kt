package org.openedx.course.presentation.unit.video

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val courseRepository = mockk<CourseRepository>()
    private val notifier = mockk<CourseNotifier>()

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
        val viewModel = VideoViewModel("", courseRepository, notifier)
        coEvery { notifier.send(CourseVideoPositionChanged("", 0, false)) } returns Unit
        viewModel.sendTime()
        advanceUntilIdle()

        coVerify(exactly = 1) { notifier.send(CourseVideoPositionChanged("", 0, false)) }
    }

    @Test
    fun `markBlockCompleted exception`() = runTest {
        val viewModel = VideoViewModel("", courseRepository, notifier)
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } throws Exception()
        viewModel.markBlockCompleted("")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        }
    }

    @Test
    fun `markBlockCompleted success`() = runTest {
        val viewModel = VideoViewModel("", courseRepository, notifier)
        coEvery {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        } returns Unit
        viewModel.markBlockCompleted("")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            courseRepository.markBlocksCompletion(
                any(),
                any()
            )
        }
    }

}