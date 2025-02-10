package org.openedx.profile.presentation.delete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.Validator
import org.openedx.core.system.EdxError
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileAnalyticsEvent
import org.openedx.profile.presentation.ProfileAnalyticsKey
import org.openedx.profile.system.notifier.account.AccountDeactivated
import org.openedx.profile.system.notifier.profile.ProfileNotifier

class DeleteProfileViewModel(
    private val resourceManager: ResourceManager,
    private val interactor: ProfileInteractor,
    private val notifier: ProfileNotifier,
    private val validator: Validator,
    private val analytics: ProfileAnalytics,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DeleteProfileFragmentUIState>()
    val uiState: LiveData<DeleteProfileFragmentUIState>
        get() = _uiState

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    fun deleteProfile(password: String) {
        logDeleteProfileClickedEvent()
        if (!validator.isPasswordValid(password)) {
            _uiState.value =
                DeleteProfileFragmentUIState.Error(
                    resourceManager.getString(org.openedx.profile.R.string.profile_invalid_password)
                )
            return
        }
        viewModelScope.launch {
            _uiState.value = DeleteProfileFragmentUIState.Loading
            try {
                interactor.deactivateAccount(password)
                _uiState.value = DeleteProfileFragmentUIState.Success
                logDeleteProfileEvent(true)
                notifier.send(AccountDeactivated())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                    _uiState.value = DeleteProfileFragmentUIState.Initial
                } else if (e is EdxError.UserNotActiveException) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_user_not_active))
                    _uiState.value = DeleteProfileFragmentUIState.Initial
                } else {
                    _uiState.value =
                        DeleteProfileFragmentUIState.Error(
                            resourceManager.getString(org.openedx.profile.R.string.profile_password_is_incorrect)
                        )
                }
                logDeleteProfileEvent(false)
            }
        }
    }

    private fun logDeleteProfileClickedEvent() {
        logEvent(ProfileAnalyticsEvent.USER_DELETE_ACCOUNT_CLICKED)
    }

    private fun logDeleteProfileEvent(isSuccess: Boolean) {
        logEvent(
            ProfileAnalyticsEvent.DELETE_ACCOUNT_SUCCESS,
            buildMap {
                put(ProfileAnalyticsKey.SUCCESS.key, isSuccess)
            }
        )
    }

    private fun logEvent(event: ProfileAnalyticsEvent, param: Map<String, Any?> = emptyMap()) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(ProfileAnalyticsKey.NAME.key, event.biValue)
                put(ProfileAnalyticsKey.CATEGORY.key, ProfileAnalyticsKey.PROFILE.key)
                putAll(param)
            }
        )
    }
}
