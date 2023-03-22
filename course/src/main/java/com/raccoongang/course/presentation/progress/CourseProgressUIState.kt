package com.raccoongang.course.presentation.progress

import com.raccoongang.core.domain.model.CourseProgress

sealed class CourseProgressUIState {
    data class Data(
        val sections: List<CourseProgress.Section>,
        val progress: Int,
    ) : CourseProgressUIState()

    object Loading : CourseProgressUIState()
}
