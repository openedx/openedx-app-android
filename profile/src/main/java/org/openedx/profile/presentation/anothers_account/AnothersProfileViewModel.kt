package org.openedx.profile.presentation.anothers_account

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.profile.domain.interactor.AnothersProfileInteractor

class AnothersProfileViewModel(
    private val interactor: AnothersProfileInteractor,
    private val resourceManager: ResourceManager,
    val username: String
) : BaseViewModel() {

    private val _uiState = mutableStateOf<AnothersProfileUIState>(AnothersProfileUIState.Loading)
    val uiState: State<AnothersProfileUIState>
        get() = _uiState

    private val _uiMessage = mutableStateOf<UIMessage?>(null)
    val uiMessage: State<UIMessage?>
        get() = _uiMessage

    init {
        getAccount(username)
    }

    private fun getAccount(username: String) {
        _uiState.value = AnothersProfileUIState.Loading
        viewModelScope.launch {
            try {
                val account = interactor.getAccount(username)
                _uiState.value = AnothersProfileUIState.Data(account)
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
}