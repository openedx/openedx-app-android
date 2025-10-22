package org.openedx.course.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.Block
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentVideoEmptyState
import org.openedx.course.presentation.ui.CourseVideoItem

@Composable
fun VideosHomePagerCardContent(
    uiState: CourseHomeUIState.CourseData,
    onVideoClick: (Block) -> Unit,
    onViewAllVideosClick: () -> Unit
) {
    val allVideos = uiState.courseVideos.values.flatten()
    if (allVideos.isEmpty()) {
        CourseContentVideoEmptyState(
            onReturnToCourseClick = {},
            showReturnButton = false
        )
        return
    }

    val completedVideos = allVideos.count { it.isCompleted() }
    val totalVideos = allVideos.size
    val firstIncompleteVideo = allVideos.find { !it.isCompleted() }
    val videoProgress = uiState.videoProgress ?: if (firstIncompleteVideo?.isCompleted() ?: false) {
        1f
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with progress
        Text(
            text = stringResource(R.string.course_container_content_tab_video),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$completedVideos/$totalVideos",
                style = MaterialTheme.appTypography.displaySmall,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.course_videos_completed),
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textPrimaryVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            progress = if (totalVideos > 0) completedVideos.toFloat() / totalVideos else 0f,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Continue Watching section
        if (firstIncompleteVideo != null) {
            val title = if (videoProgress > 0) {
                stringResource(R.string.course_continue_watching)
            } else {
                stringResource(R.string.course_next_video)
            }
            Text(
                text = title,
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Video card using CourseVideoItem
            CourseVideoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                videoBlock = firstIncompleteVideo,
                preview = uiState.videoPreview,
                progress = videoProgress,
                onClick = {
                    onVideoClick(firstIncompleteVideo)
                },
                titleStyle = MaterialTheme.appTypography.titleMedium,
                contentModifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                progressModifier = Modifier.height(8.dp),
            )
        } else {
            CaughtUpMessage(
                message = stringResource(R.string.course_videos_caught_up)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View All Videos button
        ViewAllButton(
            text = stringResource(R.string.course_view_all_videos),
            onClick = onViewAllVideosClick
        )
    }
}
