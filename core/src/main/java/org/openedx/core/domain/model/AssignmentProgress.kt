package org.openedx.core.domain.model

data class AssignmentProgress(
    val assignmentType: String,
    val numPointsEarned: Float,
    val numPointsPossible: Float
)
