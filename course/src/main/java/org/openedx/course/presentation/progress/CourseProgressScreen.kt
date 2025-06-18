package org.openedx.course.presentation.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
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
    fragmentManager: FragmentManager,
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
    // Mocked values for demo, replace with real data from progress
    val completionPercent = 0.5f // 50% completed
    val overallGrade = 0.52f // 52%
    val gradeCutoff = uiState.progress.courseDetails.gradeCutoffs["A"] ?: 0.75f
    val requiredGradePercent = (gradeCutoff * 100).toInt()
    val currentGradePercent = (overallGrade * 100).toInt()

    val scaffoldState = rememberScaffoldState()

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

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState())
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = screenWidth,
                color = MaterialTheme.appColors.background
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CourseCompletionView(completionPercent)
                        OverallGradeView(
                            courseProgress = uiState.progress,
                            currentGradePercent = currentGradePercent,
                            overallGrade = overallGrade,
                            gradeCutoff = gradeCutoff,
                            requiredGradePercent = requiredGradePercent
                        )
                        GradeDetailsView(uiState.progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeDetailsView(
    courseProgress: CourseProgress,
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
    courseProgress.courseDetails.graders.forEach { grader ->
        AssignmentTypeRow(grader)
    }
    Spacer(modifier = Modifier.height(8.dp))
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
            text = "777%",
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OverallGradeView(
    courseProgress: CourseProgress,
    currentGradePercent: Int,
    overallGrade: Float,
    gradeCutoff: Float,
    requiredGradePercent: Int,
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
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.course_progress_current_overall),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textDark,
        )
        Text(
            text = "${currentGradePercent}%",
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.primary,
            fontWeight = FontWeight.SemiBold
        )
    }

    Column {
        val weightSum = courseProgress.courseDetails.gradeCutoffs.values.sum()
        val weightMax = courseProgress.courseDetails.gradeCutoffs.size
        val left = weightMax - weightSum
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.appColors.cardViewBackground,
                    shape = CircleShape
                )
        ) {
            courseProgress.courseDetails.gradeCutoffs.toList().forEach { grade ->
                val color =
                    courseProgress.courseDetails.graders.find { it.id == grade.first }?.color
                        ?: Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(grade.second)
                        .background(color)
                        .fillMaxHeight()
                )
            }
            Box(
                modifier = Modifier
                    .weight(left)
                    .fillMaxHeight()
            )
        }
        if (left > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((weightSum + left / 2) / weightMax),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.course_ic_marker),
                        tint = MaterialTheme.appColors.warning,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.offset(y = 2.dp),
                        text = "$requiredGradePercent%",
                        style = MaterialTheme.appTypography.labelMedium,
                        color = MaterialTheme.appColors.textDark,
                    )
                }
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
                    requiredGradePercent
                ),
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textDark,
            )
        }
    }
}

@Composable
private fun CourseCompletionView(
    completionPercent: Float
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
                progress = completionPercent,
                color = MaterialTheme.appColors.progressBarColor,
                backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(completionPercent * 100).toInt()}%",
                    style = MaterialTheme.appTypography.headlineSmall,
                    color = MaterialTheme.appColors.primary,
                )
                Text(
                    text = "Status",
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant,
                )
            }
        }
    }
}

@Composable
private fun AssignmentTypeRow(grader: CourseProgress.Grader) {
    Column {
        Text(
            text = grader.type,
            style = MaterialTheme.appTypography.labelLarge,
            color = grader.color,
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
                        color = grader.color,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "13 / 16 Complete",
                    style = MaterialTheme.appTypography.bodySmall,
                    color = MaterialTheme.appColors.textDark,
                )
                Text(
                    text = "${grader.weight}% of Grade",
                    style = MaterialTheme.appTypography.bodySmall,
                    color = MaterialTheme.appColors.textDark,
                )
            }
            Text(
                "10 / 15%",
                style = MaterialTheme.appTypography.bodyLarge,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.appColors.textDark,
            )
        }
    }
}
