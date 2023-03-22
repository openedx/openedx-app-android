package com.raccoongang.profile.presentation.edit

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.profile.domain.interactor.ProfileInteractor
import com.raccoongang.profile.system.notifier.AccountUpdated
import com.raccoongang.profile.system.notifier.ProfileNotifier
import kotlinx.coroutines.launch
import java.io.File

class EditProfileViewModel(
    private val interactor: ProfileInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: ProfileNotifier,
    account: Account
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

    private val _showLeaveDialog = MutableLiveData<Boolean>()
    val showLeaveDialog: LiveData<Boolean>
        get() = _showLeaveDialog


    fun updateAccount(fields: Map<String, Any?>) {
        _uiState.value = EditProfileUIState(account, true)
        viewModelScope.launch {
            try {
                if (deleteImage.value == true) {
                    interactor.deleteProfileImage()
                }
                val account = interactor.updateAccount(fields)
                _uiState.value = EditProfileUIState(account, isUpdating = false)
                sendAccountUpdated()
                _deleteImage.value = false
            } catch (e: Exception) {
                _uiState.value = EditProfileUIState(account.copy())
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
        _uiState.value = EditProfileUIState(account, true)
        viewModelScope.launch {
            try {
                interactor.setProfileImage(file, mimeType)
                val updatedAccount = interactor.updateAccount(fields)
                _uiState.value = EditProfileUIState(updatedAccount, isUpdating = false)
                _selectedImageUri.value = null
                sendAccountUpdated()
            } catch (e: Exception) {
                _uiState.value = EditProfileUIState(account.copy())
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

}