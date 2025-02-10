package org.openedx.discussion.presentation.threads

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.FragmentViewType
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.SheetContent
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.isImeVisibleState
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.threads.DiscussionThreadsFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discussion.presentation.ui.ThreadItem
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.discussion.R as discussionR

class DiscussionThreadsFragment : Fragment() {

    private val viewModel by viewModel<DiscussionThreadsViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TOPIC_ID, ""),
            requireArguments().getString(ARG_THREAD_TYPE, "")
        )
    }
    private val router by inject<DiscussionRouter>()
    private var viewType = FragmentViewType.FULL_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        viewType =
            FragmentViewType.valueOf(requireArguments().getString(ARG_FRAGMENT_VIEW_TYPE, ""))
        if (viewType == FragmentViewType.MAIN_CONTENT) {
            viewModel.markBlockCompleted(requireArguments().getString(ARG_BLOCK_ID, ""))
        }
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

                val uiState by viewModel.uiState.observeAsState(DiscussionThreadsUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                DiscussionThreadsScreen(
                    windowSize = windowSize,
                    title = requireArguments().getString(ARG_TITLE, ""),
                    uiState = uiState,
                    uiMessage = uiMessage,
                    canLoadMore = canLoadMore,
                    viewType = viewType,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.updateThread(SortType.LAST_ACTIVITY_AT.queryParam)
                    },
                    updatedOrder = {
                        viewModel.getThreadByType(it)
                    },
                    updatedFilter = {
                        viewModel.filterThreads(it)
                    },
                    onItemClick = {
                        router.navigateToDiscussionComments(
                            requireActivity().supportFragmentManager,
                            it
                        )
                    },
                    onCreatePostClick = {
                        val id = viewModel.topicId.ifEmpty { "course" }
                        router.navigateToAddThread(
                            requireActivity().supportFragmentManager,
                            id,
                            viewModel.courseId
                        )
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_THREAD_TYPE = "threadType"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_TOPIC_ID = "topicId"
        private const val ARG_TITLE = "title"
        private const val ARG_FRAGMENT_VIEW_TYPE = "fragmentViewType"
        const val LOAD_MORE_THRESHOLD = 4

        fun newInstance(
            threadType: String,
            courseId: String,
            topicId: String,
            title: String,
            viewType: String,
            blockId: String = ""
        ): DiscussionThreadsFragment {
            val fragment = DiscussionThreadsFragment()
            fragment.arguments = bundleOf(
                ARG_THREAD_TYPE to threadType,
                ARG_COURSE_ID to courseId,
                ARG_TOPIC_ID to topicId,
                ARG_TITLE to title,
                ARG_FRAGMENT_VIEW_TYPE to viewType,
                ARG_BLOCK_ID to blockId
            )
            return fragment
        }
    }
}

