package org.openedx.profile.presentation.profile

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
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.captureUiMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.ProfileMocks
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.anothersaccount.AnothersProfileUIState
import org.openedx.profile.presentation.anothersaccount.AnothersProfileViewModel
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@OptIn(ExperimentalCoroutinesApi::class)
class AnothersProfileViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<ProfileInteractor>()
    private val username = "username"

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAccount no internetConnection`() = runTest {
        val viewModel = AnothersProfileViewModel(
            interactor,
            resourceManager,
            username
        )
        coEvery { interactor.getAccount(username) } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount(username) }

        val message = captureUiMessage(viewModel)
        assert(viewModel.uiState.value is AnothersProfileUIState.Loading)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `getAccount unknown exception`() = runTest {
        val viewModel = AnothersProfileViewModel(
            interactor,
            resourceManager,
            username
        )
        coEvery { interactor.getAccount(username) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount(username) }

        val message = captureUiMessage(viewModel)
        assert(viewModel.uiState.value is AnothersProfileUIState.Loading)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `getAccount success`() = runTest {
        val viewModel = AnothersProfileViewModel(
            interactor,
            resourceManager,
            username
        )
        coEvery { interactor.getAccount(username) } returns ProfileMocks.account.copy(
            accountPrivacy = Account.Privacy.PRIVATE
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount(username) }

        assert(viewModel.uiState.value is AnothersProfileUIState.Data)
        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
    }
}
