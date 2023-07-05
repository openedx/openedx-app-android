package org.openedx.discovery.presentation

import org.openedx.core.domain.model.Course

sealed class DiscoveryUIState {
    data class Courses(val courses: List<Course>) : DiscoveryUIState()
    object Loading : DiscoveryUIState()
}