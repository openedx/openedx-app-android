package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.DatesSection

sealed class DatesUIState {
    data class Dates(val courseDates: LinkedHashMap<DatesSection, List<CourseDateBlock>>) :
        DatesUIState()

    object Empty : DatesUIState()
    object Loading : DatesUIState()
}
