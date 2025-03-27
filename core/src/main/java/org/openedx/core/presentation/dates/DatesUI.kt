package org.openedx.core.presentation.dates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils.formatToString
import org.openedx.core.utils.clearTime

@Composable
private fun CourseDateBlockSectionGeneric(
    sectionKey: DatesSection = DatesSection.NONE,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        if (sectionKey != DatesSection.COMPLETED) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                text = stringResource(id = sectionKey.stringResId),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // ensures all cards share the height of the tallest one.
        ) {
            if (sectionKey != DatesSection.COMPLETED) {
                DateBullet(section = sectionKey)
            }
            content()
        }
    }
}

@Composable
private fun DateBlockContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 8.dp, end = 8.dp)
    ) {
        content()
    }
}

@Composable
fun CourseDateBlockSection(
    sectionKey: DatesSection = DatesSection.NONE,
    useRelativeDates: Boolean,
    sectionDates: List<CourseDateBlock>,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    CourseDateBlockSectionGeneric(sectionKey = sectionKey) {
        DateBlock(
            dateBlocks = sectionDates,
            onItemClick = onItemClick,
            useRelativeDates = useRelativeDates
        )
    }
}

@JvmName("CourseDateBlockSectionCourseDates")
@Composable
fun CourseDateBlockSection(
    sectionKey: DatesSection = DatesSection.NONE,
    useRelativeDates: Boolean,
    sectionDates: List<CourseDate>,
    onItemClick: (CourseDate) -> Unit,
) {
    CourseDateBlockSectionGeneric(sectionKey = sectionKey) {
        DateBlock(
            dateBlocks = sectionDates,
            onItemClick = onItemClick,
            useRelativeDates = useRelativeDates
        )
    }
}

@Composable
private fun DateBullet(
    section: DatesSection = DatesSection.NONE,
) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight()
            .padding(top = 2.dp, bottom = 2.dp)
            .background(
                color = section.color,
                shape = MaterialTheme.shapes.medium
            )
    )
}

@Composable
private fun DateBlock(
    dateBlocks: List<CourseDateBlock>,
    useRelativeDates: Boolean,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    DateBlockContainer {
        var lastAssignmentDate = dateBlocks.first().date.clearTime()
        dateBlocks.forEachIndexed { index, dateBlock ->
            val canShowDate = if (index == 0) true else (lastAssignmentDate != dateBlock.date)
            CourseDateItem(dateBlock, canShowDate, index != 0, useRelativeDates, onItemClick)
            lastAssignmentDate = dateBlock.date
        }
    }
}

@JvmName("DateBlockCourseDate")
@Composable
private fun DateBlock(
    dateBlocks: List<CourseDate>,
    useRelativeDates: Boolean,
    onItemClick: (CourseDate) -> Unit,
) {
    DateBlockContainer {
        dateBlocks.forEachIndexed { index, dateBlock ->
            CourseDateItem(dateBlock, index != 0, useRelativeDates, onItemClick)
        }
    }
}

@Composable
private fun CourseDateItem(
    dateBlock: CourseDateBlock,
    canShowDate: Boolean,
    isMiddleChild: Boolean,
    useRelativeDates: Boolean,
    onItemClick: (CourseDateBlock) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        if (isMiddleChild) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (canShowDate) {
            val timeTitle = formatToString(context, dateBlock.date, useRelativeDates)
            Text(
                text = timeTitle,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textDark,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
                .clickable(
                    enabled = dateBlock.blockId.isNotEmpty() && dateBlock.learnerHasAccess,
                    onClick = { onItemClick(dateBlock) }
                )
        ) {
            dateBlock.dateType.drawableResId?.let { icon ->
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(
                        id = if (!dateBlock.learnerHasAccess) {
                            R.drawable.core_ic_lock
                        } else {
                            icon
                        }
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textDark
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = if (!dateBlock.assignmentType.isNullOrEmpty()) {
                    "${dateBlock.assignmentType}: ${dateBlock.title}"
                } else {
                    dateBlock.title
                },
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(7.dp))
            if (dateBlock.blockId.isNotEmpty() && dateBlock.learnerHasAccess) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    tint = MaterialTheme.appColors.textDark,
                    contentDescription = "Open Block Arrow",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
        if (dateBlock.description.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                text = dateBlock.description,
                style = MaterialTheme.appTypography.labelMedium,
            )
        }
    }
}

@Composable
private fun CourseDateItem(
    dateBlock: CourseDate,
    isMiddleChild: Boolean,
    useRelativeDates: Boolean,
    onItemClick: (CourseDate) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        if (isMiddleChild) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        val timeTitle = formatToString(context, dateBlock.dueDate, useRelativeDates)
        Text(
            text = timeTitle,
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textDark,
            maxLines = 1,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
                .clickable(
                    enabled = dateBlock.firstComponentBlockId.isNotEmpty() && dateBlock.learnerHasAccess,
                    onClick = { onItemClick(dateBlock) }
                )
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(Alignment.CenterVertically),
                painter = painterResource(R.drawable.core_ic_assignment),
                contentDescription = null,
                tint = MaterialTheme.appColors.textDark
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = dateBlock.assignmentTitle,
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(7.dp))
            if (dateBlock.firstComponentBlockId.isNotEmpty() && dateBlock.learnerHasAccess) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    tint = MaterialTheme.appColors.textDark,
                    contentDescription = "Open Block Arrow",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = dateBlock.courseName,
            maxLines = 1,
            style = MaterialTheme.appTypography.labelMedium,
        )
    }
}
