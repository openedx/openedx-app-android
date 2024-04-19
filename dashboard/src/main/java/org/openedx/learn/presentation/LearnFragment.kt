package org.openedx.learn.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.CourseContainerTabEntity
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.presentation.global.InDevelopmentScreen
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.crop
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.courses.presentation.UserCoursesViewModel
import org.openedx.courses.presentation.UsersCourseScreen
import org.openedx.dashboard.R
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.learn.LearnType

class LearnFragment : Fragment() {

    private val userCoursesViewModel by viewModel<UserCoursesViewModel>()
    private val router by inject<DashboardRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                LearnScreen(
                    windowSize = windowSize,
                    userCoursesViewModel = userCoursesViewModel,
                    onCourseClick = {
                        router.navigateToCourseOutline(
                            requireParentFragment().parentFragmentManager,
                            it.course.id,
                            it.course.name,
                            it.mode,
                            CourseContainerTabEntity.COURSE
                        )
                    },
                    openDates = {
                        router.navigateToCourseOutline(
                            requireParentFragment().parentFragmentManager,
                            it.course.id,
                            it.course.name,
                            it.mode,
                            CourseContainerTabEntity.DATES
                        )
                    },
                    onResumeClick = { componentId ->
                        //TODO
                    },
                    onViewAllClick = {
                        router.navigateToAllEnrolledCourses(
                            requireParentFragment().parentFragmentManager
                        )
                    },
                    onSearchClick = {
                        router.navigateToCourseSearch(
                            requireParentFragment().parentFragmentManager, ""
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LearnScreen(
    windowSize: WindowSize,
    userCoursesViewModel: UserCoursesViewModel,
    onCourseClick: (course: EnrolledCourse) -> Unit,
    openDates: (course: EnrolledCourse) -> Unit,
    onResumeClick: (componentId: String) -> Unit,
    onViewAllClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val pagerState = rememberPagerState {
        LearnType.entries.size
    }
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxSize(),
            )
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->


        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape()
                .then(contentWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                label = stringResource(id = R.string.dashboard_learn),
                onSearchClick = onSearchClick
            )

            if (userCoursesViewModel.isProgramTypeWebView) {
                LearnDropdownMenu(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 16.dp),
                    pagerState = pagerState
                )
            }

            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize(),
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> UsersCourseScreen(
                        viewModel = userCoursesViewModel,
                        onCourseClick = onCourseClick,
                        onViewAllClick = onViewAllClick,
                        openDates = openDates,
                        onResumeClick = onResumeClick
                    )

                    1 -> InDevelopmentScreen()
                }
            }
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    label: String,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = label,
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.headlineBolt
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 16.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LearnDropdownMenu(
    modifier: Modifier = Modifier,
    pagerState: PagerState
) {
    var expanded by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(LearnType.COURSES) }

    LaunchedEffect(currentValue) {
        pagerState.scrollToPage(
            when (currentValue) {
                LearnType.COURSES -> 0
                LearnType.PROGRAMS -> 1
            }
        )
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    expanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = currentValue.title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleSmall
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                tint = MaterialTheme.appColors.textDark,
                contentDescription = null
            )
        }

        MaterialTheme(
            colors = MaterialTheme.colors.copy(surface = MaterialTheme.appColors.background),
            shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            DropdownMenu(
                modifier = Modifier
                    .crop(vertical = 8.dp)
                    .widthIn(min = 182.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for (learnType in LearnType.entries) {
                    val background: Color
                    val textColor: Color
                    if (currentValue == learnType) {
                        background = MaterialTheme.appColors.primary
                        textColor = MaterialTheme.appColors.buttonText
                    } else {
                        background = Color.Transparent
                        textColor = MaterialTheme.appColors.textDark
                    }
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(background),
                        onClick = {
                            currentValue = learnType
                            expanded = false
                        }
                    ) {
                        Text(
                            text = stringResource(id = learnType.title),
                            style = MaterialTheme.appTypography.titleSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun LearnScreenPreview() {
    OpenEdXTheme {
        LearnScreen(
            userCoursesViewModel = viewModel(),
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onCourseClick = {},
            onViewAllClick = {},
            onSearchClick = {},
            openDates = {},
            onResumeClick = {}
        )
    }
}
