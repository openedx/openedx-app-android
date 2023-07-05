package org.openedx.auth.presentation.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.extension.isEmailValid
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.EdxError
import org.openedx.core.system.ResourceManager
import kotlinx.coroutines.launch

class RestorePasswordViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: AuthAnalytics
) : BaseViewModel() {

    private val _uiState = MutableLiveData<RestorePasswordUIState>()
    val uiState: LiveData<RestorePasswordUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    fun passwordReset(email: String) {
        _uiState.value = RestorePasswordUIState.Loading
        viewModelScope.launch {
            try {
                if (email.isNotEmpty() && email.isEmailValid()) {
                    if (interactor.passwordReset(email)) {
                        _uiState.value = RestorePasswordUIState.Success(email)
                        analytics.resetPasswordClickedEvent(true)
                    } else {
                        _uiState.value = RestorePasswordUIState.Initial
                        _uiMessage.value =
                            UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                        analytics.resetPasswordClickedEvent(false)
                    }
                } else {
                    _uiState.value = RestorePasswordUIState.Initial
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(org.openedx.auth.R.string.auth_invalid_email))
                    analytics.resetPasswordClickedEvent(false)
                }
            } catch (e: Exception) {
                _uiState.value = RestorePasswordUIState.Initial
                analytics.resetPasswordClickedEvent(false)
                if (e is EdxError.ValidationException) {
                    _uiMessage.value = UIMessage.SnackBarMessage(e.error)
                } else if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }
}