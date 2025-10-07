package org.openedx.course.presentation.assignments

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.BlockType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.Progress
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.course.R
import org.openedx.course.presentation.contenttab.CourseContentAssignmentEmptyState
import org.openedx.course.presentation.ui.CourseProgress
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import java.util.Date
import org.openedx.core.R as coreR

private const val ICON_SIZE_DP = 20
private const val POINTER_ICON_SIZE_DP = 10
private const val POINTER_ICON_PADDING_TOP_DP = 4
private const val PROGRESS_HEIGHT_DP = 6
private const val ASSIGNMENT_BUTTON_CARD_BACKGROUND_ALPHA = 0.5f
private const val COMPLETED_ASSIGNMENTS_COUNT = 1
private const val COMPLETED_ASSIGNMENTS_COUNT_TABLET = 2
private const val TOTAL_ASSIGNMENTS_COUNT = 3

@Composable
fun CourseContentAssignmentScreen(
    windowSize: WindowSize,
    viewModel: CourseAssignmentViewModel,
    fragmentManager: FragmentManager,
    onNavigateToHome: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContentAssignmentScreen(
        uiState = uiState,
        windowSize = windowSize,
        onNavigateToHome = onNavigateToHome,
        onAssignmentClick = { subSectionBlock ->
            viewModel.courseRouter.navigateToCourseSubsections(
                fm = fragmentManager,
                courseId = viewModel.courseId,
                subSectionId = subSectionBlock.id,
                mode = CourseViewMode.FULL
            )
            viewModel.logAssignmentClick(subSectionBlock.id)
        },
    )
}

@Composable
private fun CourseContentAssignmentScreen(
    uiState: CourseAssignmentUIState,
    windowSize: WindowSize,
    onNavigateToHome: () -> Unit,
    onAssignmentClick: (Block) -> Unit,
) {
    val screenWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }

    when (uiState) {
        is CourseAssignmentUIState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is CourseAssignmentUIState.Empty -> {
            CourseContentAssignmentEmptyState(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onReturnToCourseClick = onNavigateToHome
            )
        }

        is CourseAssignmentUIState.CourseData -> {
            val gradingPolicy = uiState.courseProgress.gradingPolicy
            val defaultGradeColor = MaterialTheme.appColors.primary
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val progress = uiState.progress
                val description = stringResource(
                    id = R.string.course_completed_of,
                    progress.completed,
                    progress.total
                )
                LazyColumn(
                    modifier = screenWidth,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Column {
                            CourseProgress(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                progress = progress,
                                description = description
                            )
                            Spacer(modifier = Modifier.padding(vertical = 6.dp))
                            Divider(
                                color = MaterialTheme.appColors.divider
                            )
                            Spacer(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    uiState.groupedAssignments.onEachIndexed { index, (type, blocks) ->
                        val percentOfGrade = gradingPolicy?.assignmentPolicies
                            ?.find { it.type == type }
                            ?.weight?.times(100)
                            ?.toInt() ?: 0
                        val gradeColor =
                            if (gradingPolicy?.assignmentColors?.isNotEmpty() == true) {
                                gradingPolicy.assignmentColors[index % gradingPolicy.assignmentColors.size]
                            } else {
                                defaultGradeColor
                            }
                        item {
                            AssignmentGroupSection(
                                label = type,
                                percentOfGrade = percentOfGrade,
                                gradeColor = gradeColor,
                                assignments = blocks,
                                sectionNames = uiState.sectionNames,
                                onAssignmentClick = onAssignmentClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentGroupSection(
    label: String,
    assignments: List<Block>,
    sectionNames: Map<String, String>,
    percentOfGrade: Int,
    gradeColor: Color,
    onAssignmentClick: (Block) -> Unit,
) {
    val progress = Progress(
        total = assignments.size,
        completed = assignments.filter { it.isCompleted() }.size
    )
    val description = stringResource(
        id = R.string.course_completed,
        progress.completed,
        progress.total
    )
    val firstUncompletedId = assignments.firstOrNull { !it.isCompleted() }?.id
    var selectedId by rememberSaveable(label) { mutableStateOf(firstUncompletedId) }
    var isCompletedShown by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.appTypography.headlineSmall,
                color = MaterialTheme.appColors.textDark,
            )
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                color = gradeColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, gradeColor),
                shape = MaterialTheme.appShapes.material.small
            ) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = stringResource(R.string.course_of_grade, percentOfGrade),
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.labelSmall,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.padding(vertical = 12.dp))
        CourseProgress(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            progress = progress,
            description = description,
            isCompletedShown = isCompletedShown,
            onVisibilityChanged = if (progress.value == 1f) {
                { isCompletedShown = !isCompletedShown }
            } else {
                null
            },
        )
        if (isCompletedShown || progress.value != 1f) {
            if (assignments.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(assignments) { assignment ->
                        AssignmentButton(
                            assignment = assignment,
                            isSelected = assignment.id == selectedId,
                            onClick = {
                                selectedId = assignment.id
                            }
                        )
                    }
                }
            }
            if (assignments.size > 1) {
                // Show details for selected assignment in this group
                assignments.find { it.id == selectedId }?.let { assignment ->
                    AssignmentDetails(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        assignment = assignment,
                        sectionName = sectionNames[assignment.id] ?: "",
                        onAssignmentClick = onAssignmentClick
                    )
                }
            } else {
                val assignment = assignments.firstOrNull() ?: return@Column
                AssignmentDetails(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp),
                    assignment = assignment,
                    sectionName = sectionNames[assignment.id] ?: "",
                    onAssignmentClick = onAssignmentClick
                )
            }
        }
        Divider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.appColors.divider
        )
    }
}

