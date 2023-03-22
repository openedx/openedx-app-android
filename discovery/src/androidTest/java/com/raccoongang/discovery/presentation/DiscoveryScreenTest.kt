package com.raccoongang.discovery.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.Media
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.WindowType
import org.junit.Rule
import org.junit.Test

class DiscoveryScreenTest {

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

    //endregion

    @Test
    fun discoveryScreenLoading() {
        composeTestRule.setContent {
            DiscoveryScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                state = DiscoveryUIState.Loading,
                uiMessage = null,
                canLoadMore = false,
                refreshing = false,
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {}
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
    fun discoveryScreenLoaded() {
        composeTestRule.setContent {
            DiscoveryScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                state = DiscoveryUIState.Courses(listOf(course)),
                uiMessage = null,
                canLoadMore = false,
                refreshing = false,
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {}
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren().assertAny(hasText(course.name))
        }
    }

    @Test
    fun discoveryScreenPaginationAvailable() {
        composeTestRule.setContent {
            DiscoveryScreen(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                state = DiscoveryUIState.Courses(listOf(course)),
                uiMessage = null,
                canLoadMore = true,
                refreshing = false,
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {}
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren().assertAny(hasText(course.name))
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