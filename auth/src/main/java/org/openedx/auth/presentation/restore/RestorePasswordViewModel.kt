package org.openedx.auth.presentation.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthAnalyticsEvent
import org.openedx.auth.presentation.AuthAnalyticsKey
import org.openedx.core.R
import org.openedx.core.system.EdxError
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.foundation.extension.isEmailValid
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class RestorePasswordViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: AuthAnalytics,
    private val appNotifier: AppNotifier
) : BaseViewModel() {

    private val _uiState = MutableLiveData<RestorePasswordUIState>()
    val uiState: LiveData<RestorePasswordUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEventUIState: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    init {
        collectAppUpgradeEvent()
    }

    fun passwordReset(email: String) {
        logEvent(AuthAnalyticsEvent.RESET_PASSWORD_CLICKED)
        _uiState.value = RestorePasswordUIState.Loading
        viewModelScope.launch {
            try {
                if (email.isNotEmpty() && email.isEmailValid()) {
                    if (interactor.passwordReset(email)) {
                        _uiState.value = RestorePasswordUIState.Success(email)
                        logResetPasswordEvent(true)
                    } else {
                        _uiState.value = RestorePasswordUIState.Initial
                        _uiMessage.value =
                            UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                        logResetPasswordEvent(false)
                    }
                } else {
                    _uiState.value = RestorePasswordUIState.Initial
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(org.openedx.auth.R.string.auth_invalid_email)
                        )
                    logResetPasswordEvent(false)
                }
            } catch (e: Exception) {
                _uiState.value = RestorePasswordUIState.Initial
                logResetPasswordEvent(false)
                if (e is EdxError.ValidationException) {
                    _uiMessage.value = UIMessage.SnackBarMessage(e.error)
                } else if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_no_connection)
                        )
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_unknown_error)
                        )
                }
            }
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is AppUpgradeEvent) {
                    _appUpgradeEvent.value = event
                }
            }
        }
    }

    private fun logResetPasswordEvent(success: Boolean) {
        logEvent(
            event = AuthAnalyticsEvent.RESET_PASSWORD_SUCCESS,
            params = buildMap {
                put(AuthAnalyticsKey.SUCCESS.key, success)
            }
        )
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
