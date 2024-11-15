package org.openedx.auth.presentation.restore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.R
import org.openedx.core.system.EdxError
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class RestorePasswordViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<AuthInteractor>()
    private val analytics = mockk<AuthAnalytics>()
    private val appNotifier = mockk<AppNotifier>()

    //region parameters

    private val correctEmail = "acc@test.org"
    private val emptyEmail = ""

    //endregion

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val invalidEmail = "Invalid email"
    private val invalidPassword = "Password too short"

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { resourceManager.getString(org.openedx.auth.R.string.auth_invalid_email) } returns invalidEmail
        every { resourceManager.getString(org.openedx.auth.R.string.auth_invalid_password) } returns invalidPassword
        every { appNotifier.notifier } returns emptyFlow()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `passwordReset empty email validation error`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(emptyEmail) } returns true
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(emptyEmail)
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(invalidEmail, message?.message)
    }

    @Test
    fun `passwordReset invalid email validation error`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(invalidEmail) } returns true
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(invalidEmail)
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(invalidEmail, message?.message)
    }

    @Test
    fun `passwordReset validation error`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(correctEmail) } throws EdxError.ValidationException("error")
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals("error", message?.message)
    }

    @Test
    fun `passwordReset no internet error`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(correctEmail) } throws UnknownHostException()
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `passwordReset unknown error`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(correctEmail) } throws Exception()
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `unSuccess restore password`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(correctEmail) } returns false
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `success restore password`() = runTest {
        val viewModel =
            RestorePasswordViewModel(interactor, resourceManager, analytics, appNotifier)
        coEvery { interactor.passwordReset(correctEmail) } returns true
        every { analytics.logEvent(any(), any()) } returns Unit
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val state = viewModel.uiState.value as? RestorePasswordUIState.Success
        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(correctEmail, state?.email)
        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Success)
        assertEquals(null, message)
    }
}
