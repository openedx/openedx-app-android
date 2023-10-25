package org.openedx.auth.presentation.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.auth.R
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.Validator
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.EdxError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.system.notifier.AppUpgradeEventUIState
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.core.R as CoreRes

class SignInViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val preferencesManager: CorePreferences,
    private val validator: Validator,
    private val analytics: AuthAnalytics,
    private val appUpgradeNotifier: AppUpgradeNotifier
) : BaseViewModel() {

    private val _showProgress = MutableLiveData<Boolean>()
    val showProgress: LiveData<Boolean>
        get() = _showProgress

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _loginSuccess = SingleEventLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean>
        get() = _loginSuccess

    private val _appUpgradeEventUIState = SingleEventLiveData<AppUpgradeEventUIState>()
    val appUpgradeEventUIState: LiveData<AppUpgradeEventUIState>
        get() = _appUpgradeEventUIState

    init {
        collectAppUpgradeEvent()
    }

    fun login(username: String, password: String) {
        if (!validator.isEmailValid(username)) {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.auth_invalid_email))
            return
        }
        if (!validator.isPasswordValid(password)) {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.auth_invalid_password))
            return
        }

        _showProgress.value = true
        viewModelScope.launch {
            try {
                interactor.login(username, password)
                _loginSuccess.value = true
                setUserId()
                analytics.userLoginEvent(LoginMethod.PASSWORD.methodName)
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
            _showProgress.value = false
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                when (event) {
                    is AppUpgradeEvent.UpgradeRequiredEvent -> {
                        _appUpgradeEventUIState.value = AppUpgradeEventUIState.UpgradeRequiredScreen
                    }

                    else -> {}
                }
            }
        }
    }

    fun signUpClickedEvent() {
        analytics.signUpClickedEvent()
    }

    fun forgotPasswordClickedEvent() {
        analytics.forgotPasswordClickedEvent()
    }

    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }
}

private enum class LoginMethod(val methodName: String) {
    PASSWORD("Password"),
    FACEBOOK("Facebook"),
    GOOGLE("Google"),
    MICROSOFT("Microsoft")
}

