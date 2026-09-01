package org.openedx.discovery.presentation

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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Pagination
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.CourseList
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.captureUiMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@OptIn(ExperimentalCoroutinesApi::class)
class NativeDiscoveryViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscoveryInteractor>()
    private val networkConnection = mockk<NetworkConnection>()
    private val analytics = mockk<DiscoveryAnalytics>()
    private val corePreferences = mockk<CorePreferences>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every {
            resourceManager.getString(foundationR.string.foundation_error_no_connection)
        } returns noInternet
        every {
            resourceManager.getString(foundationR.string.foundation_error_unknown_error)
        } returns somethingWrong
        every { corePreferences.user } returns null
        every { config.getApiHostURL() } returns "http://localhost:8000"
        every { config.isPreLoginExperienceEnabled() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getCoursesList no internet connection`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
        assert(viewModel.canLoadMore.value == null)
    }

    @Test
    fun `getCoursesList unknown exception`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
        assert(viewModel.canLoadMore.value == null)
    }

    @Test
    fun `getCoursesList from cache`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns false
        coEvery { interactor.getCoursesListFromCache() } returns emptyList()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.getCoursesListFromCache() }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `getCoursesList from network with next page`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "2",
                7,
                "1"
            ),
            emptyList()
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `getCoursesList from network without next page`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "",
                7,
                "1"
            ),
            emptyList()
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCoursesList(any(), any(), any()) }
        coVerify(exactly = 0) { interactor.getCoursesListFromCache() }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `updateData no internet connection`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws UnknownHostException()
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
    }

    @Test
    fun `updateData unknown exception`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } throws Exception()
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == null)
        assert(viewModel.uiState.value is DiscoveryUIState.Loading)
    }

    @Test
    fun `updateData success with next page`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "2",
                7,
                "1"
            ),
            emptyList()
        )
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
    }

    @Test
    fun `updateData success without next page`() = runTest {
        val viewModel = NativeDiscoveryViewModel(
            config,
            networkConnection,
            interactor,
            resourceManager,
            analytics,
            corePreferences
        )
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCoursesList(any(), any(), any()) } returns CourseList(
            Pagination(
                10,
                "",
                7,
                "1"
            ),
            emptyList()
        )
        viewModel.updateData()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getCoursesList(any(), any(), any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
        assert(viewModel.uiState.value is DiscoveryUIState.Courses)
    }
}
