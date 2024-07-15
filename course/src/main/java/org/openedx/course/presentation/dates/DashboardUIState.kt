package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDatesResult

sealed class DatesUIState {
    data class Dates(val courseDatesResult: CourseDatesResult) : DatesUIState()
    data object Error : DatesUIState()
    data object Loading : DatesUIState()
}
