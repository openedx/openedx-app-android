package org.openedx.course.presentation.outline

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.Certificate
import org.openedx.course.R
import org.junit.Rule
import org.junit.Test

class CourseOutlineScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    //region block
    private val mockBlock = Block(
        id = "id",
        blockId = "blockId",
        lmsWebUrl = "lmsWebUrl",
        legacyWebUrl = "legacyWebUrl",
        studentViewUrl = "studentViewUrl",
        type = BlockType.HTML,
        displayName = "Block",
        graded = false,
        studentViewData = null,
        studentViewMultiDevice = false,
        blockCounts = BlockCounts(0),
        descendants = emptyList(),
        completion = 0.0
    )
    //endregion

    @Test
    fun courseOutlineLoading() {
        composeTestRule.setContent {
            CourseOutlineScreen(
                uiState = CourseOutlineUIState.Loading,
                courseTitle = "Title",
                courseImage = "",
                courseCertificate = Certificate(""),
                uiMessage = null,
                refreshing = false,
                onSwipeRefresh = {},
                onItemClick = {},
                onResumeClick = {},
                onBackClick = {}
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

            onNode(hasText(activity.getString(R.string.course_content))).assertDoesNotExist()
        }
    }


    @Test
    fun courseOutlineLoaded() {
        composeTestRule.setContent {
            CourseOutlineScreen(
                uiState = CourseOutlineUIState.CourseData(listOf(mockBlock, mockBlock), null),
                courseTitle = "Title",
                courseImage = "",
                courseCertificate = Certificate(""),
                uiMessage = null,
                refreshing = false,
                onSwipeRefresh = {},
                onItemClick = {},
                onResumeClick = {},
                onBackClick = {}
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
            ).assertDoesNotExist()

            onNode(hasScrollAction() and hasAnyChild(hasText(mockBlock.displayName))).assertExists()
            onNode(hasScrollAction()).onChildren().assertCountEquals(3)
            onNode(hasText(activity.getString(R.string.course_content))).assertExists()
        }
    }
}