@Composable
private fun AssignmentButton(assignment: Block, isSelected: Boolean, onClick: () -> Unit) {
    val isDuePast = assignment.due != null && assignment.due!! < Date()
    val cardBorderColor = when {
        isSelected -> MaterialTheme.appColors.primary
        assignment.isCompleted() -> MaterialTheme.appColors.successGreen
        isDuePast -> MaterialTheme.appColors.warning
        else -> MaterialTheme.appColors.textDark
    }
    val icon = when {
        assignment.isCompleted() -> painterResource(id = coreR.drawable.ic_core_check)
        isDuePast -> painterResource(id = coreR.drawable.ic_core_watch_later)
        else -> null
    }
    val iconDescription = when {
        assignment.isCompleted() -> stringResource(R.string.course_accessibility_assignment_completed)
        isDuePast -> stringResource(R.string.course_accessibility_assignment_completed)
        else -> null
    }
    val borderWidth = when {
        isSelected -> 2.dp
        else -> 1.dp
    }
    val cardBackground = when {
        assignment.isCompleted() -> MaterialTheme.appColors.successGreen.copy(
            ASSIGNMENT_BUTTON_CARD_BACKGROUND_ALPHA
        )

        isDuePast -> MaterialTheme.appColors.warning.copy(ASSIGNMENT_BUTTON_CARD_BACKGROUND_ALPHA)
        else -> MaterialTheme.appColors.cardViewBackground
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
        ) {
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .height(42.dp)
                    .clickable {
                        onClick()
                    },
                backgroundColor = cardBackground,
                shape = MaterialTheme.appShapes.material.small,
                border = BorderStroke(
                    width = borderWidth,
                    color = cardBorderColor
                ),
                elevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = assignment.assignmentProgress?.label ?: "",
                        color = MaterialTheme.appColors.textDark,
                        style = MaterialTheme.appTypography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (icon != null) {
                Image(
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = (-6).dp),
                    painter = icon,
                    contentDescription = iconDescription,
                )
            }
        }
        if (isSelected) {
            Icon(
                modifier = Modifier
                    .size(POINTER_ICON_SIZE_DP.dp)
                    .padding(top = POINTER_ICON_PADDING_TOP_DP.dp),
                painter = painterResource(id = coreR.drawable.ic_core_pointer),
                tint = MaterialTheme.appColors.primary,
                contentDescription = null
            )
        } else {
            Box(
                modifier = Modifier
                    .size(POINTER_ICON_SIZE_DP.dp)
                    .padding(top = POINTER_ICON_PADDING_TOP_DP.dp)
            )
        }
    }
}

