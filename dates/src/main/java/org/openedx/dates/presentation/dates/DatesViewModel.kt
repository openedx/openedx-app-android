package org.openedx.dates.presentation.dates

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.extension.isNotNull
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.isToday
import org.openedx.core.utils.toCalendar
import org.openedx.dates.domain.interactor.DatesInteractor
import org.openedx.dates.presentation.DatesAnalytics
import org.openedx.dates.presentation.DatesAnalyticsEvent
import org.openedx.dates.presentation.DatesAnalyticsKey
import org.openedx.dates.presentation.DatesRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.util.Calendar
import java.util.Date

class DatesViewModel(
    private val datesRouter: DatesRouter,
    private val networkConnection: NetworkConnection,
    private val resourceManager: ResourceManager,
    private val datesInteractor: DatesInteractor,
    private val analytics: DatesAnalytics,
    corePreferences: CorePreferences,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DatesUIState())
    val uiState: StateFlow<DatesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    var useRelativeDates = corePreferences.isRelativeDatesEnabled

    private var page = 1

    init {
        fetchDates(false)
    }

    private fun fetchDates(refresh: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(
                        isLoading = !refresh,
                        isRefreshing = refresh,
                    )
                }
                if (refresh) {
                    page = 1
                }
                val response = if (networkConnection.isOnline() || page > 1) {
                    datesInteractor.getUserDates(page)
                } else {
                    null
                }
                if (response != null) {
                    if (response.next.isNotNull() && page != response.count) {
                        _uiState.update { state -> state.copy(canLoadMore = true) }
                        page++
                    } else {
                        _uiState.update { state -> state.copy(canLoadMore = false) }
                        page = -1
                    }
                    _uiState.update { state ->
                        state.copy(
                            dates = state.dates + groupCourseDates(response.results)
                        )
                    }
                } else {
                    val cachedList = datesInteractor.getUserDatesFromCache()
                    _uiState.update { state -> state.copy(canLoadMore = false) }
                    page = -1
                    _uiState.update { state ->
                        state.copy(
                            dates = groupCourseDates(cachedList)
                        )
                    }
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                    )
                }
            } finally {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun shiftDueDate() {
        logEvent(DatesAnalyticsEvent.SHIFT_DUE_DATE_CLICK)
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(
                        isShiftDueDatesPressed = true,
                    )
                }
                val pastDueDates = _uiState.value.dates[DatesSection.PAST_DUE] ?: emptyList()
                val courseIds = pastDueDates
                    .filter { it.relative }
                    .map { it.courseId }
                datesInteractor.shiftDueDate(courseIds)
                refreshData()
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                    )
                }
            } finally {
                _uiState.update { state ->
                    state.copy(
                        isShiftDueDatesPressed = false,
                    )
                }
            }
        }
    }

    fun fetchMore() {
        if (!_uiState.value.isLoading && page != -1) {
            fetchDates(false)
        }
    }

    fun refreshData() {
        fetchDates(true)
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        datesRouter.navigateToSettings(fragmentManager)
    }

    fun navigateToCourseOutline(
        fragmentManager: FragmentManager,
        courseDate: CourseDate,
    ) {
        logEvent(DatesAnalyticsEvent.ASSIGNMENT_CLICK)
        datesRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = courseDate.courseId,
            courseTitle = courseDate.courseName,
            openTab = "",
            resumeBlockId = courseDate.assignmentBlockId
        )
    }

    private fun groupCourseDates(dates: List<CourseDate>): Map<DatesSection, List<CourseDate>> {
        val now = Date()
        val calNow = Calendar.getInstance().apply { time = now }
        val grouped = dates.groupBy { courseDate ->
            val dueDate = courseDate.dueDate
            if (dueDate.before(now)) {
                DatesSection.PAST_DUE
            } else if (dueDate.isToday()) {
                DatesSection.TODAY
            } else {
                val calDue = dueDate.toCalendar()
                val weekNow = calNow.get(Calendar.WEEK_OF_YEAR)
                val weekDue = calDue.get(Calendar.WEEK_OF_YEAR)
                val yearNow = calNow.get(Calendar.YEAR)
                val yearDue = calDue.get(Calendar.YEAR)
                if (weekNow == weekDue && yearNow == yearDue) {
                    DatesSection.THIS_WEEK
                } else {
                    DatesSection.UPCOMING
                }
            }
        }

        return grouped
    }

    private fun logEvent(
        event: DatesAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(DatesAnalyticsKey.NAME.key, event.biValue)
                putAll(params)
            }
        )
    }
}

interface DatesViewActions {
    object OpenSettings : DatesViewActions
    class OpenEvent(val date: CourseDate) : DatesViewActions
    object LoadMore : DatesViewActions
    object SwipeRefresh : DatesViewActions
    object ShiftDueDate : DatesViewActions
}
