package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDateBlock

sealed class DatesUIState {
    data class Dates(val courseDates: LinkedHashMap<String, ArrayList<CourseDateBlock>>) :
        DatesUIState()

    object Empty : DatesUIState()
    object Loading : DatesUIState()
}
