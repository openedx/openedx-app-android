package org.openedx.discussion.presentation.topics

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import org.openedx.core.FragmentViewType
import org.openedx.core.NoContentScreenType
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.NoContentScreen
import org.openedx.core.ui.StaticSearchBar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.R
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.ui.ThreadItemCategory
import org.openedx.discussion.presentation.ui.TopicItem
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue

@Composable
fun DiscussionTopicsScreen(
    discussionTopicsViewModel: DiscussionTopicsViewModel,
    windowSize: WindowSize,
    fragmentManager: FragmentManager
) {
    val uiState by discussionTopicsViewModel.uiState.observeAsState(DiscussionTopicsUIState.Loading)
    val uiMessage by discussionTopicsViewModel.uiMessage.collectAsState(null)

    DiscussionTopicsUI(
        windowSize = windowSize,
        uiState = uiState,
        uiMessage = uiMessage,
        onSearchClick = {
            discussionTopicsViewModel.discussionRouter.navigateToSearchThread(
                fragmentManager,
                discussionTopicsViewModel.courseId
            )
        },
        onItemClick = { action, data, title ->
            discussionTopicsViewModel.discussionClickedEvent(
                action,
                data,
                title
            )
            discussionTopicsViewModel.discussionRouter.navigateToDiscussionThread(
                fragmentManager,
                action,
                discussionTopicsViewModel.courseId,
                data,
                title,
                FragmentViewType.FULL_CONTENT
            )
        },
    )
}

@Composable
private fun DiscussionTopicsUI(
    windowSize: WindowSize,
    uiState: DiscussionTopicsUIState,
    uiMessage: UIMessage?,
    onSearchClick: () -> Unit,
    onItemClick: (String, String, String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
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

        val searchTabWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.width(420.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val contentPaddings by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 24.dp
                )
            )
        }

        val categoriesHeight by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 86.dp,
                    compact = 77.dp
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                if ((uiState is DiscussionTopicsUIState.Error).not()) {
                    StaticSearchBar(
                        modifier = Modifier
                            .height(48.dp)
                            .then(searchTabWidth)
                            .padding(horizontal = contentPaddings)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.discussion_search_all_posts),
                        onClick = onSearchClick
                    )
                }
                Surface(
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.appColors.background)
                                .padding(horizontal = contentPaddings),
                        ) {
                            when (uiState) {
                                is DiscussionTopicsUIState.Topics -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text(
                                                modifier = Modifier,
                                                text = stringResource(id = R.string.discussion_main_categories),
                                                style = MaterialTheme.appTypography.titleMedium,
                                                color = MaterialTheme.appColors.textPrimaryVariant
                                            )
                                        }
                                        item {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                ThreadItemCategory(
                                                    name = stringResource(id = R.string.discussion_all_posts),
                                                    painterResource = painterResource(
                                                        id = R.drawable.discussion_all_posts
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(categoriesHeight),
                                                    onClick = {
                                                        onItemClick(
                                                            DiscussionTopicsViewModel.ALL_POSTS,
                                                            "",
                                                            context.getString(R.string.discussion_all_posts)
                                                        )
                                                    }
                                                )
                                                ThreadItemCategory(
                                                    name = stringResource(id = R.string.discussion_posts_following),
                                                    painterResource = painterResource(id = R.drawable.discussion_star),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(categoriesHeight),
                                                    onClick = {
                                                        onItemClick(
                                                            DiscussionTopicsViewModel.FOLLOWING_POSTS,
                                                            "",
                                                            context.getString(R.string.discussion_posts_following)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        itemsIndexed(uiState.data) { index, topic ->
                                            if (topic.children.isNotEmpty()) {
                                                Text(
                                                    modifier = Modifier.padding(
                                                        top = 10.dp
                                                    ),
                                                    text = topic.name,
                                                    style = MaterialTheme.appTypography.titleMedium,
                                                    color = MaterialTheme.appColors.textPrimaryVariant
                                                )
                                            } else {
                                                TopicItem(topic = topic, onClick = { id, title ->
                                                    onItemClick(
                                                        DiscussionTopicsViewModel.TOPIC,
                                                        id,
                                                        title
                                                    )
                                                })
                                                if (uiState.data.getOrNull(index + 1)?.children?.isEmpty() == true) {
                                                    Divider()
                                                }
                                            }
                                        }
                                    }
                                }

                                DiscussionTopicsUIState.Loading -> {}
                                else -> {
                                    NoContentScreen(noContentScreenType = NoContentScreenType.COURSE_DISCUSSIONS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionTopicsScreenPreview() {
    OpenEdXTheme {
        DiscussionTopicsUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionTopicsUIState.Topics(listOf(mockTopic, mockTopic)),
            uiMessage = null,
            onItemClick = { _, _, _ -> },
            onSearchClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorDiscussionTopicsScreenPreview() {
    OpenEdXTheme {
        DiscussionTopicsUI(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionTopicsUIState.Error,
            uiMessage = null,
            onItemClick = { _, _, _ -> },
            onSearchClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionTopicsScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionTopicsUI(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionTopicsUIState.Topics(listOf(mockTopic, mockTopic)),
            uiMessage = null,
            onItemClick = { _, _, _ -> },
            onSearchClick = {}
        )
    }
}

private val mockTopic = Topic(
    id = "",
    name = "All Topics",
    threadListUrl = "",
    children = emptyList()
)