@Composable
private fun AssignmentDetails(
    modifier: Modifier = Modifier,
    assignment: Block,
    sectionName: String,
    onAssignmentClick: (Block) -> Unit,
) {
    val dueDate =
        assignment.due?.let {
            TimeUtils.formatToDueInString(LocalContext.current, it)
        } ?: ""
    val isDuePast = assignment.due != null && assignment.due!! < Date()
    val progress = assignment.completion.toFloat()
    val color = when {
        assignment.isCompleted() -> MaterialTheme.appColors.successGreen
        isDuePast -> MaterialTheme.appColors.warning
        else -> MaterialTheme.appColors.assignmentCardBorder
    }
    val label = assignment.assignmentProgress?.label
    val description = when {
        assignment.isCompleted() -> {
            "$label " + stringResource(
                R.string.course_complete_points,
                assignment.assignmentProgress?.toPointString() ?: ""
            )
        }

        isDuePast -> {
            "$label " + stringResource(
                R.string.course_past_due,
                assignment.assignmentProgress?.toPointString() ?: ""
            )
        }

        progress < 1f && assignment.due == null -> {
            "$label " + stringResource(
                R.string.course_in_progress,
                assignment.assignmentProgress?.toPointString() ?: ""
            )
        }

        else -> {
            "$label $dueDate"
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onAssignmentClick(assignment)
            },
        backgroundColor = MaterialTheme.appColors.cardViewBackground,
        shape = MaterialTheme.appShapes.material.small,
        border = BorderStroke(
            width = 1.dp,
            color = color
        ),
        elevation = 0.dp,
    ) {
        Column {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PROGRESS_HEIGHT_DP.dp),
                progress = progress,
                color = MaterialTheme.appColors.progressBarColor,
                backgroundColor = color
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = sectionName,
                        style = MaterialTheme.appTypography.bodySmall,
                        color = MaterialTheme.appColors.textDark
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = assignment.displayName,
                        style = MaterialTheme.appTypography.bodyLarge,
                        color = MaterialTheme.appColors.textDark
                    )
                    if (description.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(top = 6.dp),
                            text = description,
                            style = MaterialTheme.appTypography.bodySmall,
                            color = MaterialTheme.appColors.textDark
                        )
                    }
                }
                Icon(
                    modifier = Modifier
                        .size(ICON_SIZE_DP.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.primary
                )
            }
        }
    }
}

@Preview
@Composable
private fun CourseContentAssignmentScreenPreview() {
    OpenEdXTheme {
        CourseContentAssignmentScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseAssignmentUIState.CourseData(
                progress = Progress(COMPLETED_ASSIGNMENTS_COUNT, TOTAL_ASSIGNMENTS_COUNT),
                groupedAssignments = mapOf(
                    "Homework" to listOf(mockChapterBlock, mockSequentialBlock)
                ),
                courseProgress = mockCourseProgress,
                sectionNames = mapOf()
            ),
            onAssignmentClick = {},
            onNavigateToHome = {},
        )
    }
}

@Preview
@Composable
private fun CourseContentAssignmentScreenEmptyPreview() {
    OpenEdXTheme {
        CourseContentAssignmentScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseAssignmentUIState.Empty,
            onAssignmentClick = {},
            onNavigateToHome = {},
        )
    }
}

