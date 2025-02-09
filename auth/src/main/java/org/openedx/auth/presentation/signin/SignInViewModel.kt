package org.openedx.auth.presentation.signin

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.auth.R
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.auth.presentation.AgreementProvider
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthAnalyticsEvent
import org.openedx.auth.presentation.AuthAnalyticsKey
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.sso.OAuthHelper
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.Validator
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.createHonorCodeField
import org.openedx.core.extension.isInternetError
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.core.system.EdxError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.core.utils.Logger
import org.openedx.core.R as CoreRes

class SignInViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val preferencesManager: CorePreferences,
    private val validator: Validator,
    private val appUpgradeNotifier: AppUpgradeNotifier,
    private val analytics: AuthAnalytics,
    private val oAuthHelper: OAuthHelper,
    private val configuration: Config,
    private val router: AuthRouter,
    private val whatsNewGlobalManager: WhatsNewGlobalManager,
    agreementProvider: AgreementProvider,
    config: Config,
    val courseId: String?,
    val infoType: String?,
) : BaseViewModel() {

    private val logger = Logger("SignInViewModel")

    private val _uiState = MutableStateFlow(
        SignInUIState(
            isLoginRegistrationFormEnabled = config.isLoginRegistrationEnabled(),
            isSSOLoginEnabled = config.isSSOLoginEnabled(),
            ssoButtonTitle = config.getSSOButtonTitle(key = Resources.getSystem().getConfiguration().locales[0].language.uppercase(), ""),
            isSSODefaultLoginButton = config.isSSODefaultLoginButton(),
            isFacebookAuthEnabled = config.getFacebookConfig().isEnabled(),
            isGoogleAuthEnabled = config.getGoogleConfig().isEnabled(),
            isMicrosoftAuthEnabled = config.getMicrosoftConfig().isEnabled(),
            isSocialAuthEnabled = config.isSocialAuthEnabled(),
            isLogistrationEnabled = config.isPreLoginExperienceEnabled(),
            agreement = agreementProvider.getAgreement(isSignIn = true)?.createHonorCodeField(),
        )
    )
    internal val uiState: StateFlow<SignInUIState> = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    init {
        collectAppUpgradeEvent()
    }

    fun login(username: String, password: String) {
        logEvent(AuthAnalyticsEvent.USER_SIGN_IN_CLICKED)
        if (!validator.isEmailOrUserNameValid(username)) {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.auth_invalid_email_username))
            return
        }
        if (!validator.isPasswordValid(password)) {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.auth_invalid_password))
            return
        }

        _uiState.update { it.copy(showProgress = true) }
        viewModelScope.launch {
            try {
                interactor.login(username, password)
                _uiState.update { it.copy(loginSuccess = true) }
                setUserId()
                logEvent(
                    AuthAnalyticsEvent.SIGN_IN_SUCCESS,
                    buildMap {
                        put(
                            AuthAnalyticsKey.METHOD.key,
                            AuthType.PASSWORD.methodName.lowercase()
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is EdxError.InvalidGrantException) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_invalid_grant))
                } else if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_unknown_error))
                }
            }
            _uiState.update { it.copy(showProgress = false) }
        }
    }

    fun ssoClicked(fragmentManager: FragmentManager) {
        router.navigateToSSOWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(org.openedx.core.R.string.core_sso_sign_in),
            url = configuration.getSSOURL(),
        )
    }

    fun ssoLogin(token: String) {
        logEvent(AuthAnalyticsEvent.USER_SIGN_IN_CLICKED)


        _uiState.update { it.copy(showProgress = true) }
        viewModelScope.launch {
            try {
                interactor.ssoLogin("JWT $token")
                _uiState.update { it.copy(loginSuccess = true) }

                setUserId()
                logEvent(
                    AuthAnalyticsEvent.SIGN_IN_SUCCESS,
                    buildMap {
                        put(
                            AuthAnalyticsKey.METHOD.key,
                            AuthType.PASSWORD.methodName.lowercase()
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is EdxError.InvalidGrantException) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_invalid_grant))
                } else if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreRes.string.core_error_unknown_error))
                }
            }
            _uiState.update { it.copy(showProgress = false) }
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                _appUpgradeEvent.value = event
            }
        }
    }

    fun socialAuth(fragment: Fragment, authType: AuthType) {
        _uiState.update { it.copy(showProgress = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    oAuthHelper.socialAuth(fragment, authType)
                }
            }
                .getOrNull()
                .checkToken()
        }
    }

    fun navigateToSignUp(parentFragmentManager: FragmentManager) {
        router.navigateToSignUp(parentFragmentManager, null, null)
        logEvent(AuthAnalyticsEvent.REGISTER_CLICKED)
    }

    fun navigateToForgotPassword(parentFragmentManager: FragmentManager) {
        router.navigateToRestorePassword(parentFragmentManager)
        logEvent(AuthAnalyticsEvent.FORGOT_PASSWORD_CLICKED)
    }

    override fun onCleared() {
        super.onCleared()
        oAuthHelper.clear()
    }

    private suspend fun exchangeToken(token: String, authType: AuthType) {
        runCatching {
            interactor.loginSocial(token, authType)
        }.onFailure { error ->
            logger.e { "Social login error: $error" }
            onUnknownError()
        }.onSuccess {
            logger.d { "Social login (${authType.methodName}) success" }
            _uiState.update { it.copy(loginSuccess = true) }
            setUserId()
            _uiState.update { it.copy(showProgress = false) }
        }
    }

    private fun onUnknownError(message: (() -> String)? = null) {
        message?.let {
            logger.e { it() }
        }
        _uiMessage.value = UIMessage.SnackBarMessage(
            resourceManager.getString(CoreRes.string.core_error_unknown_error)
        )
        _uiState.update { it.copy(showProgress = false) }
    }

    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }

    private suspend fun SocialAuthResponse?.checkToken() {
        this?.accessToken?.let { token ->
            if (token.isNotEmpty()) {
                exchangeToken(token, authType)
            } else {
                _uiState.update { it.copy(showProgress = false) }
            }
        } ?: onUnknownError()
    }

    fun openLink(fragmentManager: FragmentManager, links: Map<String, String>, link: String) {
        links.forEach { (key, value) ->
            if (value == link) {
                router.navigateToWebContent(fragmentManager, key, value)
                return
            }
        }
    }

    fun proceedWhatsNew(parentFragmentManager: FragmentManager) {
        val isNeedToShowWhatsNew = whatsNewGlobalManager.shouldShowWhatsNew()
        if (uiState.value.loginSuccess) {
            router.clearBackStack(parentFragmentManager)
            if (isNeedToShowWhatsNew) {
                router.navigateToWhatsNew(
                    parentFragmentManager,
                    courseId,
                    infoType
                )
            } else {
                router.navigateToMain(
                    parentFragmentManager,
                    courseId,
                    infoType
                )
            }
        }
    }

    private fun logEvent(
        event: AuthAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(AuthAnalyticsKey.NAME.key, event.biValue)
                putAll(params)
            }
        )
    }
}
