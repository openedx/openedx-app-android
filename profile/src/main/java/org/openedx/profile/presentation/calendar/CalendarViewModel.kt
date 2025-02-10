package org.openedx.profile.presentation.calendar

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.calendar.CalendarCreated
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.system.notifier.calendar.CalendarSyncDisabled
import org.openedx.core.system.notifier.calendar.CalendarSyncFailed
import org.openedx.core.system.notifier.calendar.CalendarSyncOffline
import org.openedx.core.system.notifier.calendar.CalendarSynced
import org.openedx.core.system.notifier.calendar.CalendarSyncing
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.profile.presentation.ProfileRouter

class CalendarViewModel(
    private val calendarSyncScheduler: CalendarSyncScheduler,
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier,
    private val calendarInteractor: CalendarInteractor,
    private val corePreferences: CorePreferences,
    private val profileRouter: ProfileRouter,
    private val networkConnection: NetworkConnection,
) : BaseViewModel() {

    private val calendarInitState: CalendarUIState
        get() = CalendarUIState(
            isCalendarExist = isCalendarExist(),
            calendarData = null,
            calendarSyncState = if (networkConnection.isOnline()) {
                CalendarSyncState.SYNCED
            } else {
                CalendarSyncState.OFFLINE
            },
            isCalendarSyncEnabled = calendarPreferences.isCalendarSyncEnabled,
            coursesSynced = null,
            isRelativeDateEnabled = corePreferences.isRelativeDatesEnabled,
        )

    private val _uiState = MutableStateFlow(calendarInitState)
    val uiState: StateFlow<CalendarUIState>
        get() = _uiState.asStateFlow()

    init {
        calendarSyncScheduler.requestImmediateSync()
        viewModelScope.launch {
            calendarNotifier.notifier.collect { calendarEvent ->
                when (calendarEvent) {
                    CalendarCreated -> {
                        calendarSyncScheduler.requestImmediateSync()
                        _uiState.update { it.copy(isCalendarExist = true) }
                        getCalendarData()
                    }

                    CalendarSyncing -> {
                        _uiState.update { it.copy(calendarSyncState = CalendarSyncState.SYNCHRONIZATION) }
                    }

                    CalendarSynced -> {
                        _uiState.update { it.copy(calendarSyncState = CalendarSyncState.SYNCED) }
                        updateSyncedCoursesCount()
                    }

                    CalendarSyncFailed -> {
                        _uiState.update { it.copy(calendarSyncState = CalendarSyncState.SYNC_FAILED) }
                        updateSyncedCoursesCount()
                    }

                    CalendarSyncOffline -> {
                        _uiState.update { it.copy(calendarSyncState = CalendarSyncState.OFFLINE) }
                    }

                    CalendarSyncDisabled -> {
                        _uiState.update { calendarInitState }
                    }
                }
            }
        }

        getCalendarData()
        updateSyncedCoursesCount()
    }

    fun setUpCalendarSync(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher.launch(calendarManager.permissions)
    }

    fun setCalendarSyncEnabled(isEnabled: Boolean, fragmentManager: FragmentManager) {
        if (!isEnabled) {
            _uiState.value.calendarData?.let {
                val dialog = DisableCalendarSyncDialogFragment.newInstance(it)
                dialog.show(
                    fragmentManager,
                    DisableCalendarSyncDialogFragment.DIALOG_TAG
                )
            }
        } else {
            calendarPreferences.isCalendarSyncEnabled = true
            _uiState.update { it.copy(isCalendarSyncEnabled = true) }
            calendarSyncScheduler.requestImmediateSync()
        }
    }

    fun setRelativeDateEnabled(isEnabled: Boolean) {
        corePreferences.isRelativeDatesEnabled = isEnabled
        _uiState.update { it.copy(isRelativeDateEnabled = isEnabled) }
    }

    fun navigateToCoursesToSync(fragmentManager: FragmentManager) {
        profileRouter.navigateToCoursesToSync(fragmentManager)
    }

    private fun getCalendarData() {
        if (calendarManager.hasPermissions()) {
            val calendarData = calendarManager.getCalendarData(calendarId = calendarPreferences.calendarId)
            _uiState.update { it.copy(calendarData = calendarData) }
        }
    }

    private fun updateSyncedCoursesCount() {
        viewModelScope.launch {
            val courseStates = calendarInteractor.getAllCourseCalendarStateFromCache()
            if (courseStates.isNotEmpty()) {
                val syncedCoursesCount = courseStates.count { it.isCourseSyncEnabled }
                _uiState.update { it.copy(coursesSynced = syncedCoursesCount) }
            }
        }
    }

    private fun isCalendarExist(): Boolean {
        return try {
            calendarPreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST &&
                    calendarManager.isCalendarExist(calendarPreferences.calendarId)
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }
    }
}
