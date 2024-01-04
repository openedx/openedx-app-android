package org.openedx.auth.presentation.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.ApiConstants
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.system.notifier.AppUpgradeNotifier

class SignUpViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: AuthAnalytics,
    private val preferencesManager: CorePreferences,
    private val appUpgradeNotifier: AppUpgradeNotifier,
    config: Config,
    val courseId: String?,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        SignUpUIState(
            isFacebookAuthEnabled = config.getFacebookConfig().isEnabled(),
            isGoogleAuthEnabled = config.getGoogleConfig().isEnabled(),
            isMicrosoftAuthEnabled = config.getMicrosoftConfig().isEnabled(),
            isSocialAuthEnabled = config.isSocialAuthEnabled(),
            isLoading = true,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isButtonLoading = MutableLiveData(false)
    val isButtonLoading: LiveData<Boolean>
        get() = _isButtonLoading

    private val _successLogin = MutableLiveData(false)
    val successLogin: LiveData<Boolean>
        get() = _successLogin

    private val _validationError = MutableLiveData(false)
    val validationError: LiveData<Boolean>
        get() = _validationError

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    private val optionalFields = mutableMapOf<String, String>()
    private val allFields = mutableListOf<RegistrationField>()

    init {
        collectAppUpgradeEvent()
    }

    fun getRegistrationFields() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val fields = interactor.getRegistrationFields()
                _uiState.update { state ->
                    state.copy(
                        fields = fields.filter { it.required },
                        optionalFields = fields.filter { !it.required },
                        isLoading = false,
                    )
                }
                optionalFields.clear()
                allFields.clear()
                allFields.addAll(fields)
                optionalFields.putAll((fields.filter { !it.required }.associate { it.name to "" }))
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

    fun register(mapFields: Map<String, String>) {
        analytics.createAccountClickedEvent("")
        val resultMap = mapFields.toMutableMap()
        optionalFields.forEach { (k, _) ->
            if (mapFields[k].isNullOrEmpty()) {
                resultMap.remove(k)
            }
        }
        _isButtonLoading.value = true
        _validationError.value = false
        viewModelScope.launch {
            try {
                val validationFields = interactor.validateRegistrationFields(resultMap.toMap())
                setErrorInstructions(validationFields.validationResult)
                if (validationFields.hasValidationError()) {
                    _validationError.value = true
                } else {
                    interactor.register(resultMap.toMap())
                    interactor.login(
                        resultMap.getValue(ApiConstants.EMAIL),
                        resultMap.getValue(ApiConstants.PASSWORD)
                    )
                    setUserId()
                    analytics.registrationSuccessEvent("")
                    _successLogin.value = true
                }
                _isButtonLoading.value = false
            } catch (e: Exception) {
                _isButtonLoading.value = false
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

    private fun setErrorInstructions(errorMap: Map<String, String>) {
        val updatedFields = ArrayList<RegistrationField>(allFields.size)
        allFields.forEach {
            if (errorMap.containsKey(it.name)) {
                updatedFields.add(it.copy(errorInstructions = errorMap[it.name] ?: ""))
            } else {
                updatedFields.add(it.copy(errorInstructions = ""))
            }
        }
        allFields.clear()
        allFields.addAll(updatedFields)
        _uiState.update { state ->
            state.copy(
                fields = updatedFields.filter { it.required },
                optionalFields = updatedFields.filter { !it.required },
                isLoading = false,
            )
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                _appUpgradeEvent.value = event
            }
        }
    }


    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }

}
