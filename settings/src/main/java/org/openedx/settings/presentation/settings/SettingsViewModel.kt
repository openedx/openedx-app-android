package org.openedx.settings.presentation.settings

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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
import org.openedx.settings.SettingsAnalytics
import org.openedx.settings.SettingsAnalyticsEvent
import org.openedx.settings.SettingsAnalyticsKey
import org.openedx.settings.SettingsRouter
import org.openedx.settings.domain.interactor.SettingsInteractor
import org.openedx.settings.domain.model.Configuration

class SettingsViewModel(
    private val appData: AppData,
    private val config: Config,
    private val resourceManager: ResourceManager,
    private val interactor: SettingsInteractor,
    private val cookieManager: AppCookieManager,
    private val workerController: DownloadWorkerController,
    private val analytics: SettingsAnalytics,
    private val router: SettingsRouter,
    private val appUpgradeNotifier: AppUpgradeNotifier
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<SettingsUIState> =
        MutableStateFlow(SettingsUIState.Data(configuration))
    internal val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _successLogout = MutableLiveData<Boolean>()
    val successLogout: LiveData<Boolean>
        get() = _successLogout

    private val _uiMessage = MutableLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

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
        collectAppUpgradeEvent()
    }

    fun logout() {
        logProfileEvent(SettingsAnalyticsEvent.LOGOUT_CLICKED)
        viewModelScope.launch {
            try {
                workerController.removeModels()
                withContext(Dispatchers.IO) {
                    interactor.logout()
                }
                logProfileEvent(
                    event = SettingsAnalyticsEvent.LOGGED_OUT,
                    params = buildMap {
                        put(SettingsAnalyticsKey.FORCE.key, SettingsAnalyticsKey.FALSE.key)
                    }
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

    fun videoSettingsClicked(fragmentManager: FragmentManager) {
        router.navigateToVideoSettings(fragmentManager)
        logProfileEvent(SettingsAnalyticsEvent.VIDEO_SETTING_CLICKED)
    }

    fun privacyPolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_privacy_policy),
            url = configuration.agreementUrls.privacyPolicyUrl,
        )
        logProfileEvent(SettingsAnalyticsEvent.PRIVACY_POLICY_CLICKED)
    }

    fun cookiePolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_cookie_policy),
            url = configuration.agreementUrls.cookiePolicyUrl,
        )
        logProfileEvent(SettingsAnalyticsEvent.COOKIE_POLICY_CLICKED)
    }

    fun dataSellClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_data_sell),
            url = configuration.agreementUrls.dataSellConsentUrl,
        )
        logProfileEvent(SettingsAnalyticsEvent.DATA_SELL_CLICKED)
    }

    fun faqClicked() {
        logProfileEvent(SettingsAnalyticsEvent.FAQ_CLICKED)
    }

    fun termsOfUseClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_terms_of_use),
            url = configuration.agreementUrls.tosUrl,
        )
        logProfileEvent(SettingsAnalyticsEvent.TERMS_OF_USE_CLICKED)
    }

    fun emailSupportClicked(context: Context) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            appVersion = appData.versionName
        )
        logProfileEvent(SettingsAnalyticsEvent.CONTACT_SUPPORT_CLICKED)
    }

    fun appVersionClickedEvent(context: Context) {
        AppUpdateState.openPlayMarket(context)
    }

    fun manageAccountClicked(fragmentManager: FragmentManager) {
        router.navigateToManageAccount(fragmentManager)
    }

    fun restartApp(fragmentManager: FragmentManager) {
        router.restartApp(
            fragmentManager,
            isLogistrationEnabled
        )
    }

    private fun logProfileEvent(
        event: SettingsAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(SettingsAnalyticsKey.NAME.key, event.biValue)
                put(SettingsAnalyticsKey.CATEGORY.key,SettingsAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }
}
