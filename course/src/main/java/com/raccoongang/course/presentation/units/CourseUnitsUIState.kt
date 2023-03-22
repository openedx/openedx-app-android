package com.raccoongang.course.presentation.units

import com.raccoongang.core.domain.model.Block

sealed class CourseUnitsUIState {
    data class Blocks(val blocks: List<Block>) : CourseUnitsUIState()
    object Loading : CourseUnitsUIState()
}