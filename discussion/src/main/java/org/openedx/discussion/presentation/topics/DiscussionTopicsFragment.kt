package org.openedx.discussion.presentation.topics

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.FragmentViewType
import org.openedx.core.UIMessage
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.ui.ThreadItemCategory
import org.openedx.discussion.presentation.ui.TopicItem
import org.openedx.discussion.R as discussionR

class DiscussionTopicsFragment : Fragment() {

    private val viewModel by viewModel<DiscussionTopicsViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<DiscussionRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.courseName = requireArguments().getString(ARG_COURSE_NAME, "")
        viewModel.getCourseTopics()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(DiscussionTopicsUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)
                DiscussionTopicsScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.updateCourseTopics()
                    },
                    onItemClick = { action, data, title ->
                        viewModel.discussionClickedEvent(action, data, title)
                        router.navigateToDiscussionThread(
                            requireActivity().supportFragmentManager,
                            action,
                            viewModel.courseId,
                            data,
                            title,
                            FragmentViewType.FULL_CONTENT
                        )
                    },
                    onSearchClick = {
                        router.navigateToSearchThread(
                            requireActivity().supportFragmentManager,
                            viewModel.courseId
                        )
                    }
                )
            }
        }
    }

    companion object {
        const val TOPIC = "Topic"
        const val ALL_POSTS = "All posts"
        const val FOLLOWING_POSTS = "Following"

        private const val ARG_COURSE_ID = "argCourseID"
        private const val ARG_COURSE_NAME = "argCourseName"
        fun newInstance(
            courseId: String,
            courseName: String
        ): DiscussionTopicsFragment {
            val fragment = DiscussionTopicsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_COURSE_NAME to courseName,
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionTopicsScreen(
    windowSize: WindowSize,
    uiState: DiscussionTopicsUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    onSearchClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (String, String, String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

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
                StaticSearchBar(
                    modifier = Modifier
                        .height(48.dp)
                        .then(searchTabWidth)
                        .padding(horizontal = contentPaddings)
                        .fillMaxWidth(),
                    text = stringResource(id = discussionR.string.discussion_search_all_posts),
                    onClick = onSearchClick
                )
                Surface(
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    Box(Modifier.pullRefresh(pullRefreshState)) {
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
                                                text = stringResource(id = discussionR.string.discussion_main_categories),
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
                                                    name = stringResource(id = discussionR.string.discussion_all_posts),
                                                    painterResource = painterResource(id = discussionR.drawable.discussion_all_posts),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(categoriesHeight),
                                                    onClick = {
                                                        onItemClick(
                                                            DiscussionTopicsFragment.ALL_POSTS,
                                                            "",
                                                            context.getString(discussionR.string.discussion_all_posts)
                                                        )
                                                    })
                                                ThreadItemCategory(
                                                    name = stringResource(id = discussionR.string.discussion_posts_following),
                                                    painterResource = painterResource(id = discussionR.drawable.discussion_star),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(categoriesHeight),
                                                    onClick = {
                                                        onItemClick(
                                                            DiscussionTopicsFragment.FOLLOWING_POSTS,
                                                            "",
                                                            context.getString(discussionR.string.discussion_posts_following)
                                                        )
                                                    })
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
                                                        DiscussionTopicsFragment.TOPIC,
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

                                DiscussionTopicsUIState.Loading -> {
                                    Box(
                                        Modifier
                                            .fillMaxSize(), contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                    }
                                }
                            }
                        }
                        PullRefreshIndicator(
                            refreshing,
                            pullRefreshState,
                            Modifier.align(Alignment.TopCenter)
                        )
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
        DiscussionTopicsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionTopicsUIState.Topics(listOf(mockTopic, mockTopic)),
            uiMessage = null,
            refreshing = false,
            onItemClick = { _, _, _ -> },
            onSwipeRefresh = {},
            onSearchClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionTopicsScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionTopicsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionTopicsUIState.Topics(listOf(mockTopic, mockTopic)),
            uiMessage = null,
            refreshing = false,
            onItemClick = { _, _, _ -> },
            onSwipeRefresh = {},
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