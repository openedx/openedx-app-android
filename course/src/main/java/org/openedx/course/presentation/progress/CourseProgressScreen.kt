package org.openedx.course.presentation.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.NoContentScreenType
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.ui.CircularProgress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.windowSizeValue

@Composable
fun CourseProgressScreen(
    windowSize: WindowSize,
    viewModel: CourseProgressViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState(null)

    when (val state = uiState) {
        is CourseProgressUIState.Loading -> CircularProgress()
        is CourseProgressUIState.Error -> NoContentScreen(NoContentScreenType.COURSE_PROGRESS)
        is CourseProgressUIState.Data -> CourseProgressContent(
            uiState = state,
            uiMessage = uiMessage,
            windowSize = windowSize,
        )
    }
}

@Composable
private fun CourseProgressContent(
    uiState: CourseProgressUIState.Data,
    uiMessage: UIMessage?,
    windowSize: WindowSize
) {
    val scaffoldState = rememberScaffoldState()
    val gradingPolicy = uiState.progress.gradingPolicy

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        CourseCompletionView(
                            progress = uiState.progress
                        )
                    }
                    if (gradingPolicy == null) return@LazyColumn
                    if (gradingPolicy.assignmentPolicies.isNotEmpty()) {
                        item {
                            OverallGradeView(
                                progress = uiState.progress,
                            )
                        }
                        item {
                            GradeDetailsHeaderView()
                        }
                        itemsIndexed(gradingPolicy.assignmentPolicies) { index, policy ->
                            AssignmentTypeRow(
                                progress = uiState.progress,
                                policy = policy,
                                color = if (gradingPolicy.assignmentColors.isNotEmpty()) {
                                    gradingPolicy.assignmentColors[index % gradingPolicy.assignmentColors.size]
                                } else {
                                    MaterialTheme.appColors.primary
                                }
                            )
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                        item {
                            GradeDetailsFooterView(
                                progress = uiState.progress
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NoGradesView()
                            }
                        }
                    }
                }
            }

            HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)
        }
    }
}

@Composable
private fun NoGradesView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(60.dp),
            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.appColors.divider
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.course_progress_no_assignments),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textDark,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GradeDetailsHeaderView() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.course_progress_grade_details),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textDark,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.course_progress_assignment_type),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.textPrimaryVariant,
            )
            Text(
                text = stringResource(R.string.course_progress_current_max),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.textPrimaryVariant,
            )
        }
    }
}

@Composable
private fun GradeDetailsFooterView(
    progress: CourseProgress,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.course_progress_current_overall),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textDark,
        )
        Text(
            text = "${progress.getTotalWeightPercent().toInt()}%",
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OverallGradeView(
    progress: CourseProgress,
) {
    val gradingPolicy = progress.gradingPolicy
    if (gradingPolicy == null) return
    val notCompletedWeightedGradePercent = progress.getNotCompletedWeightedGradePercent()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.course_progress_overall_title),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textDark,
        )
        Text(
            text = stringResource(R.string.course_progress_overall_description),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textDark,
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.appColors.textDark,
                        fontSize = MaterialTheme.appTypography.labelMedium.fontSize,
                        fontFamily = MaterialTheme.appTypography.labelMedium.fontFamily,
                        fontWeight = MaterialTheme.appTypography.labelMedium.fontWeight
                    )
                ) {
                    append(stringResource(R.string.course_progress_current_overall) + " ")
                }
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.appColors.primary,
                        fontSize = MaterialTheme.appTypography.labelMedium.fontSize,
                        fontFamily = MaterialTheme.appTypography.labelMedium.fontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("${progress.getTotalWeightPercent().toInt()}%")
                }
            },
            style = MaterialTheme.appTypography.labelMedium,
        )

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.appColors.gradeProgressBarBorder,
                        shape = CircleShape
                    )
            ) {
                gradingPolicy.assignmentPolicies.forEachIndexed { index, assignmentPolicy ->
                    val assignmentColors = gradingPolicy.assignmentColors
                    val color = if (assignmentColors.isNotEmpty()) {
                        assignmentColors[
                            gradingPolicy.assignmentPolicies.indexOf(
                                assignmentPolicy
                            ) % assignmentColors.size
                        ]
                    } else {
                        MaterialTheme.appColors.primary
                    }
                    val weightedPercent =
                        progress.getAssignmentWeightedGradedPercent(assignmentPolicy)
                    if (weightedPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(weightedPercent)
                                .background(color)
                                .fillMaxHeight()
                        )

                        // Add black separator between assignment policies (except after the last one)
                        if (index < gradingPolicy.assignmentPolicies.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .background(Color.Black)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
                if (notCompletedWeightedGradePercent > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(notCompletedWeightedGradePercent)
                            .fillMaxHeight()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.requiredGrade),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier.offset(x = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_course_marker),
                        tint = MaterialTheme.appColors.warning,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier
                            .offset(y = 2.dp)
                            .clearAndSetSemantics { },
                        text = "${progress.requiredGradePercent}%",
                        style = MaterialTheme.appTypography.labelMedium,
                        color = MaterialTheme.appColors.textDark,
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.appColors.cardViewBackground,
            shape = MaterialTheme.appShapes.cardShape,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.appColors.warning
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.warning,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.course_progress_required_grade_percent,
                        progress.requiredGradePercent.toString()
                    ),
                    style = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.textDark,
                )
            }
        }
    }
}

@Composable
private fun CourseCompletionView(
    progress: CourseProgress
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.course_progress_completion_title),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark,
            )
            Text(
                text = stringResource(R.string.course_progress_completion_description),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textDark,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics(mergeDescendants = true) {}
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(100.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.appColors.progressBarBackgroundColor,
                        shape = CircleShape
                    )
                    .padding(3.dp),
                progress = progress.completion,
                color = MaterialTheme.appColors.primary,
                backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${progress.completionPercent}%",
                    style = MaterialTheme.appTypography.headlineSmall,
                    color = MaterialTheme.appColors.primary,
                )
                Text(
                    text = stringResource(R.string.course_completed),
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant,
                )
            }
        }
    }
}

@Composable
private fun AssignmentTypeRow(
    progress: CourseProgress,
    policy: CourseProgress.GradingPolicy.AssignmentPolicy,
    color: Color
) {
    val earned = progress.getEarnedAssignmentProblems(policy)
    val possible = progress.getPossibleAssignmentProblems(policy)
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            text = policy.type,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(7.dp)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.course_progress_earned_possible_assignment_problems,
                        earned.toInt(),
                        possible.toInt()
                    ),
                    style = MaterialTheme.appTypography.bodySmall,
                    color = MaterialTheme.appColors.textDark,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${(policy.weight * 100).toInt()}%")
                        }
                        append(" ")
                        append(stringResource(R.string.course_progress_of_grade))
                    },
                    style = MaterialTheme.appTypography.bodySmall,
                    color = MaterialTheme.appColors.textDark,
                )
            }
            Text(
                stringResource(
                    R.string.course_progress_current_and_max_weighted_graded_percent,
                    progress.getAssignmentWeightedGradedPercent(policy).toInt(),
                    (policy.weight * 100).toInt()
                ),
                style = MaterialTheme.appTypography.bodyLarge,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.appColors.textDark,
            )
        }
    }
}
