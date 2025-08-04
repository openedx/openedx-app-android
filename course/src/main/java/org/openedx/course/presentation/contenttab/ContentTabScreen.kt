package org.openedx.course.presentation.contenttab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.course.presentation.assignments.CourseContentAssignmentScreen
import org.openedx.course.presentation.container.CourseContentTab
import org.openedx.course.presentation.outline.CourseContentAllScreen
import org.openedx.course.presentation.videos.CourseContentVideoScreen
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.windowSizeValue

@Composable
fun ContentTabScreen(
    viewModel: ContentTabViewModel,
    windowSize: WindowSize,
    fragmentManager: FragmentManager,
    courseId: String,
    courseName: String,
    pagerState: PagerState,
    onTabSelected: (CourseContentTab) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
) {
    val tabsWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth()
            )
        )
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        val selectedTab = CourseContentTab.entries[pagerState.currentPage]
        onTabSelected(selectedTab)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .then(tabsWidth)
                    .height(40.dp)
                    .clip(MaterialTheme.appShapes.buttonShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.primary,
                        MaterialTheme.appShapes.buttonShape
                    ),
                horizontalArrangement = Arrangement.Center
            ) {
                CourseContentTab.entries.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    val isEdgeItem = index == 0 || index == CourseContentTab.entries.size - 1
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) {
                                    MaterialTheme.appColors.primary
                                } else {
                                    MaterialTheme.appColors.background
                                }
                            )
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                                viewModel.logTabClickEvent(CourseContentTab.entries[index])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isEdgeItem) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .align(Alignment.CenterStart),
                                color = MaterialTheme.appColors.primary
                            )
                        }
                        Text(
                            text = stringResource(tab.labelResId),
                            color = if (isSelected) {
                                MaterialTheme.appColors.primaryButtonText
                            } else {
                                MaterialTheme.appColors.primary
                            },
                            style = MaterialTheme.typography.button
                        )
                        if (!isEdgeItem) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .align(Alignment.CenterEnd),
                                color = MaterialTheme.appColors.primary
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                beyondViewportPageCount = CourseContentTab.entries.size
            ) { page ->
                when (CourseContentTab.entries[page]) {
                    CourseContentTab.ALL -> CourseContentAllScreen(
                        windowSize = windowSize,
                        viewModel = koinViewModel(parameters = {
                            parametersOf(
                                courseId,
                                courseName
                            )
                        }),
                        fragmentManager = fragmentManager,
                        onNavigateToHome = onNavigateToHome
                    )

                    CourseContentTab.VIDEOS -> CourseContentVideoScreen(
                        windowSize = windowSize,
                        viewModel = koinViewModel(parameters = {
                            parametersOf(
                                courseId,
                                courseName
                            )
                        }),
                        fragmentManager = fragmentManager,
                        onNavigateToHome = onNavigateToHome
                    )

                    CourseContentTab.ASSIGNMENTS -> CourseContentAssignmentScreen(
                        windowSize = windowSize,
                        viewModel = koinViewModel(parameters = { parametersOf(courseId) }),
                        fragmentManager = fragmentManager,
                        onNavigateToHome = onNavigateToHome
                    )
                }
            }
        }
    }
}
