package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.dashboard.domain.CourseStatusFilter

interface AllEnrolledCoursesAction {
    object Reload : AllEnrolledCoursesAction
    object SwipeRefresh : AllEnrolledCoursesAction
    object EndOfPage : AllEnrolledCoursesAction
    object Back : AllEnrolledCoursesAction
    object Search : AllEnrolledCoursesAction
    data class OpenCourse(val enrolledCourse: EnrolledCourse) : AllEnrolledCoursesAction
    data class FilterChange(val courseStatusFilter: CourseStatusFilter?) : AllEnrolledCoursesAction
}
