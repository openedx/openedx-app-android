package org.openedx.course.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.openedx.core.domain.model.*
import org.openedx.course.R
import org.junit.Rule
import org.junit.Test

class CourseDetailsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    //region course

    private val course = Course(
        id = "id",
        blocksUrl = "blocksUrl",
        courseId = "courseId",
        effort = "effort",
        enrollmentStart = "enrollmentStart",
        enrollmentEnd = "enrollmentEnd",
        hidden = false,
        invitationOnly = false,
        media = Media(),
        mobileAvailable = true,
        name = "Test course",
        number = "number",
        org = "EdX",
        pacing = "pacing",
        shortDescription = "shortDescription",
        start = "start",
        end = "end",
        startDisplay = "startDisplay",
        startType = "startType",
        overview = ""
    )

    private val enrolledCourse = EnrolledCourse(
        auditAccessExpires = "",
        created = "created",
        certificate = Certificate(""),
        mode = "mode",
        isActive = true,
        course = EnrolledCourseData(
            id = "id",
            name = "name",
            number = "",
            org = "Org",
            start = "",
            startDisplay = "",
            startType = "",
            end = "Ending in 22 November",
            dynamicUpgradeDeadline = "",
            subscriptionId = "",
            coursewareAccess = CoursewareAccess(
                true,
                "",
                "",
                "",
                "",
                ""
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
    )

    //endregion

    @Test
    fun courseDetailsScreenLoading() {
        composeTestRule.setContent {
            CourseDetailsScreen(
                uiState = CourseDetailsUIState.Loading,
                uiMessage = null,
                htmlBody = "",
                onBackClick = {},
                onButtonClick = {}
            )
        }

        with(composeTestRule) {
            onNode(
                hasProgressBarRangeInfo(
                    ProgressBarRangeInfo(
                        current = 0f,
                        range = 0f..0f,
                        steps = 0
                    )
                )
            ).assertExists()

            onNode(
                hasClickAction() and hasTextExactly(
                    activity.getString(R.string.course_view_course),
                    activity.getString(R.string.course_register_now)
                )
            ).assertDoesNotExist()
        }
    }

    @Test
    fun courseDetailsScreenLoaded() {
        composeTestRule.setContent {
            CourseDetailsScreen(
                uiState = CourseDetailsUIState.CourseData(course, enrolledCourse),
                uiMessage = null,
                htmlBody = "",
                onBackClick = {},
                onButtonClick = {}
            )
        }

        with(composeTestRule) {
            onNode(
                hasClickAction() and hasText(
                    activity.getString(R.string.course_view_course),
                    ignoreCase = true
                )
            ).assertExists()
        }
    }
}