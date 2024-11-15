package org.openedx.course.presentation.handouts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.openedx.core.config.Config
import org.openedx.core.domain.model.AnnouncementModel
import org.openedx.core.domain.model.HandoutsModel
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class HandoutsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val interactor = mockk<CourseInteractor>()
    private val analytics = mockk<CourseAnalytics>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { config.getApiHostURL() } returns "http://localhost:8000"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getEnrolledCourse no internet connection exception`() = runTest {
        val viewModel = HandoutsViewModel("", "Handouts", config, interactor, analytics)
        coEvery { interactor.getHandouts(any()) } throws UnknownHostException()

        advanceUntilIdle()
        assert(viewModel.uiState.value == HandoutsUIState.Error)
    }

    @Test
    fun `getEnrolledCourse unknown exception`() = runTest {
        val viewModel = HandoutsViewModel("", "Handouts", config, interactor, analytics)
        coEvery { interactor.getHandouts(any()) } throws Exception()
        advanceUntilIdle()

        assert(viewModel.uiState.value == HandoutsUIState.Error)
    }

    @Test
    fun `getEnrolledCourse handouts success`() = runTest {
        val viewModel =
            HandoutsViewModel("", HandoutsType.Handouts.name, config, interactor, analytics)
        coEvery { interactor.getHandouts(any()) } returns HandoutsModel("hello")

        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getHandouts(any()) }
        coVerify(exactly = 0) { interactor.getAnnouncements(any()) }

        assert(viewModel.uiState.value is HandoutsUIState.HTMLContent)
    }

    @Test
    fun `getEnrolledCourse announcements success`() = runTest {
        val viewModel =
            HandoutsViewModel("", HandoutsType.Announcements.name, config, interactor, analytics)
        coEvery { interactor.getAnnouncements(any()) } returns listOf(
            AnnouncementModel(
                "date",
                "content"
            )
        )

        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.getHandouts(any()) }
        coVerify(exactly = 1) { interactor.getAnnouncements(any()) }

        assert(viewModel.uiState.value is HandoutsUIState.HTMLContent)
    }

    @Test
    fun `injectDarkMode test`() = runTest {
        val viewModel =
            HandoutsViewModel("", HandoutsType.Announcements.name, config, interactor, analytics)
        coEvery { interactor.getAnnouncements(any()) } returns listOf(
            AnnouncementModel(
                "date",
                "content"
            )
        )
        viewModel.injectDarkMode(
            viewModel.uiState.value.toString(),
            ULong.MAX_VALUE,
            ULong.MAX_VALUE
        )
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.getHandouts(any()) }
        coVerify(exactly = 1) { interactor.getAnnouncements(any()) }

        assert(viewModel.uiState.value is HandoutsUIState.HTMLContent)
    }
}