@Suppress("MaximumLineLength", "MaxLineLength")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionThreadsScreen(
    windowSize: WindowSize,
    title: String,
    uiState: DiscussionThreadsUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    viewType: FragmentViewType,
    refreshing: Boolean,
    onSwipeRefresh: () -> Unit,
    updatedOrder: (String) -> Unit,
    updatedFilter: (String) -> Unit,
    onItemClick: (org.openedx.discussion.domain.model.Thread) -> Unit,
    onCreatePostClick: () -> Unit,
    paginationCallback: () -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val bottomSheetScaffoldState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val focusManager = LocalFocusManager.current
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })
    val coroutine = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val context = LocalContext.current
    var sortType by rememberSaveable {
        mutableStateOf(
            Pair(
                context.getString(SortType.LAST_ACTIVITY_AT.textRes),
                SortType.LAST_ACTIVITY_AT.queryParam
            )
        )
    }
    var filterType by rememberSaveable {
        mutableStateOf(
            Pair(
                context.getString(FilterType.ALL_POSTS.textRes),
                FilterType.ALL_POSTS.value
            )
        )
    }
    var expandedList by rememberSaveable {
        mutableStateOf(emptyList<Pair<String, String>>())
    }
    var currentSelectedList by rememberSaveable {
        mutableStateOf("")
    }

    var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val isImeVisible by isImeVisibleState()

    val scaffoldModifier = if (viewType == FragmentViewType.FULL_CONTENT) {
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    } else {
        Modifier.fillMaxSize()
    }

    val statusBarInsets = if (viewType == FragmentViewType.FULL_CONTENT) {
        Modifier
            .fillMaxSize()
            .statusBarsInset()
    } else {
        Modifier
            .fillMaxSize()
    }

    LaunchedEffect(bottomSheetScaffoldState.isVisible) {
        if (!bottomSheetScaffoldState.isVisible) {
            focusManager.clearFocus()
            searchValue = TextFieldValue()
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = scaffoldModifier,
        backgroundColor = MaterialTheme.appColors.background
    ) {
        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(top = 24.dp, bottom = 80.dp),
                    compact = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                        bottom = 80.dp
                    )
                )
            )
        }

        val sortButtonsPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 24.dp
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        ModalBottomSheetLayout(
            modifier = Modifier
                .padding(bottom = if (isImeVisible && bottomSheetScaffoldState.isVisible) 120.dp else 0.dp)
                .noRippleClickable {
                    if (bottomSheetScaffoldState.isVisible) {
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                        }
                    }
                },
            sheetShape = MaterialTheme.appShapes.screenBackgroundShape,
            sheetState = bottomSheetScaffoldState,
            scrimColor = Color.Black.copy(alpha = 0.4f),
            sheetBackgroundColor = MaterialTheme.appColors.background,
            sheetContent = {
                SheetContent(
                    searchValue = searchValue,
                    expandedList = expandedList,
                    onItemClick = { item ->
                        when (currentSelectedList) {
                            FilterType.type -> {
                                filterType = item
                                updatedFilter(filterType.second)
                            }

                            SortType.type -> {
                                sortType = item
                                updatedOrder(sortType.second)
                            }
                        }
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                        }
                    },
                    searchValueChanged = {
                        searchValue = TextFieldValue(it)
                    }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .displayCutoutForLandscape()
                    .then(statusBarInsets),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(contentWidth) {
                    if (viewType == FragmentViewType.FULL_CONTENT) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BackBtn {
                                    onBackClick()
                                }
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp),
                                    text = title,
                                    color = MaterialTheme.appColors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.appTypography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.padding(
                            top = if (viewType == FragmentViewType.FULL_CONTENT) {
                                6.dp
                            } else {
                                0.dp
                            }
                        ),
                        color = MaterialTheme.appColors.background
                    ) {
                        Box(Modifier.pullRefresh(pullRefreshState)) {
                            when (uiState) {
                                is DiscussionThreadsUIState.Threads -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(Modifier.fillMaxSize()) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.appColors.background)
                                                    .padding(
                                                        horizontal = sortButtonsPadding,
                                                        vertical = 16.dp
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                IconText(
                                                    text = filterType.first,
                                                    painter = painterResource(
                                                        id = discussionR.drawable.discussion_ic_filter
                                                    ),
                                                    textStyle = MaterialTheme.appTypography.labelMedium,
                                                    color = MaterialTheme.appColors.textPrimary,
                                                    onClick = {
                                                        currentSelectedList = FilterType.type
                                                        expandedList = listOf(
                                                            Pair(
                                                                context.getString(FilterType.ALL_POSTS.textRes),
                                                                FilterType.ALL_POSTS.value
                                                            ),
                                                            Pair(
                                                                context.getString(FilterType.UNREAD.textRes),
                                                                FilterType.UNREAD.value
                                                            ),
                                                            Pair(
                                                                context.getString(FilterType.UNANSWERED.textRes),
                                                                FilterType.UNANSWERED.value
                                                            )
                                                        )
                                                        coroutine.launch {
                                                            if (bottomSheetScaffoldState.isVisible) {
                                                                bottomSheetScaffoldState.hide()
                                                            } else {
                                                                bottomSheetScaffoldState.show()
                                                            }
                                                        }
                                                    }
                                                )
                                                IconText(
                                                    text = sortType.first,
                                                    painter = painterResource(
                                                        id = discussionR.drawable.discussion_ic_sort
                                                    ),
                                                    textStyle = MaterialTheme.appTypography.labelMedium,
                                                    color = MaterialTheme.appColors.textPrimary,
                                                    onClick = {
                                                        currentSelectedList = SortType.type
                                                        expandedList = listOf(
                                                            Pair(
                                                                context.getString(SortType.LAST_ACTIVITY_AT.textRes),
                                                                SortType.LAST_ACTIVITY_AT.queryParam
                                                            ),
                                                            Pair(
                                                                context.getString(SortType.COMMENT_COUNT.textRes),
                                                                SortType.COMMENT_COUNT.queryParam
                                                            ),
                                                            Pair(
                                                                context.getString(SortType.VOTE_COUNT.textRes),
                                                                SortType.VOTE_COUNT.queryParam
                                                            )
                                                        )
                                                        coroutine.launch {
                                                            if (bottomSheetScaffoldState.isVisible) {
                                                                bottomSheetScaffoldState.hide()
                                                            } else {
                                                                bottomSheetScaffoldState.show()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            Divider()
                                            if (uiState.data.isNotEmpty()) {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = listPadding,
                                                    state = scrollState
                                                ) {
                                                    item {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                modifier = Modifier.weight(1f),
                                                                text = title,
                                                                color = MaterialTheme.appColors.textPrimary,
                                                                style = MaterialTheme.appTypography.titleLarge
                                                            )
                                                            Box(
                                                                Modifier
                                                                    .size(40.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        MaterialTheme.appColors.secondaryButtonBackground
                                                                    )
                                                                    .clickable {
                                                                        onCreatePostClick()
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.size(16.dp),
                                                                    painter = painterResource(
                                                                        discussionR.drawable.discussion_ic_add_comment
                                                                    ),
                                                                    contentDescription = stringResource(
                                                                        discussionR.string.discussion_add_comment
                                                                    ),
                                                                    tint = MaterialTheme.appColors.primaryButtonText
                                                                )
                                                            }
                                                        }
                                                    }
                                                    items(uiState.data) { threadItem ->
                                                        ThreadItem(thread = threadItem, onClick = {
                                                            onItemClick(it)
                                                        })
                                                        Divider()
                                                    }
                                                    item {
                                                        if (canLoadMore) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 16.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    color = MaterialTheme.appColors.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (scrollState.shouldLoadMore(
                                                            firstVisibleIndex,
                                                            LOAD_MORE_THRESHOLD
                                                        )
                                                    ) {
                                                        paginationCallback()
                                                    }
                                                }
                                            } else {
                                                val noDiscussionsScrollState = rememberScrollState()
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterHorizontally)
                                                        .verticalScroll(noDiscussionsScrollState)
                                                        .padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        text = title,
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        style = MaterialTheme.appTypography.titleLarge
                                                    )
                                                    Spacer(modifier = Modifier.height(20.dp))
                                                    Icon(
                                                        modifier = Modifier.size(100.dp),
                                                        painter = painterResource(
                                                            id = discussionR.drawable.discussion_ic_empty
                                                        ),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.appColors.textPrimary
                                                    )
                                                    Spacer(Modifier.height(36.dp))
                                                    Text(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        text = stringResource(discussionR.string.discussion_no_yet),
                                                        style = MaterialTheme.appTypography.titleLarge,
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(Modifier.height(12.dp))
                                                    Text(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        text = stringResource(
                                                            discussionR.string.discussion_click_button_create_discussion
                                                        ),
                                                        style = MaterialTheme.appTypography.bodyLarge,
                                                        color = MaterialTheme.appColors.textPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(Modifier.height(40.dp))
                                                    OpenEdXOutlinedButton(
                                                        modifier = Modifier
                                                            .widthIn(184.dp, Dp.Unspecified),
                                                        text = stringResource(
                                                            id = discussionR.string.discussion_create_post
                                                        ),
                                                        onClick = {
                                                            onCreatePostClick()
                                                        },
                                                        content = {
                                                            Icon(
                                                                painter = painterResource(
                                                                    id = discussionR.drawable.discussion_ic_add_comment
                                                                ),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.appColors.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = stringResource(
                                                                    id = discussionR.string.discussion_create_post
                                                                ),
                                                                color = MaterialTheme.appColors.primary,
                                                                style = MaterialTheme.appTypography.labelLarge
                                                            )
                                                        },
                                                        borderColor = MaterialTheme.appColors.primary,
                                                        textColor = MaterialTheme.appColors.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is DiscussionThreadsUIState.Loading -> {
                                    Box(
                                        Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
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
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionThreadsScreenPreview() {
    OpenEdXTheme {
        DiscussionThreadsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            "All posts",
            uiState = DiscussionThreadsUIState.Threads(listOf(mockThread, mockThread, mockThread)),
            uiMessage = null,
            onItemClick = {},
            onBackClick = {},
            updatedOrder = {},
            updatedFilter = {},
            onCreatePostClick = {},
            viewType = FragmentViewType.FULL_CONTENT,
            onSwipeRefresh = {},
            paginationCallback = {},
            refreshing = false,
            canLoadMore = false
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionThreadsScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionThreadsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            "All posts",
            uiState = DiscussionThreadsUIState.Threads(listOf(mockThread, mockThread, mockThread)),
            uiMessage = null,
            onItemClick = {},
            onBackClick = {},
            updatedOrder = {},
            updatedFilter = {},
            onCreatePostClick = {},
            viewType = FragmentViewType.FULL_CONTENT,
            onSwipeRefresh = {},
            paginationCallback = {},
            refreshing = false,
            canLoadMore = false,
        )
    }
}

private val mockThread = org.openedx.discussion.domain.model.Thread(
    "",
    "",
    "",
    "",
    "",
    "",
    "",
    TextConverter.textToLinkedImageText(""),
    false,
    true,
    20,
    emptyList(),
    false,
    "",
    "",
    "",
    "",
    DiscussionType.DISCUSSION,
    "",
    "",
    "Discussion title long Discussion title long good item",
    true,
    false,
    true,
    21,
    4,
    false,
    false,
    mapOf(),
    10,
    false,
    false
)
