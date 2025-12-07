package org.openedx.dashboard

import org.openedx.core.data.model.DateType
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CourseAssignments
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseEnrollments
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStatus
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.domain.model.Pagination
import org.openedx.core.domain.model.Progress
import java.util.Date

object DashboardMocks {
    private val courseDateBlock = CourseDateBlock(
        complete = false,
        date = Date(),
        dateType = DateType.NONE,
        description = "Assignment due"
    )

    private val courseAssignments = CourseAssignments(
        futureAssignments = listOf(courseDateBlock, courseDateBlock),
        pastAssignments = listOf(courseDateBlock)
    )

    private val courseData = EnrolledCourseData(
        id = "courseId",
        name = "Introduction to Testing",
        number = "CS101",
        org = "OpenEdX",
        start = Date(),
        startDisplay = "Jan 01",
        startType = "",
        end = Date(),
        dynamicUpgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            hasAccess = true,
            errorCode = "",
            developerMessage = "",
            userMessage = "",
            userFragment = "",
            additionalContextUserMessage = ""
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    )

    val enrolledCourse = EnrolledCourse(
        auditAccessExpires = Date(),
        created = "created",
        mode = "audit",
        isActive = true,
        course = courseData,
        certificate = null,
        progress = Progress.DEFAULT_PROGRESS,
        courseStatus = CourseStatus(
            lastVisitedModuleId = "",
            lastVisitedModulePath = emptyList(),
            lastVisitedBlockId = "",
            lastVisitedUnitDisplayName = "Unit name"
        ),
        courseAssignments = courseAssignments
    )

    fun enrolledCourses(count: Int) = List(count) { enrolledCourse }

    private val pagination = Pagination(
        count = 10,
        next = "",
        numPages = 4,
        previous = "1"
    )

    val dashboardCourseList = DashboardCourseList(
        pagination = pagination,
        courses = enrolledCourses(6)
    )

    val courseEnrollments = CourseEnrollments(
        enrollments = dashboardCourseList,
        configs = AppConfig(
            courseDatesCalendarSync = CourseDatesCalendarSync(
                isEnabled = true,
                isSelfPacedEnabled = true,
                isInstructorPacedEnabled = true,
                isDeepLinkEnabled = true
            )
        ),
        primary = enrolledCourse
    )
}
