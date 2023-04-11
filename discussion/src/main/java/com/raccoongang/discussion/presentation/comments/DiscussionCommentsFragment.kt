@file:OptIn(ExperimentalComposeUiApi::class)

package com.raccoongang.discussion.presentation.comments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.ProfileImage
import com.raccoongang.core.extension.TextConverter
import com.raccoongang.core.extension.parcelable
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.discussion.domain.model.DiscussionComment
import com.raccoongang.discussion.domain.model.DiscussionType
import com.raccoongang.discussion.presentation.DiscussionRouter
import com.raccoongang.discussion.presentation.ui.CommentItem
import com.raccoongang.discussion.presentation.ui.ThreadMainItem
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DiscussionCommentsFragment : Fragment() {

    private val viewModel by viewModel<DiscussionCommentsViewModel> {
        parametersOf((requireArguments().parcelable(ARG_THREAD)!!))
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
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(DiscussionCommentsUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val scrollToBottom by viewModel.scrollToBottom.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                DiscussionCommentsScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    title = viewModel.title,
                    canLoadMore = canLoadMore,
                    scrollToBottom = scrollToBottom,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.updateThreadComments()
                    },
                    paginationCallBack = {
                        viewModel.fetchMore()
                    },
                    onItemClick = { action, id, bool ->
                        if (!viewModel.thread.closed) {
                            when (action) {
                                ACTION_UPVOTE_COMMENT -> viewModel.setCommentUpvoted(id, bool)
                                ACTION_UPVOTE_THREAD -> viewModel.setThreadUpvoted(bool)
                                ACTION_FOLLOW_THREAD -> viewModel.setThreadFollowed(bool)
                            }
                        } else {
                            when (action) {
                                ACTION_REPORT_COMMENT -> viewModel.setCommentReported(id, bool)
                                ACTION_REPORT_THREAD -> viewModel.setThreadReported(bool)
                            }
                        }
                    },
                    onCommentClick = {
                        router.navigateToDiscussionResponses(
                            requireActivity().supportFragmentManager, it, viewModel.thread.closed
                        )
                    },
                    onAddResponseClick = {
                        viewModel.createComment(it)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )
            }
        }
    }

    companion object {
        const val ACTION_UPVOTE_COMMENT = "action_upvote_comment"
        const val ACTION_REPORT_COMMENT = "action_report_comment"
        const val ACTION_UPVOTE_THREAD = "action_upvote_thread"
        const val ACTION_REPORT_THREAD = "action_report_thread"
        const val ACTION_FOLLOW_THREAD = "action_follow_thread"

        private const val ARG_THREAD = "argThread"

        fun newInstance(thread: com.raccoongang.discussion.domain.model.Thread): DiscussionCommentsFragment {
            val fragment = DiscussionCommentsFragment()
            fragment.arguments = bundleOf(
                ARG_THREAD to thread
            )
            return fragment
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionCommentsScreen(
    windowSize: WindowSize,
    uiState: DiscussionCommentsUIState,
    uiMessage: UIMessage?,
    title: String,
    canLoadMore: Boolean,
    scrollToBottom: Boolean,
    refreshing: Boolean,
    onSwipeRefresh: () -> Unit,
    paginationCallBack: () -> Unit,
    onItemClick: (String, String, Boolean) -> Unit,
    onCommentClick: (DiscussionComment) -> Unit,
    onAddResponseClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var responseValue by rememberSaveable {
        mutableStateOf("")
    }

    val sendButtonColor = if (responseValue.isEmpty()) {
        MaterialTheme.appColors.textFieldBorder
    } else {
        MaterialTheme.appColors.primary
    }

    val iconButtonColor = if (responseValue.isEmpty()) {
        MaterialTheme.appColors.textFieldBackgroundVariant
    } else {
        Color.White
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

        val paddingContent by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 24.dp
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .then(screenWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
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
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.appColors.background
            ) {
                Box(Modifier.pullRefresh(pullRefreshState)) {
                    when (uiState) {
                        is DiscussionCommentsUIState.Success -> {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyColumn(
                                    Modifier
                                        .then(screenWidth)
                                        .weight(1f)
                                        .background(MaterialTheme.appColors.background),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    state = scrollState
                                ) {
                                    item {
                                        ThreadMainItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.appColors.background)
                                                .padding(horizontal = paddingContent)
                                                .padding(top = 32.dp),
                                            thread = uiState.thread,
                                            onClick = { action, bool ->
                                                onItemClick(action, uiState.thread.id, bool)
                                            })
                                    }
                                    if (uiState.commentsData.isNotEmpty()) {
                                        item {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = paddingContent)
                                                    .padding(top = 24.dp, bottom = 4.dp),
                                                text = pluralStringResource(
                                                    id = com.raccoongang.discussion.R.plurals.discussion_responses_capitalized,
                                                    uiState.count,
                                                    uiState.count
                                                ),
                                                color = MaterialTheme.appColors.textPrimary,
                                                style = MaterialTheme.appTypography.titleLarge
                                            )
                                        }
                                    }
                                    items(uiState.commentsData) { comment ->
                                        CommentItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = paddingContent)
                                                .clickable {
                                                    onCommentClick(comment)
                                                },
                                            comment = comment,
                                            onClick = { action, commentId, bool ->
                                                onItemClick(action, commentId, bool)
                                            },
                                            onAddCommentClick = {
                                                onCommentClick(comment)
                                            })
                                    }
                                    item {
                                        if (canLoadMore) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                            }
                                        }
                                    }
                                }
                                if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
                                    paginationCallBack()
                                }
                                if (!isSystemInDarkTheme()) {
                                    Divider(color = MaterialTheme.appColors.cardViewBorder)
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.appColors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        Modifier
                                            .then(screenWidth)
                                            .heightIn(84.dp, Dp.Unspecified)
                                            .padding(top = 16.dp, bottom = 24.dp)
                                            .padding(horizontal = 24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(36.dp, 80.dp),
                                            value = responseValue,
                                            onValueChange = { str ->
                                                responseValue = str
                                            },
                                            shape = MaterialTheme.appShapes.buttonShape,
                                            textStyle = MaterialTheme.appTypography.labelLarge,
                                            maxLines = 3,
                                            placeholder = {
                                                Text(
                                                    text = stringResource(id = com.raccoongang.discussion.R.string.discussion_add_response),
                                                    color = MaterialTheme.appColors.textFieldHint,
                                                    style = MaterialTheme.appTypography.labelLarge,
                                                )
                                            },
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                backgroundColor = MaterialTheme.appColors.textFieldBackgroundVariant,
                                                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                                                textColor = MaterialTheme.appColors.textFieldText
                                            ),
                                            enabled = !uiState.thread.closed
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(sendButtonColor)
                                                .clickable {
                                                    if (responseValue.isNotEmpty()) {
                                                        onAddResponseClick(responseValue.trim())
                                                        responseValue = ""
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                modifier = Modifier.padding(7.dp),
                                                painter = painterResource(id = com.raccoongang.discussion.R.drawable.discussion_ic_send),
                                                contentDescription = null,
                                                tint = iconButtonColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is DiscussionCommentsUIState.Loading -> {
                            Box(
                                Modifier
                                    .fillMaxSize(), contentAlignment = Alignment.Center
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


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionCommentsScreenPreview() {
    NewEdxTheme() {
        DiscussionCommentsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionCommentsUIState.Success(
                mockThread,
                listOf(mockComment, mockComment),
                2
            ),
            uiMessage = null,
            title = "Test Screen",
            canLoadMore = false,
            paginationCallBack = {},
            onItemClick = { _, _, _ ->

            },
            onCommentClick = {},
            onAddResponseClick = {},
            onBackClick = {},
            scrollToBottom = false,
            refreshing = false,
            onSwipeRefresh = {}
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionCommentsScreenTabletPreview() {
    NewEdxTheme() {
        DiscussionCommentsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionCommentsUIState.Success(
                mockThread,
                listOf(mockComment, mockComment),
                2
            ),
            uiMessage = null,
            title = "Test Screen",
            canLoadMore = false,
            paginationCallBack = {},
            onItemClick = { _, _, _ ->

            },
            onCommentClick = {},
            onAddResponseClick = {},
            onBackClick = {},
            scrollToBottom = false,
            refreshing = false,
            onSwipeRefresh = {}
        )
    }
}

private val mockThread = com.raccoongang.discussion.domain.model.Thread(
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

private val mockComment = DiscussionComment(
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
    false,
    "",
    "",
    "",
    21,
    emptyList(),
    profileImage = ProfileImage("", "", "", "", false),
    mapOf()
)