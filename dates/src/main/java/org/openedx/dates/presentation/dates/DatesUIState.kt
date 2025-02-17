package org.openedx.dates.presentation.dates

data class DatesUIState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val dates: Map<DueDateCategory, List<String>> = emptyMap()
)
