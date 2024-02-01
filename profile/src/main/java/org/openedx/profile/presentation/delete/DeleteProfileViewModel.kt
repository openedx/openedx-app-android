package org.openedx.profile.presentation.delete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.Validator
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.EdxError
import org.openedx.core.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.core.system.notifier.AccountDeactivated
import org.openedx.core.system.notifier.ProfileNotifier
import kotlinx.coroutines.launch

class DeleteProfileViewModel(
    private val resourceManager: ResourceManager,
    private val interactor: ProfileInteractor,
    private val notifier: ProfileNotifier,
    private val validator: Validator
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DeleteProfileFragmentUIState>()
    val uiState: LiveData<DeleteProfileFragmentUIState>
        get() = _uiState

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage


    fun deleteProfile(password: String) {
        if (!validator.isPasswordValid(password)) {
            _uiState.value =
                DeleteProfileFragmentUIState.Error(resourceManager.getString(org.openedx.profile.R.string.profile_invalid_password))
            return
        }
        viewModelScope.launch {
            _uiState.value = DeleteProfileFragmentUIState.Loading
            try {
                interactor.deactivateAccount(password)
                _uiState.value = DeleteProfileFragmentUIState.Success
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
                        DeleteProfileFragmentUIState.Error(resourceManager.getString(org.openedx.profile.R.string.profile_password_is_incorrect))
                }
            }
        }
    }
}
