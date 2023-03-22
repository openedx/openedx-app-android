package com.raccoongang.discovery.presentation

import com.raccoongang.core.domain.model.Course

sealed class DiscoveryUIState {
    data class Courses(val courses: List<Course>) : DiscoveryUIState()
    object Loading : DiscoveryUIState()
}