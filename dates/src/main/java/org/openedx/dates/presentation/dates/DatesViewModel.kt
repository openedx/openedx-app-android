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
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.CourseDatesResponse
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.isToday
import org.openedx.core.utils.toCalendar
import org.openedx.dates.domain.interactor.DatesInteractor
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
    private val datesInteractor: DatesInteractor
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DatesUIState())
    val uiState: StateFlow<DatesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

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
                val courseDatesResponse = datesInteractor.getUserDates()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        dates = groupCourseDates(courseDatesResponse)
                    )
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
            }
        }
    }

    fun refreshData() {
        fetchDates(true)
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        datesRouter.navigateToSettings(fragmentManager)
    }

    private fun groupCourseDates(response: CourseDatesResponse): Map<DueDateCategory, List<CourseDate>> {
        val now = Date()
        val calNow = Calendar.getInstance().apply { time = now }
        val grouped = response.results.groupBy { courseDate ->
            val dueDate = courseDate.dueDate
            if (dueDate.before(now)) {
                DueDateCategory.PAST_DUE
            } else if (dueDate.isToday()) {
                DueDateCategory.TODAY
            } else {
                val calDue = dueDate.toCalendar()
                val weekNow = calNow.get(Calendar.WEEK_OF_YEAR)
                val weekDue = calDue.get(Calendar.WEEK_OF_YEAR)
                val yearNow = calNow.get(Calendar.YEAR)
                val yearDue = calDue.get(Calendar.YEAR)
                if (weekNow == weekDue && yearNow == yearDue) {
                    DueDateCategory.THIS_WEEK
                } else {
                    DueDateCategory.UPCOMING
                }
            }
        }

        return grouped
    }
}

interface DatesViewActions {
    object OpenSettings : DatesViewActions
    class OpenEvent() : DatesViewActions
    object SwipeRefresh : DatesViewActions
}
