package org.openedx.core.domain.model

/**
 * Course Enrollment modes
 */
enum class EnrollmentMode(private val mode: String) {
    AUDIT("audit"),
    VERIFIED("verified"),
    NONE("none");

    override fun toString(): String {
        return mode
    }
}
