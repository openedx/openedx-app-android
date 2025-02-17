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
import org.openedx.dates.presentation.DatesRouter
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage

class DatesViewModel(
    private val datesRouter: DatesRouter,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DatesUIState())
    val uiState: StateFlow<DatesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    init {
        fetchDates()
    }

    private fun fetchDates() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    dates = mapOf(
                        DueDateCategory.PAST_DUE to listOf("Date1", "Date2", "Date3"),
                        DueDateCategory.TODAY to listOf("Date1"),
                        DueDateCategory.THIS_WEEK to listOf("Date1", "Date2"),
                        DueDateCategory.UPCOMING to listOf("Date1", "Date2", "Date3", "Date4"),
                    )
                )
            }
        }
    }

    fun refreshData() {
        _uiState.update { state ->
            state.copy(
                isRefreshing = true,
            )
        }
        fetchDates()
    }

    fun onSettingsClick(fragmentManager: FragmentManager) {
        datesRouter.navigateToSettings(fragmentManager)
    }
}

interface DatesViewActions {
    object OpenSettings : DatesViewActions
    class OpenEvent() : DatesViewActions
    object SwipeRefresh : DatesViewActions
}
