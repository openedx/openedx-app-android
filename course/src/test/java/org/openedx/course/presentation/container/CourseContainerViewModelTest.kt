package org.openedx.course.presentation.container

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.R
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
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
    private val analytics = mockk<CourseAnalytics>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val courseStructure = CourseStructure(
        root = "",
        blockData = listOf(),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = null,
        startDisplay = "",
        startType = "",
        end = null,
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preloadCourseStructure internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
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
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
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
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.preloadCourseStructure(any()) } returns Unit
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value != null)
    }

    @Test
    fun `preloadCourseStructure success without internet`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.preloadCourseStructureFromCache(any()) } returns Unit
        every { interactor.getCourseStructureFromCache() } returns courseStructure
        viewModel.preloadCourseStructure()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.preloadCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.preloadCourseStructureFromCache(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
        assert(viewModel.dataReady.value != null)
    }

    @Test
    fun `updateData no internet connection exception`() = runTest {
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
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
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
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
        val viewModel = CourseContainerViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            networkConnection,
            analytics
        )
        coEvery { interactor.preloadCourseStructure(any()) } returns Unit
        coEvery { notifier.send(CourseStructureUpdated("", false)) } returns Unit
        viewModel.updateData(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.preloadCourseStructure(any()) }

        assert(viewModel.errorMessage.value == null)
        assert(viewModel.showProgress.value == false)
    }

}