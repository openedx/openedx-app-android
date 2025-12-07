package org.openedx.course.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.Block
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentAssignmentEmptyState
import java.util.Date
import org.openedx.core.R as coreR

private const val MILLISECONDS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

private const val MILLISECONDS_PER_DAY =
    MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY

@Composable
fun AssignmentsHomePagerCardContent(
    uiState: CourseHomeUIState.CourseData,
    onAssignmentClick: (Block) -> Unit,
    onViewAllAssignmentsClick: () -> Unit,
    getBlockParent: (blockId: String) -> Block?,
) {
    if (uiState.courseAssignments.isEmpty()) {
        CourseContentAssignmentEmptyState(
            onReturnToCourseClick = {},
            showReturnButton = false
        )
        return
    }

    val completedAssignments = uiState.courseAssignments.count { it.isCompleted() }
    val totalAssignments = uiState.courseAssignments.size
    val firstIncompleteAssignment = uiState.courseAssignments.find { !it.isCompleted() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with progress
        Text(
            text = stringResource(R.string.course_container_content_tab_assignment),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Progress section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$completedAssignments/$totalAssignments",
                style = MaterialTheme.appTypography.displaySmall,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.course_assignments_completed),
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
            progress = if (totalAssignments > 0) completedAssignments.toFloat() / totalAssignments else 0f,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // First Incomplete Assignment section
        if (firstIncompleteAssignment != null) {
            AssignmentCard(
                assignment = firstIncompleteAssignment,
                sectionName = getBlockParent(firstIncompleteAssignment.id)?.displayName ?: "",
                onAssignmentClick = onAssignmentClick,
                background = MaterialTheme.appColors.background,
            )
        } else {
            CaughtUpMessage(
                message = stringResource(R.string.course_assignments_caught_up)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View All Assignments button
        ViewAllButton(
            text = stringResource(R.string.course_view_all_assignments),
            onClick = onViewAllAssignmentsClick
        )
    }
}

@Composable
private fun AssignmentCard(
    assignment: Block,
    sectionName: String,
    onAssignmentClick: (Block) -> Unit,
    background: Color = MaterialTheme.appColors.surface
) {
    val isDuePast = assignment.due != null && assignment.due!! < Date()

    // Header text - "Past Due" or "Due Soon"
    val headerText = if (isDuePast) {
        stringResource(coreR.string.core_date_type_past_due)
    } else {
        stringResource(R.string.course_next_assignment)
    }

    // Due date status text
    val dueDateStatusText = assignment.due?.let { due ->
        val formattedDate = TimeUtils.formatToMonthDay(due)
        val daysDifference = ((due.time - Date().time) / MILLISECONDS_PER_DAY).toInt()
        when {
            daysDifference < 0 -> {
                // Past due
                val daysPastDue = -daysDifference
                stringResource(
                    R.string.course_days_past_due,
                    daysPastDue,
                    formattedDate
                )
            }

            daysDifference == 0 -> {
                // Due today
                stringResource(
                    R.string.course_due_today,
                    formattedDate
                )
            }

            else -> {
                // Due in the future
                stringResource(
                    R.string.course_due_in_days,
                    daysDifference,
                    formattedDate
                )
            }
        }
    } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAssignmentClick(assignment) },
        backgroundColor = background,
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardViewBorder),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header section with icon and status
            if (assignment.due != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDuePast) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.appColors.warning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.appTypography.titleMedium,
                        color = MaterialTheme.appColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Due date status text
                    if (dueDateStatusText.isNotEmpty()) {
                        Text(
                            text = dueDateStatusText,
                            style = MaterialTheme.appTypography.labelSmall,
                            color = MaterialTheme.appColors.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Assignment and section name
                    Text(
                        text = assignment.displayName,
                        style = MaterialTheme.appTypography.titleSmall,
                        color = MaterialTheme.appColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sectionName,
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimaryVariant,
                    )
                }

                // Chevron arrow
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.appColors.textDark
                )
            }
        }
    }
}
