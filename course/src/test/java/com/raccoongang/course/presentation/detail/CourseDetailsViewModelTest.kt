package com.raccoongang.course.presentation.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.notifier.CourseDashboardUpdate
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val notifier = spyk<CourseNotifier>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCourseDetails no internet connection exception`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } throws UnknownHostException()
        coEvery { interactor.getEnrolledCourseById(any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCourseById(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.Loading)
    }

    @Test
    fun `getCourseDetails unknown exception`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } throws Exception()
        coEvery { interactor.getEnrolledCourseById(any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCourseById(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.Loading)
    }

    @Test
    fun `getCourseDetails success with internet`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockk()
        coEvery { interactor.getEnrolledCourseById(any()) } returns mockk()

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDetails(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCourseById(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `getCourseDetails success without internet`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCourseDetailsFromCache(any()) } returns mockk()
        coEvery { interactor.getEnrolledCourseFromCacheById(any()) } returns mockk()

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseDetails(any()) }
        coVerify(exactly = 0) { interactor.getEnrolledCourseById(any()) }
        coVerify(exactly = 1) { interactor.getCourseDetailsFromCache(any()) }
        coVerify(exactly = 1) { interactor.getEnrolledCourseFromCacheById(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse internet connection error`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        coEvery { interactor.enrollInACourse(any()) } throws UnknownHostException()
        coEvery { interactor.getEnrolledCourseById(any()) } returns mockk()
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockk()


        viewModel.enrollInACourse("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse unknown exception`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        coEvery { interactor.enrollInACourse(any()) } throws Exception()
        coEvery { interactor.getEnrolledCourseById(any()) } returns mockk()
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockk()


        viewModel.enrollInACourse("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `enrollInACourse success`() = runTest {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)
        coEvery { interactor.enrollInACourse(any()) } returns Unit
        coEvery { interactor.getEnrolledCourseById(any()) } returns mockk()
        coEvery { notifier.send(CourseDashboardUpdate()) } returns Unit
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDetails(any()) } returns mockk()


        delay(200)
        viewModel.enrollInACourse("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.enrollInACourse(any()) }
        coVerify(exactly = 2) { interactor.getEnrolledCourseById(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is CourseDetailsUIState.CourseData)
    }

    @Test
    fun `getCourseAboutBody contains black`() {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)

        val overview = viewModel.getCourseAboutBody(ULong.MAX_VALUE, ULong.MIN_VALUE)
        val count = overview.contains("black")
        assert(count)
    }

    @Test
    fun `getCourseAboutBody don't contains black`() {
        val viewModel =
            CourseDetailsViewModel("", networkConnection, interactor, resourceManager, notifier)

        val overview = viewModel.getCourseAboutBody(ULong.MAX_VALUE, ULong.MAX_VALUE)
        val count = overview.contains("black")
        assert(!count)
    }

}