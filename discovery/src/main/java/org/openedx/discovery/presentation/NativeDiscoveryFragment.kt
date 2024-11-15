package org.openedx.discovery.presentation

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.AppUpdateState
import org.openedx.core.AppUpdateState.wasUpdateDialogClosed
import org.openedx.core.domain.model.Media
import org.openedx.core.presentation.dialog.appupgrade.AppUpgradeDialogFragment
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRecommendedBox
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.StaticSearchBar
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.R
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.NativeDiscoveryFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.discovery.presentation.ui.DiscoveryCourseItem
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

class NativeDiscoveryFragment : Fragment() {

    private val viewModel by viewModel<NativeDiscoveryViewModel>()
    private val router: DiscoveryRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val canLoadMore by viewModel.canLoadMore.observeAsState(false)
                val refreshing by viewModel.isUpdating.observeAsState(false)
                val appUpgradeEvent by viewModel.appUpgradeEvent.observeAsState()
                val wasUpdateDialogClosed by remember { wasUpdateDialogClosed }
                val querySearch = arguments?.getString(ARG_SEARCH_QUERY, "") ?: ""

                DiscoveryScreen(
                    windowSize = windowSize,
                    state = uiState!!,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    canShowBackButton = viewModel.canShowBackButton,
                    isUserLoggedIn = viewModel.isUserLoggedIn,
                    isRegistrationEnabled = viewModel.isRegistrationEnabled,
                    appUpgradeParameters = AppUpdateState.AppUpgradeParameters(
                        appUpgradeEvent = appUpgradeEvent,
                        wasUpdateDialogClosed = wasUpdateDialogClosed,
                        appUpgradeRecommendedDialog = {
                            val dialog = AppUpgradeDialogFragment.newInstance()
                            dialog.show(
                                requireActivity().supportFragmentManager,
                                AppUpgradeDialogFragment::class.simpleName
                            )
                        },
                        onAppUpgradeRecommendedBoxClick = {
                            AppUpdateState.openPlayMarket(requireContext())
                        },
                        onAppUpgradeRequired = {
                            router.navigateToUpgradeRequired(
                                requireActivity().supportFragmentManager
                            )
                        }
                    ),
                    onSearchClick = {
                        viewModel.discoverySearchBarClickedEvent()
                        router.navigateToCourseSearch(
                            requireActivity().supportFragmentManager,
                            ""
                        )
                    },
                    paginationCallback = {
                        viewModel.fetchMore()
                    },
                    onSwipeRefresh = {
                        viewModel.updateData()
                    },
                    onReloadClick = {
                        viewModel.getCoursesList()
                    },
                    onItemClick = { course ->
                        viewModel.discoveryCourseClicked(course.id, course.name)
                        viewModel.courseDetailClickedEvent(course.id, course.name)
                        router.navigateToCourseDetail(
                            requireActivity().supportFragmentManager,
                            course.id
                        )
                    },
                    onRegisterClick = {
                        router.navigateToSignUp(parentFragmentManager, null, null)
                    },
                    onSignInClick = {
                        router.navigateToSignIn(parentFragmentManager, null, null)
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onSettingsClick = {
                        router.navigateToSettings(requireActivity().supportFragmentManager)
                    }
                )
                LaunchedEffect(uiState) {
                    if (querySearch.isNotEmpty()) {
                        router.navigateToCourseSearch(
                            requireActivity().supportFragmentManager,
                            querySearch
                        )
                        arguments?.putString(ARG_SEARCH_QUERY, "")
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_SEARCH_QUERY = "query_search"
        const val LOAD_MORE_THRESHOLD = 4
        fun newInstance(querySearch: String = ""): NativeDiscoveryFragment {
            val fragment = NativeDiscoveryFragment()
            fragment.arguments = bundleOf(
                ARG_SEARCH_QUERY to querySearch
            )
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun DiscoveryScreen(
    windowSize: WindowSize,
    state: DiscoveryUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    canLoadMore: Boolean,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    canShowBackButton: Boolean,
    isUserLoggedIn: Boolean,
    isRegistrationEnabled: Boolean,
    appUpgradeParameters: AppUpdateState.AppUpgradeParameters,
    onSearchClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onReloadClick: () -> Unit,
    paginationCallback: () -> Unit,
    onItemClick: (Course) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if (!isUserLoggedIn) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp,
                        )
                        .navigationBarsPadding()
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
        val searchTabWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val contentPaddings by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(
                        top = 32.dp,
                        bottom = 40.dp
                    ),
                    compact = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        if (canShowBackButton) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                BackBtn(
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.appColors.primary
                ) {
                    onBackClick()
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Toolbar(
                    label = stringResource(id = R.string.discovery_Discovery),
                    canShowBackBtn = canShowBackButton,
                    canShowSettingsIcon = !canShowBackButton,
                    onBackClick = onBackClick,
                    onSettingsClick = onSettingsClick
                )

                Spacer(modifier = Modifier.height(16.dp))
                StaticSearchBar(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 24.dp)
                        .then(searchTabWidth),
                    onClick = {
                        onSearchClick()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Surface(
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pullRefresh(pullRefreshState)
                ) {
                    when (state) {
                        is DiscoveryUIState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is DiscoveryUIState.Courses -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LazyColumn(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth),
                                    contentPadding = contentPaddings,
                                    state = scrollState
                                ) {
                                    item {
                                        Column {
                                            Text(
                                                modifier = Modifier.testTag("txt_discovery_new"),
                                                text = stringResource(id = R.string.discovery_discovery_new),
                                                color = MaterialTheme.appColors.textPrimary,
                                                style = MaterialTheme.appTypography.displaySmall
                                            )
                                            Text(
                                                modifier = Modifier
                                                    .testTag("txt_discovery_lets_find")
                                                    .padding(top = 4.dp),
                                                text = stringResource(id = R.string.discovery_lets_find),
                                                color = MaterialTheme.appColors.textPrimary,
                                                style = MaterialTheme.appTypography.titleSmall
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                        }
                                    }
                                    items(state.courses) { course ->
                                        DiscoveryCourseItem(
                                            apiHostUrl = apiHostUrl,
                                            course = course,
                                            windowSize = windowSize,
                                            onClick = {
                                                onItemClick(course)
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        when (appUpgradeParameters.appUpgradeEvent) {
                            is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                                if (appUpgradeParameters.wasUpdateDialogClosed) {
                                    AppUpgradeRecommendedBox(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = appUpgradeParameters.onAppUpgradeRecommendedBoxClick
                                    )
                                } else {
                                    if (!AppUpdateState.wasUpdateDialogDisplayed) {
                                        AppUpdateState.wasUpdateDialogDisplayed = true
                                        appUpgradeParameters.appUpgradeRecommendedDialog()
                                    }
                                }
                            }

                            is AppUpgradeEvent.UpgradeRequiredEvent -> {
                                if (!AppUpdateState.wasUpdateDialogDisplayed) {
                                    AppUpdateState.wasUpdateDialogDisplayed = true
                                    appUpgradeParameters.onAppUpgradeRequired()
                                }
                            }

                            else -> {}
                        }
                        if (!isInternetConnectionShown && !hasInternetConnection) {
                            OfflineModeDialog(
                                Modifier
                                    .fillMaxWidth(),
                                onDismissCLick = {
                                    isInternetConnectionShown = true
                                },
                                onReloadClick = {
                                    isInternetConnectionShown = true
                                    onReloadClick()
                                }
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
@Composable
private fun CourseItemPreview() {
    OpenEdXTheme {
        DiscoveryCourseItem(
            apiHostUrl = "",
            course = mockCourse,
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscoveryScreenPreview() {
    OpenEdXTheme {
        DiscoveryScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = DiscoveryUIState.Courses(
                listOf(
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                )
            ),
            uiMessage = null,
            apiHostUrl = "",
            onSearchClick = {},
            paginationCallback = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            canLoadMore = false,
            refreshing = false,
            hasInternetConnection = true,
            isUserLoggedIn = false,
            isRegistrationEnabled = true,
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun DiscoveryScreenTabletPreview() {
    OpenEdXTheme {
        DiscoveryScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            state = DiscoveryUIState.Courses(
                listOf(
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                    mockCourse,
                )
            ),
            uiMessage = null,
            apiHostUrl = "",
            onSearchClick = {},
            paginationCallback = {},
            onSwipeRefresh = {},
            onItemClick = {},
            onReloadClick = {},
            canLoadMore = false,
            refreshing = false,
            hasInternetConnection = true,
            isUserLoggedIn = true,
            isRegistrationEnabled = true,
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
            onSignInClick = {},
            onRegisterClick = {},
            onBackClick = {},
            onSettingsClick = {},
            canShowBackButton = false
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
