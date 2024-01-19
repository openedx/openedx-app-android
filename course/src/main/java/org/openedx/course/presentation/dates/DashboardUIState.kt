package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDatesResult

sealed class DatesUIState {
    data class Dates(
        val courseDatesResult: CourseDatesResult,
    ) : DatesUIState()

    object Empty : DatesUIState()
    object Loading : DatesUIState()
}