@Preview(device = Devices.NEXUS_9)
@Composable
private fun CourseContentAssignmentScreenTabletPreview() {
    OpenEdXTheme {
        CourseContentAssignmentScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseAssignmentUIState.CourseData(
                progress = Progress(COMPLETED_ASSIGNMENTS_COUNT_TABLET, TOTAL_ASSIGNMENTS_COUNT),
                groupedAssignments = mapOf(
                    "Homework" to listOf(mockChapterBlock),
                    "Quiz" to listOf(mockSequentialBlock)
                ),
                courseProgress = mockCourseProgress,
                sectionNames = mapOf()
            ),
            onAssignmentClick = {},
            onNavigateToHome = {},
        )
    }
}

private val mockCourseProgress = CourseProgress(
    verifiedMode = "verified",
    accessExpiration = "2024-12-31",
    certificateData = CourseProgress.CertificateData(
        certStatus = "downloadable",
        certWebViewUrl = "https://example.com/cert",
        downloadUrl = "https://example.com/cert.pdf",
        certificateAvailableDate = "2024-06-01"
    ),
    completionSummary = CourseProgress.CompletionSummary(
        completeCount = 5,
        incompleteCount = 3,
        lockedCount = 1
    ),
    courseGrade = CourseProgress.CourseGrade(
        letterGrade = "B+",
        percent = 85.5,
        isPassing = true
    ),
    creditCourseRequirements = "Complete all assignments",
    end = "2024-12-31",
    enrollmentMode = "verified",
    gradingPolicy = CourseProgress.GradingPolicy(
        assignmentPolicies = listOf(
            CourseProgress.GradingPolicy.AssignmentPolicy(
                numDroppable = 1,
                numTotal = 5,
                shortLabel = "HW",
                type = "Homework",
                weight = 0.4
            ),
            CourseProgress.GradingPolicy.AssignmentPolicy(
                numDroppable = 0,
                numTotal = 3,
                shortLabel = "Quiz",
                type = "Quiz",
                weight = 0.6
            )
        ),
        gradeRange = mapOf(
            "A" to 0.9f,
            "B" to 0.8f,
            "C" to 0.7f,
            "D" to 0.6f
        ),
        assignmentColors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50))
    ),
    hasScheduledContent = false,
    sectionScores = listOf(
        CourseProgress.SectionScore(
            displayName = "Week 1",
            subsections = listOf(
                CourseProgress.SectionScore.Subsection(
                    assignmentType = "Homework",
                    blockKey = "block1",
                    displayName = "Homework 1",
                    hasGradedAssignment = true,
                    override = "",
                    learnerHasAccess = true,
                    numPointsEarned = 8f,
                    numPointsPossible = 10f,
                    percentGraded = 80.0,
                    problemScores = listOf(
                        CourseProgress.SectionScore.Subsection.ProblemScore(
                            earned = 8.0,
                            possible = 10.0
                        )
                    ),
                    showCorrectness = "always",
                    showGrades = true,
                    url = "https://example.com/hw1"
                )
            )
        )
    ),
    studioUrl = "https://studio.example.com",
    username = "testuser",
    userHasPassingGrade = true,
    verificationData = CourseProgress.VerificationData(
        link = "https://example.com/verify",
        status = "approved",
        statusDate = "2024-01-15"
    ),
    disableProgressGraph = false
)

private val mockAssignmentProgress = AssignmentProgress(
    assignmentType = "Home",
    numPointsEarned = 1f,
    numPointsPossible = 3f,
    shortLabel = "HM1"
)

private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false,
    assignmentProgress = mockAssignmentProgress,
    due = Date(),
    offlineDownload = null
)

private val mockSequentialBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.SEQUENTIAL,
    displayName = "Sequential",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.SEQUENTIAL,
    completion = 0.0,
    containsGatedContent = false,
    assignmentProgress = mockAssignmentProgress,
    due = Date(),
    offlineDownload = null
)
