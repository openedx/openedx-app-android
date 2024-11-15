package org.openedx.auth.presentation.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.text.intl.Locale
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AgreementProvider
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.sso.OAuthHelper
import org.openedx.core.ApiConstants
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.config.FacebookConfig
import org.openedx.core.config.GoogleConfig
import org.openedx.core.config.MicrosoftConfig
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AgreementUrls
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val preferencesManager = mockk<CorePreferences>()
    private val interactor = mockk<AuthInteractor>()
    private val analytics = mockk<AuthAnalytics>()
    private val appNotifier = mockk<AppNotifier>()
    private val agreementProvider = mockk<AgreementProvider>()
    private val oAuthHelper = mockk<OAuthHelper>()
    private val router = mockk<AuthRouter>()

    //region parameters

    private val parametersMap = mapOf(
        ApiConstants.EMAIL to "user@gmail.com",
        ApiConstants.PASSWORD to "password123",
        "honor_code" to "true",
    )

    private val listOfFields = listOf(
        RegistrationField(
            ApiConstants.EMAIL,
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
            ApiConstants.PASSWORD,
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
        every { appNotifier.notifier } returns emptyFlow()
        every { agreementProvider.getAgreement(false) } returns null
        every { config.isSocialAuthEnabled() } returns false
        every { config.getAgreement(Locale.current.language) } returns AgreementUrls()
        every { config.getFacebookConfig() } returns FacebookConfig()
        every { config.getGoogleConfig() } returns GoogleConfig()
        every { config.getMicrosoftConfig() } returns MicrosoftConfig()
        every { config.getMicrosoftConfig() } returns MicrosoftConfig()
        every { analytics.logScreenEvent(any(), any()) } returns Unit
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register has validation errors`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            parametersMap
        )
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        parametersMap.forEach {
            viewModel.updateField(it.key, it.value)
        }
        viewModel.register()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appNotifier.notifier }

        assertEquals(true, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.successLogin)
        assertFalse(viewModel.uiState.value.isButtonLoading)
    }

    @Test
    fun `register no internet error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        val deferred = async { viewModel.uiMessage.first() }

        coEvery { interactor.validateRegistrationFields(parametersMap) } throws UnknownHostException()
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        parametersMap.forEach {
            viewModel.updateField(it.key, it.value)
        }
        viewModel.register()
        advanceUntilIdle()
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        assertFalse(viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.successLogin)
        assertFalse(viewModel.uiState.value.isButtonLoading)
        assertEquals(noInternet, (deferred.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `something went wrong error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        val deferred = async { viewModel.uiMessage.first() }

        coEvery { interactor.validateRegistrationFields(parametersMap) } throws Exception()
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.register()
        advanceUntilIdle()
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        assertFalse(viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.successLogin)
        assertFalse(viewModel.uiState.value.isButtonLoading)
        assertEquals(somethingWrong, (deferred.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `success register`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            emptyMap()
        )
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery { analytics.logEvent(any(), any()) } returns Unit
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        parametersMap.forEach {
            viewModel.updateField(it.key, it.value)
        }
        viewModel.register()
        advanceUntilIdle()
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 1) { interactor.register(any()) }
        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        assertFalse(viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isButtonLoading)
        assertTrue(viewModel.uiState.value.successLogin)
    }

    @Test
    fun `getRegistrationFields no internet error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        val deferred = async { viewModel.uiMessage.first() }

        coEvery { interactor.getRegistrationFields() } throws UnknownHostException()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appNotifier.notifier }

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(noInternet, (deferred.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `getRegistrationFields unknown error`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        val deferred = async { viewModel.uiMessage.first() }

        coEvery { interactor.getRegistrationFields() } throws Exception()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appNotifier.notifier }

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(somethingWrong, (deferred.await() as? UIMessage.SnackBarMessage)?.message)
    }

    @Test
    fun `getRegistrationFields success`() = runTest {
        val viewModel = SignUpViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            analytics = analytics,
            preferencesManager = preferencesManager,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            courseId = "",
            infoType = "",
        )
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }
        verify(exactly = 1) { appNotifier.notifier }

        // val fields = viewModel.uiState.value as? SignUpUIState.Fields

        assertFalse(viewModel.uiState.value.isLoading)
    }
}
