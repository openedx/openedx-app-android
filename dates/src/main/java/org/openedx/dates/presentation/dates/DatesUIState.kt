package org.openedx.dates.presentation.dates

import org.openedx.core.domain.model.CourseDate

data class DatesUIState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val dates: Map<DueDateCategory, List<CourseDate>> = emptyMap()
)
