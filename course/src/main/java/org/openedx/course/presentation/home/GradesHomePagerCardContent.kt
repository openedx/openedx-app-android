package org.openedx.course.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseHomeGradesEmptyState
import org.openedx.course.presentation.progress.CurrentOverallGradeText
import org.openedx.course.presentation.progress.GradeProgressBar
import org.openedx.course.presentation.progress.RequiredGradeMarker

@Composable
fun GradesHomePagerCardContent(
    uiState: CourseHomeUIState.CourseData,
    onViewProgressClick: () -> Unit
) {
    val courseProgress = uiState.courseProgress
    val gradingPolicy = courseProgress?.gradingPolicy
    val assignmentPolicies = courseProgress?.getNotEmptyGradingPolicies()
    val requiredGradeString = stringResource(
        R.string.course_progress_required_grade_percent,
        courseProgress?.requiredGradePercent.toString()
    )

    if (courseProgress == null || gradingPolicy == null || assignmentPolicies.isNullOrEmpty()) {
        CourseHomeGradesEmptyState()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.course_grades_title),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.course_grades_description),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        CurrentOverallGradeText(progress = courseProgress)
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .semantics {
                    contentDescription = requiredGradeString
                }
        ) {
            GradeProgressBar(
                progress = courseProgress,
                gradingPolicy = gradingPolicy,
                notCompletedWeightedGradePercent = courseProgress.getNotCompletedWeightedGradePercent()
            )
            RequiredGradeMarker(progress = courseProgress)
        }
        Spacer(modifier = Modifier.height(20.dp))
        GradeCardsGrid(
            assignmentPolicies = assignmentPolicies,
            assignmentColors = gradingPolicy.assignmentColors,
            progress = courseProgress,
            courseStructure = uiState.courseStructure
        )
        Spacer(modifier = Modifier.height(8.dp))
        ViewAllButton(
            text = stringResource(R.string.course_view_progress),
            onClick = onViewProgressClick,
        )
    }
}

@Composable
private fun GradeCard(
    policy: CourseProgress.GradingPolicy.AssignmentPolicy,
    progress: CourseProgress,
    courseStructure: CourseStructure?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val assignments = progress.getAssignmentSections(policy.type)
    val earned = progress.getCompletedAssignmentCount(policy, courseStructure)
    val possible = assignments.size
    val gradePercent = if (possible > 0) (earned.toFloat() / possible * 100).toInt() else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        backgroundColor = color.copy(alpha = 0.1f),
        shape = MaterialTheme.appShapes.material.small,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 10.dp)
        ) {
            // Assignment type title
            Text(
                text = policy.type,
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Grade percentage with colored bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .background(
                            color = color,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "$gradePercent%",
                        style = MaterialTheme.appTypography.bodyLarge,
                        color = MaterialTheme.appColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.course_progress_earned_possible_assignment_problems,
                            earned,
                            possible
                        ),
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeCardsGrid(
    assignmentPolicies: List<CourseProgress.GradingPolicy.AssignmentPolicy>,
    assignmentColors: List<Color>,
    progress: CourseProgress,
    courseStructure: CourseStructure?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group policies into rows of 2
        assignmentPolicies.chunked(2).forEach { rowPolicies ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPolicies.forEachIndexed { index, policy ->
                    val policyIndex = assignmentPolicies.indexOf(policy)
                    GradeCard(
                        modifier = Modifier.weight(1f),
                        policy = policy,
                        progress = progress,
                        courseStructure = courseStructure,
                        color = if (assignmentColors.isNotEmpty()) {
                            assignmentColors[policyIndex % assignmentColors.size]
                        } else {
                            MaterialTheme.appColors.primary
                        },
                    )
                }
                // Fill remaining space if row has only 1 item
                if (rowPolicies.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
