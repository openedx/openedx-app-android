package org.openedx.auth.presentation.signup

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.auth.presentation.AgreementProvider
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthAnalyticsEvent
import org.openedx.auth.presentation.AuthAnalyticsKey
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.sso.OAuthHelper
import org.openedx.core.ApiConstants
import org.openedx.core.BaseViewModel
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.domain.model.createHonorCodeField
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.core.utils.Logger
import org.openedx.core.R as coreR

class SignUpViewModel(
    private val interactor: AuthInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: AuthAnalytics,
    private val preferencesManager: CorePreferences,
    private val appUpgradeNotifier: AppUpgradeNotifier,
    private val agreementProvider: AgreementProvider,
    private val oAuthHelper: OAuthHelper,
    private val config: Config,
    private val router: AuthRouter,
    val courseId: String?,
    val infoType: String?,
) : BaseViewModel() {

    private val logger = Logger("SignUpViewModel")

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

    private val _uiMessage = MutableSharedFlow<UIMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        collectAppUpgradeEvent()
    }

    fun getRegistrationFields() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                updateFields(interactor.getRegistrationFields())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(coreR.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(coreR.string.core_error_unknown_error)
                        )
                    )
                }
            } finally {
                _uiState.update { state ->
                    state.copy(isLoading = false)
                }
            }
        }
    }

    private fun updateFields(allFields: List<RegistrationField>) {
        val mutableAllFields = allFields.toMutableList()
        val requiredFields = mutableListOf<RegistrationField>()
        val optionalFields = mutableListOf<RegistrationField>()
        val agreementFields = mutableListOf<RegistrationField>()
        val agreementText = agreementProvider.getAgreement(isSignIn = false)
        if (agreementText != null) {
            val honourCode =
                allFields.find { it.name == ApiConstants.RegistrationFields.HONOR_CODE }
            val marketingEmails =
                allFields.find { it.name == ApiConstants.RegistrationFields.MARKETING_EMAILS }
            mutableAllFields.remove(honourCode)
            requiredFields.addAll(mutableAllFields.filter { it.required })
            optionalFields.addAll(mutableAllFields.filter { !it.required })
            requiredFields.remove(marketingEmails)
            optionalFields.remove(marketingEmails)
            marketingEmails?.let { agreementFields.add(it) }
            agreementFields.add(agreementText.createHonorCodeField())
        } else {
            requiredFields.addAll(mutableAllFields.filter { it.required })
            optionalFields.addAll(mutableAllFields.filter { !it.required })
        }
        _uiState.update { state ->
            state.copy(
                allFields = mutableAllFields,
                requiredFields = requiredFields,
                optionalFields = optionalFields,
                agreementFields = agreementFields,
            )
        }
    }

    fun register() {
        logEvent(AuthAnalyticsEvent.CREATE_ACCOUNT_CLICKED)
        val mapFields = uiState.value.allFields.associate { it.name to it.placeholder } +
                mapOf(ApiConstants.RegistrationFields.HONOR_CODE to true.toString())
        val resultMap = mapFields.toMutableMap()
        uiState.value.allFields.filter { !it.required }.forEach { (k, _) ->
            if (mapFields[k].isNullOrEmpty()) {
                resultMap.remove(k)
            }
        }
        _uiState.update { it.copy(isButtonLoading = true, validationError = false) }
        viewModelScope.launch {
            try {
                setErrorInstructions(emptyMap())
                val validationFields = interactor.validateRegistrationFields(mapFields)
                setErrorInstructions(validationFields.validationResult)
                if (validationFields.hasValidationError()) {
                    _uiState.update { it.copy(validationError = true, isButtonLoading = false) }
                } else {
                    val socialAuth = uiState.value.socialAuth
                    if (socialAuth?.accessToken != null) {
                        resultMap[ApiConstants.ACCESS_TOKEN] = socialAuth.accessToken
                        resultMap[ApiConstants.PROVIDER] = socialAuth.authType.postfix
                        resultMap[ApiConstants.CLIENT_ID] = config.getOAuthClientId()
                    }
                    interactor.register(resultMap.toMap())
                    logEvent(
                        event = AuthAnalyticsEvent.REGISTER_SUCCESS,
                        params = buildMap {
                            put(
                                AuthAnalyticsKey.METHOD.key,
                                (socialAuth?.authType?.methodName
                                    ?: AuthType.PASSWORD.methodName).lowercase()
                            )
                        }
                    )
                    if (socialAuth == null) {
                        interactor.login(
                            resultMap.getValue(ApiConstants.EMAIL),
                            resultMap.getValue(ApiConstants.PASSWORD)
                        )
                        setUserId()
                        _uiState.update { it.copy(successLogin = true, isButtonLoading = false) }
                    } else {
                        exchangeToken(socialAuth)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isButtonLoading = false) }
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(coreR.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(coreR.string.core_error_unknown_error)
                        )
                    )
                }
            }
        }
    }

    fun socialAuth(fragment: Fragment, authType: AuthType) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    oAuthHelper.socialAuth(fragment, authType)
                }
            }
                .getOrNull()
                .checkToken()
        }
    }

    private suspend fun SocialAuthResponse?.checkToken() {
        this?.accessToken?.let { token ->
            if (token.isNotEmpty()) {
                exchangeToken(this)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        } ?: _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun exchangeToken(socialAuth: SocialAuthResponse) {
        runCatching {
            interactor.loginSocial(socialAuth.accessToken, socialAuth.authType)
        }.onFailure {
            val fields = uiState.value.allFields.toMutableList()
                .filter { it.type != RegistrationFieldType.PASSWORD }
                .map { field ->
                    when (field.name) {
                        ApiConstants.NAME -> field.copy(placeholder = socialAuth.name)
                        ApiConstants.EMAIL -> field.copy(placeholder = socialAuth.email)
                        else -> field
                    }
                }
            setErrorInstructions(emptyMap())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    socialAuth = socialAuth,
                )
            }
            updateFields(fields)
        }.onSuccess {
            setUserId()
            logEvent(
                AuthAnalyticsEvent.SIGN_IN_SUCCESS,
                buildMap {
                    put(
                        AuthAnalyticsKey.METHOD.key,
                        socialAuth.authType.methodName.lowercase()
                    )
                }
            )
            _uiState.update { it.copy(successLogin = true) }
            logger.d { "Social login (${socialAuth.authType.methodName}) success" }
        }
    }

    private fun setErrorInstructions(errorMap: Map<String, String>) {
        val allFields = uiState.value.allFields
        val updatedFields = ArrayList<RegistrationField>(allFields.size)
        allFields.forEach {
            if (errorMap.containsKey(it.name)) {
                updatedFields.add(it.copy(errorInstructions = errorMap[it.name] ?: ""))
            } else {
                updatedFields.add(it.copy(errorInstructions = ""))
            }
        }
        updateFields(updatedFields)
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                _uiState.update { it.copy(appUpgradeEvent = event) }
            }
        }
    }

    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }

    fun updateField(key: String, value: String) {
        val updatedFields = uiState.value.allFields.toMutableList().map { field ->
            if (field.name == key) {
                field.copy(placeholder = value)
            } else {
                field
            }
        }
        updateFields(updatedFields)
    }

    fun openLink(fragmentManager: FragmentManager, links: Map<String, String>, link: String) {
        links.forEach { (key, value) ->
            if (value == link) {
                router.navigateToWebContent(fragmentManager, key, value)
                return
            }
        }
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
