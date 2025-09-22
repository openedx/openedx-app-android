package org.openedx.course.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.Mock
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.progress.CourseCompletionCircularProgress
import org.openedx.course.presentation.ui.CourseSection

@Composable
fun CourseCompletionHomePagerCardContent(
    modifier: Modifier = Modifier,
    uiState: CourseHomeUIState.CourseData,
    onViewAllContentClick: () -> Unit,
    onDownloadClick: (blockIds: List<String>) -> Unit,
    onSubSectionClick: (Block) -> Unit,
) {
    val courseProgress = uiState.courseProgress?.completion ?: 0f
    val courseProgressPercent = uiState.courseProgress?.completionPercent ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.course_completion_title),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.course_completion_progress_label),
                    style = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.textDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.course_completion_progress_description,
                        courseProgressPercent
                    ),
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textDark
                )
            }

            // Circular Progress
            CourseCompletionCircularProgress(
                progress = courseProgress,
                progressPercent = courseProgressPercent,
                completedText = stringResource(R.string.course_completion_completed)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        uiState.next?.let { (chapter, subsection) ->
            // Section progress
            val subSections = uiState.courseSubSections[chapter.id]
            val completedCount = subSections?.count { it.isCompleted() } ?: 0
            val totalCount = subSections?.size ?: 0
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

            CourseSection(
                section = chapter,
                onItemClick = {
                    onSubSectionClick(subsection)
                },
                isExpandable = false,
                isSectionVisible = true,
                showDueDate = false,
                useRelativeDates = uiState.useRelativeDates,
                subSections = listOf(subsection),
                downloadedStateMap = uiState.downloadedState,
                onSubSectionClick = onSubSectionClick,
                onDownloadClick = onDownloadClick,
                progress = progress,
                background = MaterialTheme.appColors.background
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View All Content Button
        ViewAllButton(
            text = stringResource(R.string.course_completion_view_all_content),
            onClick = onViewAllContentClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
private fun CourseCompletionHomePagerCardContentPreview() {
    OpenEdXTheme {
        CourseCompletionHomePagerCardContent(
            uiState = CourseHomeUIState.CourseData(
                courseStructure = Mock.mockCourseStructure,
                courseProgress = null, // No course progress for preview
                next = Pair(Mock.mockChapterBlock, Mock.mockChapterBlock), // Mock next section
                downloadedState = mapOf(),
                resumeComponent = Mock.mockChapterBlock,
                resumeUnitTitle = "Resumed Unit",
                courseSubSections = mapOf(),
                subSectionsDownloadsCount = mapOf(),
                datesBannerInfo = CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                ),
                useRelativeDates = true,
                courseVideos = mapOf(),
                courseAssignments = emptyList(),
                videoPreview = null,
                videoProgress = 0f
            ),
            onViewAllContentClick = {},
            onDownloadClick = {},
            onSubSectionClick = {},
        )
    }
}
