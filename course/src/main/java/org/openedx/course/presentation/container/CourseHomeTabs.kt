package org.openedx.course.presentation.container

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.appColors
import org.openedx.course.R

enum class CourseHomeTab(@StringRes val title: Int, val icon: ImageVector) {
    HOME(R.string.course_navigation_home, Icons.Default.Home),
    VIDEOS(R.string.course_navigation_videos, Icons.Rounded.PlayCircleFilled),
    DATES(R.string.course_navigation_dates, Icons.Outlined.CalendarMonth),
    DISCUSSIONS(R.string.course_navigation_discussions, Icons.AutoMirrored.Filled.Chat),
    MORE(R.string.course_navigation_more, Icons.AutoMirrored.Filled.TextSnippet)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseHomeTabs(
    pagerState: PagerState,
    rowState: LazyListState = rememberLazyListState(),
    onPageChange: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val windowSize = rememberWindowSize()
    val horizontalPadding = if (!windowSize.isTablet){
        12.dp
    } else {
        98.dp
    }
    LazyRow(
        modifier = Modifier,
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = horizontalPadding),
    ) {
        itemsIndexed(CourseHomeTab.entries) { index, item ->
            val backgroundColor = if (pagerState.currentPage == index) {
                MaterialTheme.appColors.primary
            } else {
                MaterialTheme.appColors.tabUnselectedBtnBackground
            }
            val contentColor = if (pagerState.currentPage == index) {
                Color.White
            } else {
                MaterialTheme.appColors.tabUnselectedBtnContent
            }
            val border = if (isSystemInDarkTheme()) {
                Modifier
            } else {
                Modifier.border(
                    1.dp,
                    MaterialTheme.appColors.primary,
                    CircleShape
                )
            }
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable {
                        scope.launch {
                            pagerState.scrollToPage(index)
                            onPageChange(index)
                        }
                    }
                        then (border)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = rememberVectorPainter(item.icon),
                    tint = contentColor,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(item.title),
                    color = contentColor
                )
            }
        }
    }
}
