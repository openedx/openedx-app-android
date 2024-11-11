package org.openedx.auth.presentation.signin

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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.auth.R
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AgreementProvider
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.sso.OAuthHelper
import org.openedx.core.Validator
import org.openedx.core.config.Config
import org.openedx.core.config.FacebookConfig
import org.openedx.core.config.GoogleConfig
import org.openedx.core.config.MicrosoftConfig
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.core.system.EdxError
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.SignInEvent
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.core.R as CoreRes

@ExperimentalCoroutinesApi
class SignInViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val validator = mockk<Validator>()
    private val resourceManager = mockk<ResourceManager>()
    private val preferencesManager = mockk<CorePreferences>()
    private val interactor = mockk<AuthInteractor>()
    private val analytics = mockk<AuthAnalytics>()
    private val appNotifier = mockk<AppNotifier>()
    private val agreementProvider = mockk<AgreementProvider>()
    private val oAuthHelper = mockk<OAuthHelper>()
    private val router = mockk<AuthRouter>()
    private val whatsNewGlobalManager = mockk<WhatsNewGlobalManager>()
    private val calendarInteractor = mockk<CalendarInteractor>()
    private val calendarPreferences = mockk<CalendarPreferences>()

    private val invalidCredential = "Invalid credentials"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val invalidEmailOrUsername = "Invalid email or username"
    private val invalidPassword = "Password too short"

    private val user = User(0, "", "", "")

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(CoreRes.string.core_error_invalid_grant) } returns invalidCredential
        every { resourceManager.getString(CoreRes.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(CoreRes.string.core_error_unknown_error) } returns somethingWrong
        every { resourceManager.getString(R.string.auth_invalid_email_username) } returns invalidEmailOrUsername
        every { resourceManager.getString(R.string.auth_invalid_password) } returns invalidPassword
        every { appNotifier.notifier } returns emptyFlow()
        every { agreementProvider.getAgreement(true) } returns null
        every { config.isPreLoginExperienceEnabled() } returns false
        every { config.isSocialAuthEnabled() } returns false
        every { config.getFacebookConfig() } returns FacebookConfig()
        every { config.getGoogleConfig() } returns GoogleConfig()
        every { config.getMicrosoftConfig() } returns MicrosoftConfig()
        every { calendarPreferences.calendarUser } returns ""
        every { calendarPreferences.clearCalendarPreferences() } returns Unit
        coEvery { calendarInteractor.clearCalendarCachedData() } returns Unit
        every { analytics.logScreenEvent(any(), any()) } returns Unit
        every { config.isRegistrationEnabled() } returns true
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login empty credentials validation error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        viewModel.login("", "")
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertEquals(invalidEmailOrUsername, message.message)
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
    }

    @Test
    fun `login invalid email validation error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        viewModel.login("acc@test.o", "")
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertEquals(invalidEmailOrUsername, message.message)
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
    }

    @Test
    fun `login empty password validation error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        viewModel.login("acc@test.org", "")

        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertEquals(invalidPassword, message.message)
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
    }

    @Test
    fun `login invalid password validation error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        viewModel.login("acc@test.org", "ed")

        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertEquals(invalidPassword, message.message)
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
    }

    @Test
    fun `login success`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        coEvery { appNotifier.send(any<SignInEvent>()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        coEvery { interactor.login("acc@test.org", "edx") } returns Unit
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        verify(exactly = 2) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showProgress)
        assert(uiState.loginSuccess)
        assertEquals(null, viewModel.uiMessage.value)
    }

    @Test
    fun `login network error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        coEvery { interactor.login("acc@test.org", "edx") } throws UnknownHostException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }
        verify(exactly = 1) { appNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `login invalid grant error`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        coEvery { interactor.login("acc@test.org", "edx") } throws EdxError.InvalidGrantException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appNotifier.notifier }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
        assertEquals(invalidCredential, message.message)
    }

    @Test
    fun `login unknown exception`() = runTest {
        every { validator.isEmailOrUserNameValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { analytics.logEvent(any(), any()) } returns Unit
        val viewModel = SignInViewModel(
            interactor = interactor,
            resourceManager = resourceManager,
            preferencesManager = preferencesManager,
            validator = validator,
            analytics = analytics,
            appNotifier = appNotifier,
            oAuthHelper = oAuthHelper,
            agreementProvider = agreementProvider,
            config = config,
            router = router,
            whatsNewGlobalManager = whatsNewGlobalManager,
            courseId = "",
            infoType = "",
            calendarInteractor = calendarInteractor,
            calendarPreferences = calendarPreferences
        )
        coEvery { interactor.login("acc@test.org", "edx") } throws IllegalStateException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appNotifier.notifier }
        verify(exactly = 1) { analytics.logEvent(any(), any()) }
        verify(exactly = 1) { analytics.logScreenEvent(any(), any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showProgress)
        assertFalse(uiState.loginSuccess)
        assertEquals(somethingWrong, message.message)
    }
}
