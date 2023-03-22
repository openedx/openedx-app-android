package com.raccoongang.course.presentation.container

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.R
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CourseContainerViewModelTest {

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
    fun `preloadCourseStructure internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.preloadCourseStructure(any()) } throws UnknownHostException()
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        val message = viewModel.errorMessage.value
        assertEquals(noInternet, message)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value == null)
    }

    @Test
    fun `preloadCourseStructure unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.preloadCourseStructure(any()) } throws Exception()
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        val message = viewModel.errorMessage.value
        assertEquals(somethingWrong, message)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value == null)
    }

    @Test
    fun `preloadCourseStructure success with internet`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.preloadCourseStructure(any()) } returns Unit
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value == true)
    }

    @Test
    fun `preloadCourseStructure success without internet`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.preloadCourseStructureFromCache(any()) } returns Unit
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.preloadCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.preloadCourseStructureFromCache(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value == true)
    }

    @Test
    fun `updateData no internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        coEvery { interactor.preloadCourseStructure(any()) } throws UnknownHostException()
        coEvery { notifier.send(CourseStructureUpdated("", false)) } returns Unit
        viewModel.updateData(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        val message = viewModel.errorMessage.value
        assertEquals(noInternet, message)
        assert(viewModel.showProgress.value == false)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        coEvery { interactor.preloadCourseStructure(any()) } throws Exception()
        coEvery { notifier.send(CourseStructureUpdated("", false)) } returns Unit
        viewModel.updateData(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        val message = viewModel.errorMessage.value
        assertEquals(somethingWrong, message)
        assert(viewModel.showProgress.value == false)
    }

    @Test
    fun `updateData success`() = runTest {
        val viewModel = CourseContainerViewModel("", interactor,resourceManager, notifier, networkConnection)
        coEvery { interactor.preloadCourseStructure(any()) } returns Unit
        coEvery { notifier.send(CourseStructureUpdated("", false)) } returns Unit
        viewModel.updateData(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
    }

}