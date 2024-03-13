package org.openedx.profile.presentation.profile

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.AppUpdateState
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.core.utils.EmailUtil
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Configuration
import org.openedx.profile.presentation.ProfileAnalyticEvent
import org.openedx.profile.presentation.ProfileAnalyticKey
import org.openedx.profile.presentation.ProfileAnalyticValue
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.AccountDeactivated
import org.openedx.profile.system.notifier.AccountUpdated
import org.openedx.profile.system.notifier.ProfileNotifier

class ProfileViewModel(
    private val appData: AppData,
    private val config: Config,
    private val interactor: ProfileInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: ProfileNotifier,
    private val dispatcher: CoroutineDispatcher,
    private val cookieManager: AppCookieManager,
    private val workerController: DownloadWorkerController,
    private val analytics: ProfileAnalytics,
    private val router: ProfileRouter,
    private val appUpgradeNotifier: AppUpgradeNotifier
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<ProfileUIState> =
        MutableStateFlow(ProfileUIState.Loading)
    internal val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    private val _successLogout = MutableLiveData<Boolean>()
    val successLogout: LiveData<Boolean>
        get() = _successLogout

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent?>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent?>
        get() = _appUpgradeEvent

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    private val configuration
        get() = Configuration(
            agreementUrls = config.getAgreement(Locale.current.language),
            faqUrl = config.getFaqUrl(),
            supportEmail = config.getFeedbackEmailAddress(),
            versionName = appData.versionName,
        )

    init {
        getAccount()
        collectAppUpgradeEvent()
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
                val cachedAccount = interactor.getCachedAccount()
                if (cachedAccount == null) {
                    _uiState.value = ProfileUIState.Loading
                } else {
                    _uiState.value = ProfileUIState.Data(
                        account = cachedAccount,
                        configuration = configuration,
                    )
                }
                val account = interactor.getAccount()
                _uiState.value = ProfileUIState.Data(
                    account = account,
                    configuration = configuration,
                )
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
        logProfileEvent(ProfileAnalyticEvent.LOGOUT_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.LOGOUT_CLICKED.biValue)
        })
        viewModelScope.launch {
            try {
                workerController.removeModels()
                withContext(dispatcher) {
                    interactor.logout()
                }
                logProfileEvent(ProfileAnalyticEvent.LOGGED_OUT.event, buildMap {
                    put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.LOGGED_OUT.biValue)
                })
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                cookieManager.clearWebViewCookie()
                _successLogout.value = true
            }
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                _appUpgradeEvent.value = event
            }
        }
    }

    fun profileEditClicked(fragmentManager: FragmentManager) {
        (uiState.value as? ProfileUIState.Data)?.let { data ->
            router.navigateToEditProfile(
                fragmentManager,
                data.account
            )
        }
        logProfileEvent(ProfileAnalyticEvent.EDIT_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.EDIT_CLICKED.biValue)
        })
    }

    fun profileVideoSettingsClicked(fragmentManager: FragmentManager) {
        router.navigateToVideoSettings(fragmentManager)
        logProfileEvent(ProfileAnalyticEvent.VIDEO_SETTING_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.VIDEO_SETTING_CLICKED.biValue)
        })
    }

    fun privacyPolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_privacy_policy),
            url = configuration.agreementUrls.privacyPolicyUrl,
        )
        logProfileEvent(ProfileAnalyticEvent.PRIVACY_POLICY_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.PRIVACY_POLICY_CLICKED.biValue)
        })
    }

    fun cookiePolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_cookie_policy),
            url = configuration.agreementUrls.cookiePolicyUrl,
        )
        logProfileEvent(ProfileAnalyticEvent.COOKIE_POLICY_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.COOKIE_POLICY_CLICKED.biValue)
        })
    }

    fun dataSellClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_data_sell),
            url = configuration.agreementUrls.dataSellConsentUrl,
        )
        logProfileEvent(ProfileAnalyticEvent.DATA_SELL_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.DATA_SELL_CLICKED.biValue)
        })
    }

    fun faqClicked() {
        logProfileEvent(ProfileAnalyticEvent.FAQ_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.FAQ_CLICKED.biValue)
        })
    }

    fun termsOfUseClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_terms_of_use),
            url = configuration.agreementUrls.tosUrl,
        )
        logProfileEvent(ProfileAnalyticEvent.TERMS_OF_USE_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.TERMS_OF_USE_CLICKED.biValue)
        })
    }

    fun emailSupportClicked(context: Context) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            appVersion = appData.versionName
        )
        logProfileEvent(ProfileAnalyticEvent.CONTACT_SUPPORT_CLICKED.event, buildMap {
            put(ProfileAnalyticKey.NAME.key, ProfileAnalyticValue.CONTACT_SUPPORT_CLICKED.biValue)
        })
    }

    fun appVersionClickedEvent(context: Context) {
        AppUpdateState.openPlayMarket(context)
    }

    fun restartApp(fragmentManager: FragmentManager) {
        router.restartApp(
            fragmentManager,
            isLogistrationEnabled
        )
    }

    private fun logProfileEvent(event: String, params: Map<String, Any?>) {
        analytics.logEvent(event, buildMap {
            put(ProfileAnalyticKey.CATEGORY.key, ProfileAnalyticKey.PROFILE.key)
            putAll(params)
        })
    }
}
