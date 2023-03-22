package com.raccoongang.course.presentation.unit.video

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseVideoPositionChanged
import com.raccoongang.course.data.repository.CourseRepository
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
    private val preferencesManager = mockk<PreferencesManager>()


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
        val viewModel = VideoViewModel("", courseRepository, preferencesManager, notifier)
        coEvery { notifier.send(CourseVideoPositionChanged("", 0)) } returns Unit
        viewModel.sendTime()
        advanceUntilIdle()

        coVerify(exactly = 1) { notifier.send(CourseVideoPositionChanged("", 0)) }
    }

    @Test
    fun `markBlockCompleted exception`() = runTest {
        val viewModel = VideoViewModel("", courseRepository, preferencesManager, notifier)
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
        val viewModel = VideoViewModel("", courseRepository, preferencesManager, notifier)
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