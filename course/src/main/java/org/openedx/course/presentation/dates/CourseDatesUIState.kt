package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDatesResult

sealed interface CourseDatesUIState {
    data class CourseDates(
        val courseDatesResult: CourseDatesResult,
    ) : CourseDatesUIState

    data object Empty : CourseDatesUIState
    data object Loading : CourseDatesUIState
}
