package org.openedx.discovery.presentation

import org.openedx.core.presentation.global.ErrorType
import org.openedx.discovery.domain.model.Course

sealed class DiscoveryUIState {
    data class Courses(val courses: List<Course>) : DiscoveryUIState()
    data object Loading : DiscoveryUIState()
}
