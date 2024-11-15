package org.openedx.discovery.presentation.search

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextRange
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
import org.openedx.core.domain.model.Media
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.SearchBar
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.search.CourseSearchFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discovery.presentation.ui.DiscoveryCourseItem
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.discovery.R as discoveryR

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
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(
                    CourseSearchUIState.Courses(
                        emptyList(),
                        0
                    )
                )
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)
                val querySearch = arguments?.getString(ARG_SEARCH_QUERY, "") ?: ""

                CourseSearchScreen(
                    windowSize = windowSize,
                    state = uiState,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    querySearch = querySearch,
                    isUserLoggedIn = viewModel.isUserLoggedIn,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
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
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager, null, null)
                    },
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager, null, null)
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_SEARCH_QUERY = "query_search"
        const val LOAD_MORE_THRESHOLD = 4
        fun newInstance(querySearch: String): CourseSearchFragment {
            val fragment = CourseSearchFragment()
            fragment.arguments = bundleOf(
                ARG_SEARCH_QUERY to querySearch
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun CourseSearchScreen(
    windowSize: WindowSize,
    state: CourseSearchUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    canLoadMore: Boolean,
    refreshing: Boolean,
    querySearch: String,
    isUserLoggedIn: Boolean,
    isRegistrationEnabled: Boolean,
    onBackClick: () -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSwipeRefresh: () -> Unit,
    paginationCallback: () -> Unit,
    onItemClick: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = querySearch,
                selection = TextRange(querySearch.length)
            )
        )
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
            .navigationBarsPadding()
            .semantics { testTagsAsResourceId = true },
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if (!isUserLoggedIn) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp,
                        )
                ) {
                    AuthButtonsPanel(
                        onRegisterClick = onRegisterClick,
                        onSignInClick = onSignInClick,
                        showRegisterButton = isRegistrationEnabled
                    )
                }
            }
        }
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
                                .testTag("txt_search_title")
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp),
                            text = stringResource(id = org.openedx.core.R.string.core_search),
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
                                        modifier = Modifier.testTag("txt_search_results_title"),
                                        text = stringResource(id = discoveryR.string.discovery_search_results),
                                        color = MaterialTheme.appColors.textPrimary,
                                        style = MaterialTheme.appTypography.displaySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .testTag("txt_search_results_subtitle")
                                            .padding(top = 4.dp),
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
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    }
                                }

                                is CourseSearchUIState.Courses -> {
                                    items(state.courses) { course ->
                                        DiscoveryCourseItem(
                                            apiHostUrl = apiHostUrl,
                                            course,
                                            windowSize = windowSize,
                                            onClick = { courseId ->
                                                onItemClick(courseId)
                                            }
                                        )
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
    LaunchedEffect(rememberSaveable { true }) {
        onSearchTextChanged(querySearch)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CourseSearchScreenPreview() {
    OpenEdXTheme {
        CourseSearchScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = CourseSearchUIState.Courses(listOf(mockCourse, mockCourse), 2),
            uiMessage = null,
            apiHostUrl = "",
            canLoadMore = false,
            refreshing = false,
            querySearch = "",
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            onBackClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onItemClick = {},
            onSignInClick = {},
            onRegisterClick = {},
        )
    }
}

@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CourseSearchScreenTabletPreview() {
    OpenEdXTheme {
        CourseSearchScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            state = CourseSearchUIState.Courses(listOf(mockCourse, mockCourse), 2),
            uiMessage = null,
            apiHostUrl = "",
            canLoadMore = false,
            refreshing = false,
            querySearch = "",
            isUserLoggedIn = false,
            isRegistrationEnabled = true,
            onBackClick = {},
            onSearchTextChanged = {},
            onSwipeRefresh = {},
            paginationCallback = {},
            onItemClick = {},
            onSignInClick = {},
            onRegisterClick = {},
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
    overview = "",
    isEnrolled = false
)
