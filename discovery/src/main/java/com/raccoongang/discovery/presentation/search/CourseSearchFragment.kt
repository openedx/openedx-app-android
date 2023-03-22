package com.raccoongang.discovery.presentation.search

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.fragment.app.Fragment
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.Media
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.discovery.presentation.DiscoveryRouter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.raccoongang.discovery.R as discoveryR

class CourseSearchFragment : Fragment() {

    private val viewModel by viewModel<CourseSearchViewModel>()

    private val router by inject<DiscoveryRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(
                    CourseSearchUIState.Courses(
                        emptyList(), 0
                    )
                )
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)

                CourseSearchScreen(
                    windowSize = windowSize,
                    state = uiState,
                    uiMessage = uiMessage,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onSearchTextChanged = {
                        viewModel.search(it)
                    },
                    onSwipeRefresh = {
                        viewModel.updateSearchQuery()
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    onItemClick = {
                        router.navigateToCourseDetail(
                            requireActivity().supportFragmentManager,
                            it
                        )
                    }
                )
            }
        }
    }

}


@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun CourseSearchScreen(
    windowSize: WindowSize,
    state: CourseSearchUIState,
    uiMessage: UIMessage?,
    canLoadMore: Boolean,
    refreshing: Boolean,
    onBackClick: () -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSwipeRefresh: () -> Unit,
    paginationCallback: () -> Unit,
    onItemClick: (String) -> Unit
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
                            text = stringResource(id = com.raccoongang.core.R.string.core_search),
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
                                stringResource(id = discoveryR.string.discovery_start_typing_to_find)
                            } else {
                                pluralStringResource(
                                    id = discoveryR.plurals.discovery_found_courses,
                                    (state as? CourseSearchUIState.Courses)?.numCourses ?: 0,
                                    (state as? CourseSearchUIState.Courses)?.numCourses ?: 0
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
                                        text = stringResource(id = discoveryR.string.discovery_search_results),
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
                            when (state) {
                                is CourseSearchUIState.Loading -> {
                                    item {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 25.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                is CourseSearchUIState.Courses -> {
                                    items(state.courses) { course ->
                                        DiscoveryCourseItem(
                                            course,
                                            windowSize = windowSize,
                                            onClick = { courseId ->
                                                onItemClick(courseId)
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
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                    if (scrollState.shouldLoadMore(firstVisibleIndex, 4)) {
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
fun CourseSearchScreenPreview() {
    NewEdxTheme {
        CourseSearchScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = CourseSearchUIState.Courses(listOf(mockCourse, mockCourse), 2),
            uiMessage = null,
            canLoadMore = false,
            refreshing = false,
            onBackClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onItemClick = {}
        )
    }
}

@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CourseSearchScreenTabletPreview() {
    NewEdxTheme {
        CourseSearchScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            state = CourseSearchUIState.Courses(listOf(mockCourse, mockCourse), 2),
            uiMessage = null,
            canLoadMore = false,
            refreshing = false,
            onBackClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onItemClick = {}
        )
    }
}


private val mockCourse = Course(
    id = "id",
    blocksUrl = "blocksUrl",
    courseId = "courseId",
    effort = "effort",
    enrollmentStart = null,
    enrollmentEnd = null,
    hidden = false,
    invitationOnly = false,
    media = Media(),
    mobileAvailable = true,
    name = "Test course",
    number = "number",
    org = "EdX",
    pacing = "pacing",
    shortDescription = "shortDescription",
    start = "start",
    end = "end",
    startDisplay = "startDisplay",
    startType = "startType",
    overview = ""
)