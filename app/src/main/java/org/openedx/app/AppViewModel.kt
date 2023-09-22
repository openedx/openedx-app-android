package org.openedx.app

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.RoomDatabase
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.app.system.notifier.LogoutEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.data.storage.CorePreferences

class AppViewModel(
    private val notifier: AppNotifier,
    private val room: RoomDatabase,
    private val preferencesManager: CorePreferences,
    private val dispatcher: CoroutineDispatcher,
    private val analytics: AppAnalytics
) : BaseViewModel() {

    val logoutUser: LiveData<Unit>
        get() = _logoutUser
    private val _logoutUser = SingleEventLiveData<Unit>()

    private var logoutHandledAt: Long = 0

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        setUserId()
        viewModelScope.launch {
            notifier.notifier.collect { event ->
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
    }

    private fun setUserId() {
        preferencesManager.user?.let {
            analytics.setUserIdForSession(it.id)
        }
    }

}