package org.openedx.app

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.app.deeplink.DeepLink
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.app.system.notifier.LogoutEvent
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.DownloadFailed
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.utils.FileUtil
import org.openedx.course.presentation.download.DownloadDialogManager

class AppViewModel(
    private val config: Config,
    private val appNotifier: AppNotifier,
    private val room: RoomDatabase,
    private val preferencesManager: CorePreferences,
    private val dispatcher: CoroutineDispatcher,
    private val analytics: AppAnalytics,
    private val deepLinkRouter: DeepLinkRouter,
    private val fileUtil: FileUtil,
    private val downloadNotifier: DownloadNotifier,
    private val downloadDialogManager: DownloadDialogManager,
) : BaseViewModel() {

    private val _logoutUser = SingleEventLiveData<Unit>()
    val logoutUser: LiveData<Unit>
        get() = _logoutUser

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    private var logoutHandledAt: Long = 0

    val isBranchEnabled get() = config.getBranchConfig().enabled
    private val canResetAppDirectory get() = preferencesManager.canResetAppDirectory

    var fragmentManager: FragmentManager? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        setUserId()
        if (canResetAppDirectory) {
            resetAppDirectory()
        }
        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is LogoutEvent && System.currentTimeMillis() - logoutHandledAt > 5000) {
                    logoutHandledAt = System.currentTimeMillis()
                    preferencesManager.clear()
                    withContext(dispatcher) {
                        room.clearAllTables()
                    }
                    analytics.logoutEvent(true)
                    _logoutUser.value = Unit
                }
            }
        }
        viewModelScope.launch {
            downloadNotifier.notifier.collect { event ->
                if (event is DownloadFailed) {
                    fragmentManager?.let {
                        downloadDialogManager.showDownloadFailedPopup(
                            downloadModel = event.downloadModel,
                            fragmentManager = it,
                        )
                    }
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
        fileUtil.deleteOldAppDirectory()
        preferencesManager.canResetAppDirectory = false
    }

    fun makeExternalRoute(fm: FragmentManager, deepLink: DeepLink) {
        deepLinkRouter.makeRoute(fm, deepLink)
    }

    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }
}
