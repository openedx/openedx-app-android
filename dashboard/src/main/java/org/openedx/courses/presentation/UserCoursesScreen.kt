package org.openedx.courses.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseAssignments
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStatus
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.domain.model.Pagination
import org.openedx.core.domain.model.Progress
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.courses.domain.model.UserCourses
import org.openedx.dashboard.R
import java.util.Date
import org.openedx.core.R as CoreR

@Composable
fun UsersCourseScreen(
    viewModel: UserCoursesViewModel,
    onCourseClick: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit,
) {
    val updating by viewModel.updating.observeAsState(false)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val uiState by viewModel.uiState.observeAsState(UserCoursesUIState.Loading)

    UsersCourseScreen(
        uiMessage = uiMessage,
        uiState = uiState,
        updating = updating,
        apiHostUrl = viewModel.apiHostUrl,
        onSwipeRefresh = viewModel::updateCourses,
        onCourseClick = onCourseClick,
        onViewAllClick = onViewAllClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UsersCourseScreen(
    uiMessage: UIMessage?,
    uiState: UserCoursesUIState,
    updating: Boolean,
    apiHostUrl: String,
    onSwipeRefresh: () -> Unit,
    onCourseClick: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState = rememberPullRefreshState(refreshing = updating, onRefresh = { onSwipeRefresh() })

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->
        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.appColors.background
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (uiState) {
                    is UserCoursesUIState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.appColors.primary
                        )
                    }

                    is UserCoursesUIState.Courses -> {
                        UserCourses(
                            modifier = Modifier.fillMaxSize(),
                            userCourses = uiState.userCourses,
                            apiHostUrl = apiHostUrl,
                            onCourseClick = onCourseClick,
                            onViewAllClick = onViewAllClick
                        )
                    }

                    is UserCoursesUIState.Empty -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                PullRefreshIndicator(
                    updating,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun UserCourses(
    modifier: Modifier = Modifier,
    userCourses: UserCourses,
    apiHostUrl: String,
    onCourseClick: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(vertical = 12.dp)
    ) {
        if (userCourses.primary != null) {
            PrimaryCourseCard(
                primaryCourse = userCourses.primary,
                apiHostUrl = apiHostUrl
            )
        }
        SecondaryCourses(
            courses = userCourses.enrollments.courses,
            apiHostUrl = apiHostUrl,
            onCourseClick = onCourseClick,
            onViewAllClick = onViewAllClick
        )
    }
}

@Composable
private fun SecondaryCourses(
    courses: List<EnrolledCourse>,
    apiHostUrl: String,
    onCourseClick: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 8.dp)
    ) {
        TextIcon(
            text = stringResource(R.string.dashboard_view_all, courses.size),
            textStyle = MaterialTheme.appTypography.titleSmall,
            icon = Icons.Default.ChevronRight,
            color = MaterialTheme.appColors.textDark,
            modifier = Modifier.padding(horizontal = 4.dp),
            iconModifier = Modifier.size(22.dp),
            onClick = onViewAllClick
        )
        LazyRow {
            items(courses) {
                CourseListItem(
                    course = it,
                    apiHostUrl = apiHostUrl,
                    onCourseClick = onCourseClick
                )
            }
        }
    }
}

