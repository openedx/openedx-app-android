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
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.course.presentation.container.CourseContentTab
import org.openedx.course.presentation.outline.CourseContentAllScreen
import org.openedx.course.presentation.videos.CourseContentVideoScreen
import org.openedx.foundation.presentation.WindowSize

@Composable
fun ContentScreen(
    windowSize: WindowSize,
    fragmentManager: FragmentManager,
    courseId: String,
    courseName: String
) {
    var selectedTab by rememberSaveable { mutableStateOf(CourseContentTab.ALL) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
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
                    val isSelected = selectedTab == tab
                    val isEdgeItem = index == 0 || index == CourseContentTab.entries.size - 1
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) {
                                    MaterialTheme.appColors.primary
                                } else {
                                    MaterialTheme.appColors.primaryButtonText
                                }
                            )
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedTab = tab },
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
            when (selectedTab) {
                CourseContentTab.ALL -> CourseContentAllScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(parameters = { parametersOf(courseId, courseName) }),
                    fragmentManager = fragmentManager,
                    onResetDatesClick = {}
                )

                CourseContentTab.VIDEOS -> CourseContentVideoScreen(
                    windowSize = windowSize,
                    viewModel = koinViewModel(parameters = { parametersOf(courseId, courseName) }),
                    fragmentManager = fragmentManager
                )

                CourseContentTab.ASSIGNMENTS -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Assignments screen coming soon", color = MaterialTheme.appColors.primary)
                }
            }
        }
    }
}
