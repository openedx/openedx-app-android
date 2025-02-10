package org.openedx.app

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.app.deeplink.DeepLink
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.system.push.RefreshFirebaseTokenWorker
import org.openedx.app.system.push.SyncFirebaseTokenWorker
import org.openedx.core.config.Config
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.DownloadFailed
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.system.notifier.app.SignInEvent
import org.openedx.core.utils.Directories
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.utils.FileUtil

@SuppressLint("StaticFieldLeak")
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
    private val context: Context
) : BaseViewModel() {

    private val _logoutUser = SingleEventLiveData<Unit>()
    val logoutUser: LiveData<Unit>
        get() = _logoutUser

    private val _downloadFailedDialog = MutableSharedFlow<DownloadFailed>()
    val downloadFailedDialog: SharedFlow<DownloadFailed>
        get() = _downloadFailedDialog.asSharedFlow()

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    private var logoutHandledAt: Long = 0

    val isBranchEnabled get() = config.getBranchConfig().enabled
    private val canResetAppDirectory get() = preferencesManager.canResetAppDirectory

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val user = preferencesManager.user

        setUserId(user)

        if (user != null && preferencesManager.pushToken.isNotEmpty()) {
            SyncFirebaseTokenWorker.schedule(context)
        }

        if (canResetAppDirectory) {
            resetAppDirectory()
        }

        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is SignInEvent && config.getFirebaseConfig().isCloudMessagingEnabled) {
                    SyncFirebaseTokenWorker.schedule(context)
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
                preferencesManager.clearCorePreferences()
                withContext(dispatcher) {
                    room.clearAllTables()
                }
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
