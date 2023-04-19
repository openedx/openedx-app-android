package com.raccoongang.profile.presentation.profile

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.system.AppCookieManager
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.profile.domain.interactor.ProfileInteractor
import com.raccoongang.profile.system.notifier.AccountDeactivated
import com.raccoongang.profile.system.notifier.AccountUpdated
import com.raccoongang.profile.system.notifier.ProfileNotifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val interactor: ProfileInteractor,
    private val preferencesManager: PreferencesManager,
    private val resourceManager: ResourceManager,
    private val notifier: ProfileNotifier,
    private val dispatcher: CoroutineDispatcher,
    private val cookieManager: AppCookieManager,
    private val workerController: DownloadWorkerController
) : BaseViewModel() {

    private val _uiState = MutableLiveData<ProfileUIState>(ProfileUIState.Loading)
    val uiState: LiveData<ProfileUIState>
        get() = _uiState

    private val _successLogout = MutableLiveData<Boolean>()
    val successLogout: LiveData<Boolean>
        get() = _successLogout

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    init {
        getAccount()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is AccountUpdated) {
                    getAccount()
                } else if (it is AccountDeactivated) {
                    logout()
                }
            }
        }
    }

    private fun getAccount() {
        _uiState.value = ProfileUIState.Loading
        viewModelScope.launch {
            try {
                val account = interactor.getAccount()
                _uiState.value = ProfileUIState.Data(account)
                preferencesManager.profile = account
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun updateAccount() {
        _isUpdating.value = true
        getAccount()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                workerController.cancelWork()
                withContext(dispatcher) {
                    interactor.logout()
                }
                cookieManager.clearWebViewCookie()
                _successLogout.value = true
            } catch (e: Exception) {
                e.printStackTrace()
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

}