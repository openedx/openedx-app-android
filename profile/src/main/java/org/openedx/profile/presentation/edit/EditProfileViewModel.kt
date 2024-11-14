package org.openedx.profile.presentation.edit

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileAnalyticsEvent
import org.openedx.profile.presentation.ProfileAnalyticsKey
import org.openedx.profile.system.notifier.account.AccountUpdated
import org.openedx.profile.system.notifier.profile.ProfileNotifier
import java.io.File

class EditProfileViewModel(
    private val interactor: ProfileInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: ProfileNotifier,
    private val analytics: ProfileAnalytics,
    val config: Config,
    account: Account,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<EditProfileUIState>()
    val uiState: LiveData<EditProfileUIState>
        get() = _uiState

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    var account = account
        private set

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?>
        get() = _selectedImageUri

    private val _deleteImage = MutableLiveData<Boolean>()
    val deleteImage: LiveData<Boolean>
        get() = _deleteImage

    var profileDataChanged = false
    var isLimitedProfile: Boolean = account.isLimited()
        set(value) {
            field = value
            _uiState.value = EditProfileUIState(account, isLimited = value)
            logProfileEvent(
                ProfileAnalyticsEvent.SWITCH_PROFILE,
                buildMap {
                    put(
                        ProfileAnalyticsKey.ACTION.key,
                        if (isLimitedProfile) {
                            ProfileAnalyticsKey.LIMITED_PROFILE.key
                        } else {
                            ProfileAnalyticsKey.FULL_PROFILE.key
                        }
                    )
                }
            )
        }

    private val _showLeaveDialog = MutableLiveData<Boolean>()
    val showLeaveDialog: LiveData<Boolean>
        get() = _showLeaveDialog

    init {
        logProfileScreenEvent(ProfileAnalyticsEvent.EDIT_PROFILE)
    }

    fun updateAccount(fields: Map<String, Any?>) {
        _uiState.value = EditProfileUIState(account, true, isLimitedProfile)
        viewModelScope.launch {
            try {
                if (deleteImage.value == true) {
                    interactor.deleteProfileImage()
                }
                val updatedAccount = interactor.updateAccount(fields)
                account = updatedAccount
                isLimitedProfile = updatedAccount.isLimited()
                _uiState.value =
                    EditProfileUIState(updatedAccount, isUpdating = false, isLimitedProfile)
                sendAccountUpdated()
                _deleteImage.value = false
                _selectedImageUri.value = null
            } catch (e: Exception) {
                _uiState.value = EditProfileUIState(account.copy(), isLimited = isLimitedProfile)
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun updateAccountAndImage(fields: Map<String, Any?>, file: File, mimeType: String) {
        _uiState.value = EditProfileUIState(account, true, isLimitedProfile)
        viewModelScope.launch {
            try {
                interactor.setProfileImage(file, mimeType)
                val updatedAccount = interactor.updateAccount(fields)
                account = updatedAccount
                isLimitedProfile = updatedAccount.isLimited()
                _uiState.value =
                    EditProfileUIState(updatedAccount, isUpdating = false, isLimitedProfile)
                _selectedImageUri.value = null
                sendAccountUpdated()
            } catch (e: Exception) {
                _uiState.value = EditProfileUIState(account.copy(), isLimited = isLimitedProfile)
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun deleteImage() {
        _deleteImage.value = true
        _selectedImageUri.value = null
    }

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        _deleteImage.value = false
    }

    fun setShowLeaveDialog(value: Boolean) {
        _showLeaveDialog.value = value
    }

    private suspend fun sendAccountUpdated() {
        notifier.send(AccountUpdated())
    }

    fun profileEditDoneClickedEvent() {
        logProfileEvent(ProfileAnalyticsEvent.EDIT_DONE_CLICKED)
    }

    private fun logProfileEvent(
        event: ProfileAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(ProfileAnalyticsKey.NAME.key, event.biValue)
                put(ProfileAnalyticsKey.CATEGORY.key, ProfileAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }

    private fun logProfileScreenEvent(
        event: ProfileAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(ProfileAnalyticsKey.NAME.key, event.biValue)
                put(ProfileAnalyticsKey.CATEGORY.key, ProfileAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }
}
