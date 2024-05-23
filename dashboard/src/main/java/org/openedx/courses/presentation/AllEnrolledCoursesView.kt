package org.openedx.courses.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.Lock
import org.openedx.core.R
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.dashboard.domain.CourseStatusFilter
import java.util.Date

@Composable
fun CourseItem(
    modifier: Modifier = Modifier,
    course: EnrolledCourse,
    apiHostUrl: String,
    onClick: (EnrolledCourse) -> Unit,
) {
    Card(
        modifier = modifier
            .width(170.dp)
            .height(180.dp)
            .clickable {
                onClick(course)
            },
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 4.dp
    ) {
        Box {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(apiHostUrl + course.course.courseImage)
                        .error(R.drawable.core_no_image_course)
                        .placeholder(R.drawable.core_no_image_course)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )
                val progress: Float = try {
                    course.progress.assignmentsCompleted.toFloat() / course.progress.totalAssignmentsCount.toFloat()
                } catch (_: ArithmeticException) {
                    0f
                }
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    progress = progress,
                    color = MaterialTheme.appColors.primary,
                    backgroundColor = MaterialTheme.appColors.divider
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp),
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.textFieldHint,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 1,
                    maxLines = 2,
                    text = stringResource(
                        org.openedx.dashboard.R.string.dashboard_course_date,
                        TimeUtils.getCourseFormattedDate(
                            LocalContext.current,
                            Date(),
                            course.auditAccessExpires,
                            course.course.start,
                            course.course.end,
                            course.course.startType,
                            course.course.startDisplay
                        )
                    )
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    text = course.course.name,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textDark,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 1,
                    maxLines = 2
                )
            }
            if (!course.course.coursewareAccess?.errorCode.isNullOrEmpty()) {
                Lock()
            }
        }
    }
}

@Composable
fun Header(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = stringResource(id = org.openedx.dashboard.R.string.dashboard_all_courses),
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.headlineBold
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp),
            onClick = {
                onSearchClick()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.appColors.textDark
            )
        }
    }
}

@Composable
fun EmptyState(
    currentCourseStatus: CourseStatusFilter
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = org.openedx.dashboard.R.drawable.dashboard_ic_book),
                tint = MaterialTheme.appColors.textFieldBorder,
                contentDescription = null
            )
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_title")
                    .fillMaxWidth(),
                text = stringResource(
                    id = org.openedx.dashboard.R.string.dashboard_no_status_courses,
                    stringResource(currentCourseStatus.labelResId)
                ),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}