package org.openedx.dates.presentation.dates

import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.DatesSection

data class DatesUIState(
    val isLoading: Boolean = true,
    val isShiftDueDatesPressed: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val dates: Map<DatesSection, List<CourseDate>> = emptyMap()
)
