package org.openedx.profile.presentation.settings

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.AppUpdateState
import org.openedx.core.CalendarRouter
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.utils.EmailUtil
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Configuration
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileAnalyticsEvent
import org.openedx.profile.presentation.ProfileAnalyticsKey
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.account.AccountDeactivated
import org.openedx.profile.system.notifier.profile.ProfileNotifier

class SettingsViewModel(
    private val appData: AppData,
    private val config: Config,
    private val resourceManager: ResourceManager,
    private val interactor: ProfileInteractor,
    private val cookieManager: AppCookieManager,
    private val workerController: DownloadWorkerController,
    private val analytics: ProfileAnalytics,
    private val profileRouter: ProfileRouter,
    private val calendarRouter: CalendarRouter,
    private val appNotifier: AppNotifier,
    private val profileNotifier: ProfileNotifier,
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<SettingsUIState> = MutableStateFlow(SettingsUIState.Data(configuration))
    internal val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _successLogout = MutableSharedFlow<Boolean>()
    val successLogout: SharedFlow<Boolean>
        get() = _successLogout.asSharedFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _appUpgradeEvent = MutableStateFlow<AppUpgradeEvent?>(null)
    val appUpgradeEvent: StateFlow<AppUpgradeEvent?>
        get() = _appUpgradeEvent.asStateFlow()

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
        collectProfileEvent()
    }

    fun logout() {
        logProfileEvent(ProfileAnalyticsEvent.LOGOUT_CLICKED)
        viewModelScope.launch {
            try {
                workerController.removeModels()
                withContext(Dispatchers.IO) {
                    interactor.logout()
                }
                logProfileEvent(
                    event = ProfileAnalyticsEvent.LOGGED_OUT,
                    params = buildMap {
                        put(ProfileAnalyticsKey.FORCE.key, ProfileAnalyticsKey.FALSE.key)
                    }
                )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_unknown_error)
                        )
                    )
                }
            } finally {
                cookieManager.clearWebViewCookie()
                appNotifier.send(LogoutEvent(false))
                _successLogout.emit(true)
            }
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is AppUpgradeEvent) {
                    _appUpgradeEvent.value = event
                }
            }
        }
    }

    private fun collectProfileEvent() {
        viewModelScope.launch {
            profileNotifier.notifier.collect {
                if (it is AccountDeactivated) {
                    logout()
                }
            }
        }
    }

    fun videoSettingsClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToVideoSettings(fragmentManager)
        logProfileEvent(ProfileAnalyticsEvent.VIDEO_SETTING_CLICKED)
    }

    fun privacyPolicyClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_privacy_policy),
            url = configuration.agreementUrls.privacyPolicyUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.PRIVACY_POLICY_CLICKED)
    }

    fun cookiePolicyClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_cookie_policy),
            url = configuration.agreementUrls.cookiePolicyUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.COOKIE_POLICY_CLICKED)
    }

    fun dataSellClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_data_sell),
            url = configuration.agreementUrls.dataSellConsentUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.DATA_SELL_CLICKED)
    }

    fun faqClicked() {
        logProfileEvent(ProfileAnalyticsEvent.FAQ_CLICKED)
    }

    fun termsOfUseClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_terms_of_use),
            url = configuration.agreementUrls.tosUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.TERMS_OF_USE_CLICKED)
    }

    fun emailSupportClicked(context: Context) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            appVersion = appData.versionName
        )
        logProfileEvent(ProfileAnalyticsEvent.CONTACT_SUPPORT_CLICKED)
    }

    fun appVersionClickedEvent(context: Context) {
        AppUpdateState.openPlayMarket(context)
    }

    fun manageAccountClicked(fragmentManager: FragmentManager) {
        profileRouter.navigateToManageAccount(fragmentManager)
    }

    fun calendarSettingsClicked(fragmentManager: FragmentManager) {
        calendarRouter.navigateToCalendarSettings(fragmentManager)
    }

    fun restartApp(fragmentManager: FragmentManager) {
        profileRouter.restartApp(
            fragmentManager,
            isLogistrationEnabled
        )
    }

    private fun logProfileEvent(
        event: ProfileAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(ProfileAnalyticsKey.NAME.key, event.biValue)
                put(ProfileAnalyticsKey.CATEGORY.key, ProfileAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }
}
