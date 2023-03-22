package com.raccoongang.profile.presentation.delete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.Validator
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.EdxError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.profile.domain.interactor.ProfileInteractor
import com.raccoongang.profile.system.notifier.AccountDeactivated
import com.raccoongang.profile.system.notifier.ProfileNotifier
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
                DeleteProfileFragmentUIState.Error(resourceManager.getString(com.raccoongang.profile.R.string.profile_invalid_password))
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
                        DeleteProfileFragmentUIState.Error(resourceManager.getString(com.raccoongang.profile.R.string.profile_password_is_incorrect))
                }
            }
        }
    }
}