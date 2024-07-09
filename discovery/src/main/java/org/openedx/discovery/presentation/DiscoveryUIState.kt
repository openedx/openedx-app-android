package org.openedx.discovery.presentation

import org.openedx.core.presentation.global.ErrorType
import org.openedx.discovery.domain.model.Course

sealed class DiscoveryUIState {
    data class Courses(val courses: List<Course>) : DiscoveryUIState()
    data object Loading : DiscoveryUIState()
    data object Loaded : DiscoveryUIState()
    data class Error(val errorType: ErrorType) : DiscoveryUIState()
}

enum class DiscoveryUIAction {
    WEB_PAGE_LOADED,
    WEB_PAGE_ERROR,
    RELOAD_WEB_PAGE
}

