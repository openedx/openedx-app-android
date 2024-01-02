package org.openedx.auth.presentation.signup

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
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.ApiConstants
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeNotifier
import java.net.UnknownHostException


@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val preferencesManager = mockk<CorePreferences>()
    private val interactor = mockk<AuthInteractor>()
    private val analytics = mockk<AuthAnalytics>()
    private val appUpgradeNotifier = mockk<AppUpgradeNotifier>()

    //region parameters

    private val parametersMap = mapOf(
        ApiConstants.EMAIL to "user@gmail.com",
        ApiConstants.PASSWORD to "password123"
    )

    private val listOfFields = listOf(
        RegistrationField(
            "",
            "",
            RegistrationFieldType.TEXT,
            "",
            "",
            true,
            true,
            RegistrationField.Restrictions(),
            emptyList()
        ),

        RegistrationField(
            "",
            "",
            RegistrationFieldType.TEXT,
            "",
            "",
            true,
            false,
            RegistrationField.Restrictions(),
            emptyList()
        )
    )

    private val user = User(0, "", "", "")

    //endregion

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_invalid_grant) } returns "Invalid credentials"
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { appUpgradeNotifier.notifier } returns emptyFlow()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register has validation errors`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            parametersMap
        )
        every { analytics.createAccountClickedEvent(any()) } returns Unit
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        verify(exactly = 1) { analytics.createAccountClickedEvent(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        assertEquals(true, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(null, viewModel.uiMessage.value)
    }

    @Test
    fun `register no internet error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } throws UnknownHostException()
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        every { analytics.createAccountClickedEvent(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        verify(exactly = 1) { analytics.createAccountClickedEvent(any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(false, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `something went wrong error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } throws Exception()
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        every { analytics.createAccountClickedEvent(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { analytics.createAccountClickedEvent(any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(false, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(somethingWrong, message?.message)
    }


    @Test
    fun `success register`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            emptyMap()
        )
        every { analytics.createAccountClickedEvent(any()) } returns Unit
        every { analytics.registrationSuccessEvent(any()) } returns Unit
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 1) { interactor.register(any()) }
        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 1) { analytics.createAccountClickedEvent(any()) }
        verify(exactly = 1) { analytics.registrationSuccessEvent(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        assertEquals(false, viewModel.validationError.value)
        assertEquals(false, viewModel.isButtonLoading.value)
        assertEquals(null, viewModel.uiMessage.value)
        assertEquals(true, viewModel.successLogin.value)
    }

    @Test
    fun `getRegistrationFields no internet error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.getRegistrationFields() } throws UnknownHostException()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(viewModel.uiState.value is SignUpUIState.Loading)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `getRegistrationFields unknown error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.getRegistrationFields() } throws Exception()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(viewModel.uiState.value is SignUpUIState.Loading)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `getRegistrationFields success`() = runTest {
        val viewModel = SignUpViewModel(
            interactor,
            resourceManager,
            analytics,
            preferencesManager,
            appUpgradeNotifier,
            courseId = "",
        )
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        //val fields = viewModel.uiState.value as? SignUpUIState.Fields

        assert(viewModel.uiState.value is SignUpUIState.Fields)
        assertEquals(null, viewModel.uiMessage.value)
    }
}
