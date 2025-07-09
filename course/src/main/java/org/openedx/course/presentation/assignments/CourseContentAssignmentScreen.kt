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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.Progress
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.course.R
import org.openedx.course.presentation.ui.CourseProgress
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.windowSizeValue
import java.util.Date
import org.openedx.core.R as coreR

@Composable
fun CourseContentAssignmentScreen(
    windowSize: WindowSize,
    viewModel: CourseAssignmentViewModel,
    fragmentManager: FragmentManager
) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContentAssignmentScreen(
        uiState = uiState,
        windowSize = windowSize
    )
}

@Composable
private fun CourseContentAssignmentScreen(
    uiState: CourseAssignmentUIState,
    windowSize: WindowSize,
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is CourseAssignmentUIState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error loading assignments", color = Color.Red)
            }
        }

        is CourseAssignmentUIState.CourseData -> {
            Column(modifier = screenWidth) {
                val progress = uiState.progress
                val description = stringResource(
                    id = R.string.course_completed,
                    progress.completed,
                    progress.total
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                    uiState.groupedAssignments.forEach { (type, blocks) ->
                        item {
                            AssignmentGroupSection(
                                label = type,
                                assignments = blocks
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
    assignments: List<Block>
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
    val percentOfGrade = -1 // Mocked
    val firstUncompletedId = assignments.firstOrNull { !it.isCompleted() }?.id
    var selectedId by rememberSaveable(label) { mutableStateOf(firstUncompletedId) }

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
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color.Gray),
                shape = MaterialTheme.shapes.small
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
            description = description
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(assignments) { assignment ->
                AssignmentButton(
                    assignment = assignment,
                    isSelected = assignment.id == selectedId,
                    onClick = {
                        selectedId = if (selectedId == assignment.id) {
                            null
                        } else {
                            assignment.id
                        }
                    }
                )
            }
        }
        // Show details for selected assignment in this group
        assignments.find { it.id == selectedId }?.let { assignment ->
            AssignmentDetails(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                assignment = assignment,
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.appColors.divider
        )
    }
}

@Composable
private fun AssignmentButton(assignment: Block, isSelected: Boolean, onClick: () -> Unit) {
    val label = assignment.assignmentProgress?.label ?: ""
    val isDuePast = assignment.due != null && assignment.due!! > Date()
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
        assignment.isCompleted() -> MaterialTheme.appColors.successGreen.copy(0.5f)
        isDuePast -> MaterialTheme.appColors.warning.copy(0.5f)
        else -> MaterialTheme.appColors.cardViewBackground
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.TopCenter
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
                Text(
                    text = label,
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.bodyMedium,

                    )
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
                    .size(10.dp)
                    .padding(top = 4.dp),
                painter = painterResource(id = coreR.drawable.ic_core_pointer),
                tint = MaterialTheme.appColors.primary,
                contentDescription = null
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AssignmentDetails(
    modifier: Modifier = Modifier,
    assignment: Block
) {
    val dueDate =
        assignment.due?.let { TimeUtils.formatToString(LocalContext.current, it, true) } ?: ""
    val progress = assignment.completion.toFloat()
    val color = when {
        assignment.isCompleted() -> MaterialTheme.appColors.successGreen
        assignment.due != null && assignment.due!! > Date() -> MaterialTheme.appColors.warning
        else -> MaterialTheme.appColors.cardViewBorder
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {

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
                    .height(6.dp),
                progress = progress,
                color = MaterialTheme.appColors.progressBarColor,
                backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
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
                        text = assignment.displayName,
                        style = MaterialTheme.appTypography.bodyLarge,
                        color = MaterialTheme.appColors.textDark
                    )
                    if (dueDate.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.course_assignment_due,
                                assignment.assignmentProgress?.label ?: "",
                                dueDate
                            ),
                            style = MaterialTheme.appTypography.bodySmall,
                            color = MaterialTheme.appColors.textDark
                        )
                    }
                }
                Icon(
                    modifier = Modifier
                        .size(20.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.primary
                )
            }
        }
    }
}