@Composable
private fun CourseListItem(
    course: EnrolledCourse,
    apiHostUrl: String,
    onCourseClick: (EnrolledCourse) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .padding(4.dp)
            .clickable {
                onCourseClick(course)
            },
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 2.dp
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(apiHostUrl + course.course.courseImage)
                    .error(org.openedx.core.R.drawable.core_no_image_course)
                    .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
            Text(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                text = course.course.name,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textDark,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AssignmentItem(
    painter: Painter,
    title: String?,
    info: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            tint = MaterialTheme.appColors.textDark,
            contentDescription = null
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = info,
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelSmall
            )
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun PrimaryCourseCard(
    primaryCourse: EnrolledCourse,
    apiHostUrl: String
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .padding(2.dp),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 4.dp
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(apiHostUrl + primaryCourse.course.courseImage)
                    .error(org.openedx.core.R.drawable.core_no_image_course)
                    .placeholder(org.openedx.core.R.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progress = primaryCourse.progress.numPointsEarned.toFloat(),
                color = MaterialTheme.appColors.primary,
                backgroundColor = MaterialTheme.appColors.divider
            )
            PrimaryCourseTitle(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                primaryCourse = primaryCourse
            )
            val pastAssignments = primaryCourse.courseAssignments?.pastAssignments
            if (!pastAssignments.isNullOrEmpty()) {
                val title = if (pastAssignments.size == 1) pastAssignments.first().title else null
                Divider()
                AssignmentItem(
                    painter = rememberVectorPainter(Icons.Default.Warning),
                    title = title,
                    info = stringResource(R.string.dashboard_past_due_assignment, pastAssignments.size)
                )
            }
            val futureAssignment = primaryCourse.courseAssignments?.futureAssignment
            if (futureAssignment != null) {
                Divider()
                AssignmentItem(
                    painter = painterResource(id = CoreR.drawable.ic_core_chapter_icon),
                    title = futureAssignment.title,
                    info = stringResource(
                        R.string.dashboard_assignment_due_in_days,
                        futureAssignment.assignmentType ?: "",
                        TimeUtils.getCourseFormattedDate(context, futureAssignment.date)
                    )
                )
            }
            ResumeButton(
                modifier = Modifier.fillMaxWidth(),
                primaryCourse = primaryCourse
            )
        }
    }
}

@Composable
private fun ResumeButton(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse
) {
    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .background(MaterialTheme.appColors.primary)
            .clickable {
                //TODO
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (primaryCourse.courseStatus == null) {
            Text(
                modifier = modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.dashboard_start_course),
                textAlign = TextAlign.Center,
                color = MaterialTheme.appColors.buttonText,
                style = MaterialTheme.appTypography.titleSmall
            )
        } else {
            Icon(
                imageVector = Icons.Default.School,
                tint = MaterialTheme.appColors.buttonText,
                contentDescription = null
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_resume_course),
                    color = MaterialTheme.appColors.buttonText,
                    style = MaterialTheme.appTypography.labelSmall
                )
                Text(
                    text = primaryCourse.courseStatus?.lastVisitedUnitDisplayName ?: "",
                    color = MaterialTheme.appColors.buttonText,
                    style = MaterialTheme.appTypography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun PrimaryCourseTitle(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = primaryCourse.course.org,
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textFieldHint
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = primaryCourse.course.name,
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textDark
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textFieldHint,
            text = stringResource(
                R.string.dashboard_course_date,
                TimeUtils.getCourseFormattedDate(
                    LocalContext.current,
                    Date(),
                    primaryCourse.auditAccessExpires,
                    primaryCourse.course.start,
                    primaryCourse.course.end,
                    primaryCourse.course.startType,
                    primaryCourse.course.startDisplay
                )
            )
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(185.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dashboard_ic_empty),
                contentDescription = null,
                tint = MaterialTheme.appColors.textFieldBorder
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dashboard_you_are_not_enrolled),
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}


private val mockCourseAssignments = CourseAssignments(null, emptyList())
private val mockCourse = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    progress = Progress.DEFAULT_PROGRESS,
    courseStatus = CourseStatus("", emptyList(), "", ""),
    courseAssignments = mockCourseAssignments,
    course = EnrolledCourseData(
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        dynamicUpgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            "",
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    )
)
private val mockPagination = Pagination(10, "", 4, "1")
private val mockDashboardCourseList = DashboardCourseList(
    pagination = mockPagination,
    courses = listOf(mockCourse, mockCourse, mockCourse, mockCourse, mockCourse, mockCourse)
)
private val mockUserCourses = UserCourses(
    enrollments = mockDashboardCourseList,
    primary = mockCourse
)

@Preview
@Composable
private fun UsersCourseScreenPreview() {
    OpenEdXTheme {
        UsersCourseScreen(
            uiState = UserCoursesUIState.Courses(mockUserCourses),
            apiHostUrl = "",
            uiMessage = null,
            updating = false,
            onSwipeRefresh = { },
            onCourseClick = { },
            onViewAllClick = { }
        )
    }
}
