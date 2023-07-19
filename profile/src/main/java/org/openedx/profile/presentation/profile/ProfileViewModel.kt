package org.openedx.profile.presentation.profile

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.data.storage.PreferencesManager
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.system.notifier.AccountDeactivated
import org.openedx.profile.system.notifier.AccountUpdated
import org.openedx.profile.system.notifier.ProfileNotifier
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
    private val workerController: DownloadWorkerController,
    private val analytics: ProfileAnalytics
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
                analytics.logoutEvent(false)
                _successLogout.value = true
            } catch (e: Exception) {
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

    fun profileEditClickedEvent() {
        analytics.profileEditClickedEvent()
    }

    fun profileVideoSettingsClickedEvent() {
        analytics.profileVideoSettingsClickedEvent()
    }

    fun privacyPolicyClickedEvent() {
        analytics.privacyPolicyClickedEvent()
    }

    fun cookiePolicyClickedEvent() {
        analytics.cookiePolicyClickedEvent()
    }

    fun emailSupportClickedEvent() {
        analytics.emailSupportClickedEvent()
    }

}