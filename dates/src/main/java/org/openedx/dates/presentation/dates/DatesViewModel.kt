package org.openedx.dates.presentation.dates

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
import org.openedx.core.domain.model.CourseDatesResponse
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.extension.isNotNull
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.isToday
import org.openedx.core.utils.toCalendar
import org.openedx.core.worker.CalendarSyncScheduler
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
    private val calendarSyncScheduler: CalendarSyncScheduler,
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
    private var fetchDataJob: Job? = null

    init {
        preloadFirstPageCachedDates()
        fetchDates(false)
    }

    private fun fetchDates(refresh: Boolean) {
        if (refresh) {
            _uiState.update { state -> state.copy(canLoadMore = true) }
            page = 1
        }
        fetchDataJob = viewModelScope.launch {
            try {
                updateLoadingState(refresh)
                val response = datesInteractor.getUserDates(page)
                updateUIWithResponse(response, refresh)
            } catch (e: Exception) {
                page = -1
                updateUIWithCachedResponse()
                handleFetchException(e)
            } finally {
                clearLoadingState()
            }
        }
    }

    private fun updateLoadingState(refresh: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLoading = !refresh,
                isRefreshing = refresh
            )
        }
    }

    private fun updateUIWithResponse(response: CourseDatesResponse, refresh: Boolean) {
        _uiState.update { state ->
            if (refresh || page == 1) {
                state.copy(dates = groupCourseDates(response.results))
            } else {
                val newDates = groupCourseDates(response.results)
                state.copy(dates = mergeDates(state.dates, newDates))
            }
        }
        if (response.next.isNotNull()) {
            _uiState.update { state -> state.copy(canLoadMore = true) }
            page++
        } else {
            _uiState.update { state -> state.copy(canLoadMore = false) }
        }
    }

    private suspend fun updateUIWithCachedResponse() {
        val cachedList = datesInteractor.getUserDatesFromCache()
        _uiState.update { state -> state.copy(canLoadMore = false) }
        _uiState.update { state ->
            state.copy(
                dates = groupCourseDates(cachedList)
            )
        }
    }

    private fun preloadFirstPageCachedDates() {
        viewModelScope.launch {
            val cachedList = datesInteractor.preloadFirstPageCachedDates()?.results ?: emptyList()
            _uiState.update { state ->
                state.copy(
                    dates = groupCourseDates(cachedList),
                    canLoadMore = true
                )
            }
        }
    }

    private suspend fun handleFetchException(e: Throwable) {
        if (e.isInternetError()) {
            _uiMessage.emit(
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
            )
        } else {
            _uiMessage.emit(
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
            )
        }
    }

    private fun clearLoadingState() {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                isRefreshing = false
            )
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
                datesInteractor.shiftDueDate()
                refreshData()
                calendarSyncScheduler.requestImmediateSync()
            } catch (e: Exception) {
                handleFetchException(e)
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
        if (!_uiState.value.isLoading &&
            !_uiState.value.isRefreshing &&
            _uiState.value.canLoadMore
        ) {
            fetchDates(false)
        }
    }

    fun refreshData() {
        fetchDataJob?.cancel()
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
            resumeBlockId = courseDate.firstComponentBlockId
        )
    }

    private fun groupCourseDates(dates: List<CourseDate>): Map<DatesSection, List<CourseDate>> {
        val now = Date()
        val calNow = Calendar.getInstance().apply { time = now }
        return dates.groupBy { courseDate ->
            when {
                courseDate.dueDate.before(now) -> DatesSection.PAST_DUE
                courseDate.dueDate.isToday() -> DatesSection.TODAY
                else -> {
                    val calDue = courseDate.dueDate.toCalendar()
                    val weekNow = calNow.get(Calendar.WEEK_OF_YEAR)
                    val weekDue = calDue.get(Calendar.WEEK_OF_YEAR)
                    val yearNow = calNow.get(Calendar.YEAR)
                    val yearDue = calDue.get(Calendar.YEAR)
                    if (weekNow == weekDue && yearNow == yearDue) {
                        DatesSection.THIS_WEEK
                    } else if (yearNow == yearDue && weekDue == weekNow + 1) {
                        DatesSection.NEXT_WEEK
                    } else {
                        DatesSection.UPCOMING
                    }
                }
            }
        }
    }

    private fun mergeDates(
        oldDates: Map<DatesSection, List<CourseDate>>,
        newDates: Map<DatesSection, List<CourseDate>>
    ): Map<DatesSection, List<CourseDate>> {
        val merged = oldDates.toMutableMap()
        newDates.forEach { (section, newList) ->
            val existingList = merged[section] ?: emptyList()
            merged[section] = existingList + newList
        }
        return merged
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
