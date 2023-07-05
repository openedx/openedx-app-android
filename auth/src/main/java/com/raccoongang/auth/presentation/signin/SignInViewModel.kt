package com.raccoongang.auth.presentation.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.auth.R
import com.raccoongang.auth.domain.interactor.AuthInteractor
import com.raccoongang.auth.presentation.AuthAnalytics
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.Validator
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.EdxError
import com.raccoongang.core.system.ResourceManager
import kotlinx.coroutines.launch
import com.raccoongang.core.R as CoreRes

class SignInViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val preferencesManager: PreferencesManager,
    private val validator: Validator,
    private val analytics: AuthAnalytics
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

