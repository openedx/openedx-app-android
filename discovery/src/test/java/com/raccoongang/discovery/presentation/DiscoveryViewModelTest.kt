package com.raccoongang.discovery.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.CourseList
import com.raccoongang.core.domain.model.Pagination
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.discovery.domain.interactor.DiscoveryInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class DiscoveryViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()


    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscoveryInteractor>()
    private val networkConnection = mockk<NetworkConnection>()

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
    fun `getCoursesList no internet connection`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
        assert(viewModel.canLoadMore.value == null)
    }

    @Test
    fun `getCoursesList unknown exception`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
        assert(viewModel.canLoadMore.value == null)
    }

    @Test
    fun `getCoursesList from cache`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCoursesListFromCache() } returns emptyList()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.getCoursesListFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `getCoursesList from network with next page`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "2",
                7,
                "1"
            ), emptyList()
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `getCoursesList from network without next page`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "",
                7,
                "1"
            ), emptyList()
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == false)
    }


    @Test
    fun `updateData no internet connection`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true

        coEvery { interactor.getCoursesList(any(), any(), any()) } throws UnknownHostException()
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true

        coEvery { interactor.getCoursesList(any(), any(), any()) } throws Exception()
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
    }

    @Test
    fun `updateData success with next page`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true

        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "2",
                7,
                "1"
            ), emptyList()
        )
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
    }

    @Test
    fun `updateData success without next page`() = runTest {
        val viewModel = DiscoveryViewModel(networkConnection, interactor, resourceManager)
        every { networkConnection.isOnline() } returns true

        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "",
                7,
                "1"
            ), emptyList()
        )
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
    }


}