package org.openedx.app

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.app.deeplink.DeepLink
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.system.push.PushRegistrations
import org.openedx.app.system.push.RefreshFirebaseTokenWorker
import org.openedx.app.system.push.SyncFirebaseTokenWorker
import org.openedx.core.config.Config
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.InstanceConfigLoader
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceStore
import org.openedx.core.instance.LegacySessionAdoptionStorage
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.system.notifier.DownloadFailed
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.system.notifier.app.SignInEvent
import org.openedx.core.utils.Directories
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

@SuppressLint("StaticFieldLeak")
class AppViewModel(
    private val config: Config,
    private val appNotifier: AppNotifier,
    private val preferencesManager: CorePreferences,
    private val sessionManager: InstanceSessionManager,
    private val instanceStore: InstanceStore,
    private val instanceConfigLoader: InstanceConfigLoader,
    private val pushRegistrations: PushRegistrations,
    private val namespaces: StorageNamespaceProvider,
    private val legacySessionAdoption: LegacySessionAdoptionStorage,
    private val analytics: AppAnalytics,
    private val deepLinkRouter: DeepLinkRouter,
    private val fileUtil: FileUtil,
    private val downloadNotifier: DownloadNotifier,
    private val context: Context,
    resourceManager: ResourceManager,
) : BaseViewModel(resourceManager) {

    private val _logoutUser = SingleEventLiveData<Unit>()
    val logoutUser: LiveData<Unit>
        get() = _logoutUser

    private val _downloadFailedDialog = MutableSharedFlow<DownloadFailed>()
    val downloadFailedDialog: SharedFlow<DownloadFailed>
        get() = _downloadFailedDialog.asSharedFlow()

    private val _isInstanceCatalogResolved = MutableStateFlow(false)

    /**
     * Whether the instance catalog for this launch is in place. The first screen depends on it
     * (a site picker, or a site's own sign-in), so the activity routes only once it is.
     */
    val isInstanceCatalogResolved: StateFlow<Boolean> = _isInstanceCatalogResolved.asStateFlow()

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    /** With several sites and none selected, the first screen is site selection. */
    val startsWithSiteSelection get() = instanceStore.hasMultipleInstances && instanceStore.currentInstance == null

    init {
        viewModelScope.launch { resolveInstanceCatalog() }
    }

    /**
     * The first launch after upgrading from a single-site build: the learner was signed in to
     * the site the app-level configuration points at, and that session now belongs to the
     * matching instance. Open it, instead of asking which site to use. Checked once, so signing
     * out later still leads to site selection.
     */
    private fun adoptLegacySession() {
        if (legacySessionAdoption.hasCheckedLegacySession) return
        legacySessionAdoption.hasCheckedLegacySession = true
        if (!instanceStore.hasMultipleInstances || instanceStore.currentInstance != null) return
        instanceStore.instancesConfig.instances
            .firstOrNull { namespaces.namespaceFor(it).isLegacy && preferencesManager.hasSession(it.key) }
            ?.let(instanceStore::select)
    }

    private suspend fun resolveInstanceCatalog() {
        // Falls back to the cached or bundled catalog when the fetch fails or times out.
        instanceStore.updateInstancesConfig(instanceConfigLoader.load())
        // A saved selection whose session is gone (signed out right before the process died,
        // or storage restored from a backup without its keys) would open a site nobody is signed
        // in to. With several sites, start over at site selection instead.
        val current = instanceStore.currentInstance
        if (instanceStore.hasMultipleInstances && current != null && !preferencesManager.hasSession(current.key)) {
            instanceStore.select(null)
        }
        adoptLegacySession()
        // A selection restored from storage doesn't go through a switch, so nothing else
        // applies its branding.
        sessionManager.applyThemeForCurrentInstance()
        _isInstanceCatalogResolved.value = true
    }

    private var logoutHandledAt: Long = 0

    val isBranchEnabled get() = config.getBranchConfig().enabled
    private val canResetAppDirectory get() = preferencesManager.canResetAppDirectory

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val user = preferencesManager.user

        setUserId(user)

        if (preferencesManager.pushToken.isNotEmpty()) {
            SyncFirebaseTokenWorker.scheduleForSignedInSites(context, pushRegistrations)
        }

        if (canResetAppDirectory) {
            resetAppDirectory()
        }

        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is SignInEvent && config.getFirebaseConfig().isCloudMessagingEnabled) {
                    // Sign-in completes only on the site that is still selected.
                    SyncFirebaseTokenWorker.schedule(context, instanceStore.currentInstance?.key)
                } else if (event is LogoutEvent) {
                    handleLogoutEvent(event)
                }
            }
        }
        viewModelScope.launch {
            downloadNotifier.notifier.collect { event ->
                if (event is DownloadFailed) {
                    _downloadFailedDialog.emit(event)
                }
            }
        }
    }

    fun logAppLaunchEvent() {
        analytics.logEvent(
            event = AppAnalyticsEvent.LAUNCH.eventName,
            params = buildMap {
                put(AppAnalyticsKey.NAME.key, AppAnalyticsEvent.LAUNCH.biValue)
            }
        )
    }

    private fun resetAppDirectory() {
        fileUtil.deleteOldAppDirectory(Directories.VIDEOS.name)
        preferencesManager.canResetAppDirectory = false
    }

    fun makeExternalRoute(fm: FragmentManager, deepLink: DeepLink) {
        deepLinkRouter.makeRoute(fm, deepLink)
    }

    private fun setUserId(user: User?) {
        user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }

    private suspend fun handleLogoutEvent(event: LogoutEvent) {
        if (System.currentTimeMillis() - logoutHandledAt > LOGOUT_EVENT_THRESHOLD) {
            if (event.isForced) {
                logoutHandledAt = System.currentTimeMillis()
                // Signs out of the selected site only; other sites keep their sessions.
                sessionManager.logoutCurrentInstance()
                analytics.logoutEvent(true)
                _logoutUser.value = Unit
            }

            if (config.getFirebaseConfig().isCloudMessagingEnabled) {
                RefreshFirebaseTokenWorker.schedule(context)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            }
        }
    }

    companion object {
        private const val LOGOUT_EVENT_THRESHOLD = 5000L
    }
}
