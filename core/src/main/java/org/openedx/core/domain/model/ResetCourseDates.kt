package org.openedx.core.domain.model

data class ResetCourseDates(
    val message: String,
    val body: String,
    val header: String,
    val link: String,
    val linkText: String,
)
