package org.openedx.course.presentation.dates

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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.data.model.DateType
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.domain.interactor.CourseInteractor
import java.net.UnknownHostException
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDatesViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val networkConnection = mockk<NetworkConnection>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    private val dateBlock = CourseDateBlock(
        complete = false,
        date = Date(),
        dateType = DateType.TODAY_DATE,
        description = "Mocked Course Date Description"
    )
    private val mockDateBlocks = linkedMapOf(
        Pair(
            "2023-10-20T15:08:07Z",
            arrayListOf(dateBlock, dateBlock)
        ),
        Pair(
            "2023-10-30T15:08:07Z",
            arrayListOf(dateBlock, dateBlock)
        )
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
    fun `getCourseDates no internet connection exception`() = runTest {
        val viewModel = CourseDatesViewModel("", interactor, networkConnection, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        Assert.assertEquals(noInternet, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Loading)
    }

    @Test
    fun `getCourseDates unknown exception`() = runTest {
        val viewModel = CourseDatesViewModel("", interactor, networkConnection, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        Assert.assertEquals(somethingWrong, message?.message)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Loading)
    }

    @Test
    fun `getCourseDates success with internet`() = runTest {
        val viewModel = CourseDatesViewModel("", interactor, networkConnection, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } returns mockDateBlocks

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Dates)
    }

    @Test
    fun `getCourseDates success with EmptyList`() = runTest {
        val viewModel = CourseDatesViewModel("", interactor, networkConnection, resourceManager)
        every { networkConnection.isOnline() } returns true
        coEvery { interactor.getCourseDates(any()) } returns linkedMapOf()

        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseDates(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.updating.value == false)
        assert(viewModel.uiState.value is DatesUIState.Empty)
    }
}
