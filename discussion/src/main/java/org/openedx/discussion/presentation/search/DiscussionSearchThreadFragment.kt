package org.openedx.discussion.presentation.search

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.R
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.SearchBar
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.search.DiscussionSearchThreadFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discussion.presentation.ui.ThreadItem
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.discussion.R as discussionR

class DiscussionSearchThreadFragment : Fragment() {

    private val viewModel by viewModel<DiscussionSearchThreadViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID))
    }

    private val router by inject<DiscussionRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
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

                val uiState by viewModel.uiState.observeAsState(
                    DiscussionSearchThreadUIState.Threads(
                        emptyList(),
                        0
                    )
                )
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                DiscussionSearchThreadScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    canLoadMore = canLoadMore,
                    onItemClick = {
                        router.navigateToDiscussionComments(
                            requireActivity().supportFragmentManager,
                            it
                        )
                    },
                    onSearchTextChanged = { viewModel.searchThreads(it) },
                    onSwipeRefresh = { viewModel.updateSearchQuery() },
                    paginationCallback = { viewModel.fetchMore() },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        const val LOAD_MORE_THRESHOLD = 4
        fun newInstance(
            courseId: String
        ): DiscussionSearchThreadFragment {
            val fragment = DiscussionSearchThreadFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionSearchThreadScreen(
    windowSize: WindowSize,
    uiState: DiscussionSearchThreadUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    canLoadMore: Boolean,
    onItemClick: (org.openedx.discussion.domain.model.Thread) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSwipeRefresh: () -> Unit,
    paginationCallback: () -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = scrollState.isScrollInProgress) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
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
        val searchTab by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.width(420.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            )
        }
        val contentPaddings by remember {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(
                        top = 32.dp,
                        bottom = 40.dp
                    ),
                    compact = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .zIndex(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BackBtn {
                            onBackClick()
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp),
                            text = stringResource(id = R.string.core_search),
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    SearchBar(
                        modifier = Modifier
                            .height(48.dp)
                            .then(searchTab),
                        label = "",
                        requestFocus = true,
                        searchValue = textFieldValue,
                        keyboardActions = {
                            focusManager.clearFocus()
                        },
                        onValueChanged = { text ->
                            textFieldValue = text
                            onSearchTextChanged(textFieldValue.text)
                        },
                        onClearValue = {
                            textFieldValue = TextFieldValue("")
                            onSearchTextChanged(textFieldValue.text)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pullRefresh(pullRefreshState)
                    ) {
                        val typingText =
                            if (textFieldValue.text.isEmpty()) {
                                stringResource(id = discussionR.string.discussion_start_typing_to_find)
                            } else {
                                pluralStringResource(
                                    id = discussionR.plurals.discussion_found_threads,
                                    (uiState as? DiscussionSearchThreadUIState.Threads)?.count ?: 0,
                                    (uiState as? DiscussionSearchThreadUIState.Threads)?.count ?: 0
                                )
                            }
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = contentPaddings,
                            state = scrollState
                        ) {
                            item {
                                Column {
                                    Text(
                                        text = stringResource(id = discussionR.string.discussion_search_results),
                                        color = MaterialTheme.appColors.textPrimary,
                                        style = MaterialTheme.appTypography.displaySmall
                                    )
                                    Text(
                                        modifier = Modifier.padding(top = 4.dp),
                                        text = typingText,
                                        color = MaterialTheme.appColors.textPrimary,
                                        style = MaterialTheme.appTypography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                            when (uiState) {
                                is DiscussionSearchThreadUIState.Loading -> {
                                    item {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 25.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    }
                                }
                                is DiscussionSearchThreadUIState.Threads -> {
                                    items(uiState.data) { thread ->
                                        ThreadItem(thread = thread, onClick = onItemClick)
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
                                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                            }
                                        }
                                    }
                                    if (scrollState.shouldLoadMore(firstVisibleIndex, LOAD_MORE_THRESHOLD)) {
                                        paginationCallback()
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
@Composable
fun DiscussionSearchThreadScreenPreview() {
    OpenEdXTheme {
        DiscussionSearchThreadScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionSearchThreadUIState.Threads(listOf(mockThread, mockThread), 2),
            uiMessage = null,
            refreshing = false,
            canLoadMore = true,
            onItemClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onBackClick = {}
        )
    }
}

@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DiscussionSearchThreadScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionSearchThreadScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionSearchThreadUIState.Threads(listOf(mockThread, mockThread), 2),
            uiMessage = null,
            refreshing = false,
            canLoadMore = true,
            onItemClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onBackClick = {}
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
