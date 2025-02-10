@file:OptIn(ExperimentalComposeUiApi::class)

package org.openedx.discussion.presentation.responses

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.comments.DiscussionCommentsFragment
import org.openedx.discussion.presentation.responses.DiscussionResponsesFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discussion.presentation.ui.CommentMainItem
import org.openedx.foundation.extension.parcelable
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.discussion.R as discussionR

class DiscussionResponsesFragment : Fragment() {

    private val viewModel by viewModel<DiscussionResponsesViewModel> {
        parametersOf(requireArguments().parcelable(ARG_COMMENT))
    }

    private val router by inject<DiscussionRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        viewModel.isThreadClosed = requireArguments().getBoolean(ARG_IS_CLOSED, false)
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

                val uiState by viewModel.uiState.observeAsState(DiscussionResponsesUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                DiscussionResponsesScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    isClosed = viewModel.isThreadClosed,
                    onSwipeRefresh = {
                        viewModel.updateCommentResponses()
                    },
                    paginationCallBack = {
                        viewModel.fetchMore()
                    },
                    onItemClick = { action, id, bool ->
                        when (action) {
                            DiscussionCommentsFragment.ACTION_UPVOTE_COMMENT -> {
                                if (!viewModel.isThreadClosed) {
                                    viewModel.setCommentUpvoted(
                                        id,
                                        bool
                                    )
                                }
                            }

                            DiscussionCommentsFragment.ACTION_REPORT_COMMENT -> {
                                viewModel.setCommentReported(
                                    id,
                                    bool
                                )
                            }
                        }
                    },
                    addCommentClick = {
                        viewModel.createComment(it)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onUserPhotoClick = { username ->
                        router.navigateToAnothersProfile(
                            requireActivity().supportFragmentManager,
                            username
                        )
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_COMMENT = "comment"
        private const val ARG_IS_CLOSED = "isClosed"
        const val LOAD_MORE_THRESHOLD = 4

        fun newInstance(
            comment: DiscussionComment,
            isClosed: Boolean
        ): DiscussionResponsesFragment {
            val fragment = DiscussionResponsesFragment()
            fragment.arguments = bundleOf(
                ARG_COMMENT to comment,
                ARG_IS_CLOSED to isClosed
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DiscussionResponsesScreen(
    windowSize: WindowSize,
    uiState: DiscussionResponsesUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    refreshing: Boolean,
    isClosed: Boolean,
    onSwipeRefresh: () -> Unit,
    paginationCallBack: () -> Unit,
    onItemClick: (String, String, Boolean) -> Unit,
    addCommentClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onUserPhotoClick: (String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var commentValue by rememberSaveable {
        mutableStateOf("")
    }
    val sendButtonColor = if (commentValue.isEmpty()) {
        MaterialTheme.appColors.textFieldBorder
    } else {
        MaterialTheme.appColors.primary
    }

    val iconButtonColor = if (commentValue.isEmpty()) {
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

        val internalPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 16.dp
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
                        .fillMaxWidth()
                        .displayCutoutForLandscape(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        text = stringResource(id = discussionR.string.discussion_comments_title),
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
                        is DiscussionResponsesUIState.Success -> {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .displayCutoutForLandscape(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LazyColumn(
                                        Modifier
                                            .then(screenWidth)
                                            .background(MaterialTheme.appColors.background),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                        state = scrollState
                                    ) {
                                        item {
                                            CommentMainItem(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 16.dp),
                                                internalPadding = internalPadding,
                                                comment = uiState.mainComment,
                                                onClick = { action, _, bool ->
                                                    onItemClick(
                                                        action,
                                                        uiState.mainComment.id,
                                                        bool
                                                    )
                                                },
                                                onUserPhotoClick = { username ->
                                                    onUserPhotoClick(username)
                                                }
                                            )
                                        }
                                        if (uiState.mainComment.childCount > 0) {
                                            item {
                                                Text(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = paddingContent)
                                                        .padding(top = 24.dp, bottom = 8.dp),
                                                    text = pluralStringResource(
                                                        id = org.openedx.discussion.R.plurals.discussion_comments,
                                                        uiState.mainComment.childCount,
                                                        uiState.mainComment.childCount
                                                    ),
                                                    color = MaterialTheme.appColors.textPrimary,
                                                    style = MaterialTheme.appTypography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }

                                        items(uiState.childComments) { comment ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(IntrinsicSize.Min)
                                                    .padding(start = paddingContent),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(1.dp)
                                                        .background(MaterialTheme.appColors.cardViewBorder)
                                                )
                                                CommentMainItem(
                                                    modifier = Modifier
                                                        .padding(4.dp)
                                                        .fillMaxWidth(),
                                                    comment = comment,
                                                    onClick = { action, commentId, bool ->
                                                        onItemClick(action, commentId, bool)
                                                    },
                                                    onUserPhotoClick = { username ->
                                                        onUserPhotoClick(username)
                                                    }
                                                )
                                            }
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
                                    if (scrollState.shouldLoadMore(firstVisibleIndex, LOAD_MORE_THRESHOLD)) {
                                        paginationCallBack()
                                    }
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
                                            .padding(horizontal = 24.dp)
                                            .displayCutoutForLandscape(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(36.dp, 80.dp),
                                            value = commentValue,
                                            onValueChange = { str ->
                                                commentValue = str
                                            },
                                            textStyle = MaterialTheme.appTypography.labelLarge,
                                            maxLines = 3,
                                            shape = MaterialTheme.appShapes.buttonShape,
                                            placeholder = {
                                                Text(
                                                    text = stringResource(
                                                        id = org.openedx.discussion.R.string.discussion_add_comment
                                                    ),
                                                    color = MaterialTheme.appColors.textFieldHint,
                                                    style = MaterialTheme.appTypography.labelLarge,
                                                )
                                            },
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                backgroundColor = MaterialTheme.appColors.textFieldBackgroundVariant,
                                                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                                                textColor = MaterialTheme.appColors.textFieldText
                                            ),
                                            enabled = !isClosed
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(sendButtonColor)
                                                .clickable {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    if (commentValue.isNotEmpty()) {
                                                        addCommentClick(commentValue.trim())
                                                        commentValue = ""
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                modifier = Modifier.padding(7.dp),
                                                painter = painterResource(
                                                    id = org.openedx.discussion.R.drawable.discussion_ic_send
                                                ),
                                                contentDescription = null,
                                                tint = iconButtonColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is DiscussionResponsesUIState.Loading -> {
                            Box(
                                modifier = Modifier
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionResponsesScreenPreview() {
    OpenEdXTheme {
        DiscussionResponsesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DiscussionResponsesUIState.Success(
                mockComment,
                listOf(
                    mockComment,
                    mockComment
                )
            ),
            uiMessage = null,
            canLoadMore = false,
            refreshing = false,
            onSwipeRefresh = {},
            paginationCallBack = { },
            onItemClick = { _, _, _ -> },
            addCommentClick = {},
            onBackClick = {},
            isClosed = false,
            onUserPhotoClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionResponsesScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionResponsesScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = DiscussionResponsesUIState.Success(
                mockComment,
                listOf(
                    mockComment,
                    mockComment
                )
            ),
            uiMessage = null,
            canLoadMore = false,
            refreshing = false,
            onSwipeRefresh = {},
            paginationCallBack = { },
            onItemClick = { _, _, _ -> },
            addCommentClick = {},
            onBackClick = {},
            isClosed = false,
            onUserPhotoClick = {}
        )
    }
}

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
    ProfileImage("", "", "", "", false),
    mapOf()
)
