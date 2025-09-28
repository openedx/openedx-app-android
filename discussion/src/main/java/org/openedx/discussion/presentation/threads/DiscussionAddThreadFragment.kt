package org.openedx.discussion.presentation.threads

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedTextField
import org.openedx.core.ui.SheetContent
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.isImeVisibleState
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.discussion.R as discussionR

class DiscussionAddThreadFragment : Fragment() {

    private val viewModel by viewModel<DiscussionAddThreadViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiMessage by viewModel.uiMessage.observeAsState()
                val isLoading by viewModel.isLoading.observeAsState(false)
                val success by viewModel.newThread.observeAsState()

                DiscussionAddThreadScreen(
                    windowSize = windowSize,
                    topicData = viewModel.getHandledTopicById(
                        requireArguments().getString(
                            ARG_TOPIC_ID,
                            ""
                        )
                    ),
                    topics = viewModel.getHandledTopics(),
                    uiMessage = uiMessage,
                    isLoading = isLoading,
                    onPostDiscussionClick = { type, id, title, rawBody, bool ->
                        viewModel.createThread(id, type, title, rawBody, bool)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                )

                if (success != null) {
                    viewModel.sendThreadAdded()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    companion object {
        private const val ARG_TOPIC_ID = "topicId"
        private const val ARG_COURSE_ID = "courseId"

        fun newInstance(
            topicId: String,
            courseId: String,
        ): DiscussionAddThreadFragment {
            val fragment = DiscussionAddThreadFragment()
            fragment.arguments = bundleOf(
                ARG_TOPIC_ID to topicId,
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}

@Composable
private fun DiscussionAddThreadScreen(
    windowSize: WindowSize,
    topicData: Pair<String, String>,
    topics: List<Pair<String, String>>,
    uiMessage: UIMessage?,
    isLoading: Boolean,
    onPostDiscussionClick: (String, String, String, String, Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomSheetScaffoldState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val coroutine = rememberCoroutineScope()
    var currentPage by rememberSaveable {
        mutableStateOf(0)
    }
    var titleValue by rememberSaveable {
        mutableStateOf("")
    }
    var discussionValue by rememberSaveable {
        mutableStateOf("")
    }
    var discussionType by rememberSaveable {
        mutableStateOf(DiscussionType.DISCUSSION.value)
    }
    var postToTopic by rememberSaveable {
        mutableStateOf(topicData)
    }
    var followPost by rememberSaveable {
        mutableStateOf(false)
    }
    val expandedList by rememberSaveable {
        mutableStateOf(topics)
    }
    var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val isImeVisible by isImeVisibleState()

    LaunchedEffect(bottomSheetScaffoldState.isVisible) {
        if (!bottomSheetScaffoldState.isVisible) {
            focusManager.clearFocus()
            searchValue = TextFieldValue()
        }
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

        val buttonWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(232.dp, Dp.Unspecified),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val contentPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = 0.dp,
                    compact = 24.dp
                )
            )
        }

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
            sheetState = bottomSheetScaffoldState,
            sheetShape = MaterialTheme.appShapes.screenBackgroundShape,
            scrimColor = Color.Black.copy(alpha = 0.4f),
            sheetBackgroundColor = MaterialTheme.appColors.background,
            sheetContent = {
                SheetContent(
                    title = stringResource(id = discussionR.string.discussion_topic),
                    searchValue = searchValue,
                    expandedList = expandedList,
                    onItemClick = { item ->
                        postToTopic = item
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
            HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    screenWidth
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
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
                                text = if (currentPage == 0) {
                                    stringResource(id = discussionR.string.discussion_create_post)
                                } else {
                                    stringResource(id = discussionR.string.discussion_create_question)
                                },
                                color = MaterialTheme.appColors.textPrimary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.appTypography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.appColors.background,
                        shape = MaterialTheme.appShapes.screenBackgroundShape
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.appColors.background)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                Modifier.padding(horizontal = contentPadding, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(id = discussionR.string.discussion_select_post_type),
                                    style = MaterialTheme.appTypography.titleMedium,
                                    color = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(Modifier.height(16.dp))
                                Tabs(
                                    tabs = listOf(
                                        stringResource(id = discussionR.string.discussion_discussion),
                                        stringResource(id = discussionR.string.discussion_question)
                                    ),
                                    currentPage = currentPage,
                                    onItemClick = { bool ->
                                        if (bool) {
                                            discussionType = DiscussionType.QUESTION.value
                                            currentPage = 1
                                        } else {
                                            discussionType = DiscussionType.DISCUSSION.value
                                            currentPage = 0
                                        }
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                                SelectableField(
                                    text = postToTopic.first,
                                    onClick = {
                                        coroutine.launch {
                                            if (bottomSheetScaffoldState.isVisible) {
                                                bottomSheetScaffoldState.hide()
                                            } else {
                                                bottomSheetScaffoldState.show()
                                            }
                                        }
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                                OpenEdXOutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    stringResource(id = discussionR.string.discussion_title),
                                    isSingleLine = true,
                                    withRequiredMark = true,
                                    imeAction = ImeAction.Next,
                                    keyboardActions = { focusManager ->
                                        focusManager.clearFocus()
                                    },
                                    onValueChanged = { value ->
                                        titleValue = value
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                                OpenEdXOutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    title = if (currentPage == 0) {
                                        stringResource(id = discussionR.string.discussion_discussion)
                                    } else {
                                        stringResource(
                                            id = discussionR.string.discussion_question
                                        )
                                    },
                                    isSingleLine = false,
                                    withRequiredMark = true,
                                    imeAction = ImeAction.Default,
                                    keyboardActions = { focusManager ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    onValueChanged = { value ->
                                        discussionValue = value
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        modifier = Modifier.size(24.dp),
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.appColors.primary,
                                            uncheckedColor = MaterialTheme.appColors.textFieldText
                                        ),
                                        checked = followPost,
                                        onCheckedChange = {
                                            followPost = it
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (currentPage == 0) {
                                            stringResource(id = discussionR.string.discussion_follow_discussion)
                                        } else {
                                            stringResource(id = discussionR.string.discussion_follow_question)
                                        },
                                        color = MaterialTheme.appColors.textFieldText,
                                        style = MaterialTheme.appTypography.labelLarge,
                                        modifier = Modifier.noRippleClickable {
                                            followPost = !followPost
                                        }
                                    )
                                }
                                Spacer(Modifier.height(44.dp))
                                if (isLoading) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                } else {
                                    OpenEdXButton(
                                        modifier = buttonWidth,
                                        text = if (currentPage == 0) {
                                            stringResource(id = discussionR.string.discussion_create_post)
                                        } else {
                                            stringResource(id = discussionR.string.discussion_create_question)
                                        },
                                        onClick = {
                                            onPostDiscussionClick(
                                                discussionType,
                                                postToTopic.second,
                                                titleValue,
                                                discussionValue,
                                                followPost
                                            )
                                        }
                                    )
                                }
                                Spacer(Modifier.height(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tabs(
    tabs: List<String>,
    currentPage: Int,
    onItemClick: (Boolean) -> Unit,
    isLimited: Boolean = false,
) {
    val isFirstPage = currentPage == 0
    TabRow(
        selectedTabIndex = currentPage,
        backgroundColor = MaterialTheme.appColors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(percent = 20))
            .border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                RoundedCornerShape(percent = 20)
            ),
        indicator = { _ ->
            Box {}
        }
    ) {
        tabs.forEachIndexed { index, text ->
            val selected = currentPage == index
            val textColor = if (selected) {
                Color.White
            } else {
                MaterialTheme.appColors.textPrimaryVariant
            }
            Tab(
                modifier = if (selected) {
                    Modifier
                        .clip(RoundedCornerShape(percent = 20))
                        .background(
                            MaterialTheme.appColors.primary
                        )
                } else {
                    Modifier
                        .clip(RoundedCornerShape(percent = 20))
                        .background(
                            MaterialTheme.appColors.surface
                        )
                },
                selected = selected,
                onClick = {
                    if (!isLimited && !selected) {
                        onItemClick(isFirstPage)
                    }
                },
                text = { Text(text = text, color = textColor) }
            )
        }
    }
}

@Composable
private fun SelectableField(
    text: String,
    onClick: () -> Unit,
) {
    Column {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = discussionR.string.discussion_topic),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            readOnly = true,
            enabled = false,
            value = text,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = MaterialTheme.appColors.textFieldBorder,
                backgroundColor = MaterialTheme.appColors.surface,
                textColor = MaterialTheme.appColors.textFieldText,
                unfocusedLabelColor = MaterialTheme.appColors.textFieldText,
                disabledTextColor = MaterialTheme.appColors.textFieldText
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            textStyle = MaterialTheme.appTypography.bodyMedium,
            onValueChange = { },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimaryVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable {
                    onClick()
                }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DiscussionAddThreadScreenPreview() {
    OpenEdXTheme {
        DiscussionAddThreadScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            topicData = Pair("", "General"),
            topics = emptyList(),
            uiMessage = null,
            isLoading = false,
            onBackClick = {},
            onPostDiscussionClick = { _, _, _, _, _ -> }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun DiscussionAddThreadScreenTabletPreview() {
    OpenEdXTheme {
        DiscussionAddThreadScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            topicData = Pair("", "General"),
            topics = emptyList(),
            uiMessage = null,
            isLoading = false,
            onBackClick = {},
            onPostDiscussionClick = { _, _, _, _, _ -> }
        )
    }
}
