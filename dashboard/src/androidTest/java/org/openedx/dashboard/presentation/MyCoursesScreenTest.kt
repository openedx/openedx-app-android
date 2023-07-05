package org.openedx.dashboard.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.openedx.core.domain.model.*
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.junit.Rule
import org.junit.Test

class MyCoursesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    //region mockEnrolledCourse
    private val mockCourseEnrolled = EnrolledCourse(
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
    fun dashboardScreenLoading() {
        composeTestRule.setContent {
            MyCoursesScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                DashboardUIState.Loading,
                null,
                refreshing = false,
                onItemClick = {},
                onSwipeRefresh = {}
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
            )
        }
    }

    @Test
    fun dashboardScreenLoaded() {
        composeTestRule.setContent {
            MyCoursesScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                DashboardUIState.Courses(listOf(mockCourseEnrolled, mockCourseEnrolled)),
                null,
                refreshing = false,
                onItemClick = {},
                onSwipeRefresh = {}
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren()
                .assertAny(hasText(mockCourseEnrolled.course.name))
        }
    }

    @Test
    fun dashboardScreenRefreshing() {
        composeTestRule.setContent {
            MyCoursesScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                DashboardUIState.Courses(listOf(mockCourseEnrolled, mockCourseEnrolled)),
                null,
                refreshing = true,
                onItemClick = {},
                onSwipeRefresh = {}
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren()
                .assertAny(hasText(mockCourseEnrolled.course.name))
            onNode(
                hasScrollAction().and(
                    hasAnyChild(
                        hasProgressBarRangeInfo(
                            ProgressBarRangeInfo(
                                current = 0f,
                                range = 0f..0f,
                                steps = 0
                            )
                        )
                    )
                )
            )
        }
    }

}