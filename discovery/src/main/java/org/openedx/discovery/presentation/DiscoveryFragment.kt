package org.openedx.discovery.presentation

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.AppUpdateState
import org.openedx.core.AppUpdateState.wasUpdateDialogClosed
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Course
import org.openedx.core.domain.model.Media
import org.openedx.core.presentation.dialog.appupgrade.AppUpgradeDialogFragment
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRecommendedBox
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.discovery.R

class DiscoveryFragment : Fragment() {

    private val viewModel by viewModel<DiscoveryViewModel>()
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

                DiscoveryScreen(
                    windowSize = windowSize,
                    state = uiState!!,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    canLoadMore = canLoadMore,
                    refreshing = refreshing,
                    hasInternetConnection = viewModel.hasInternetConnection,
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
                            requireActivity().supportFragmentManager
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
                        router.navigateToCourseDetail(
                            requireParentFragment().parentFragmentManager,
                            course.id
                        )
                    })
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun DiscoveryScreen(
    windowSize: WindowSize,
    state: DiscoveryUIState,
    uiMessage: UIMessage?,
    apiHostUrl: String,
    canLoadMore: Boolean,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    appUpgradeParameters: AppUpdateState.AppUpgradeParameters,
    onSearchClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onReloadClick: () -> Unit,
    paginationCallback: () -> Unit,
    onItemClick: (Course) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableStateOf(scrollState.firstVisibleItemIndex)
    }
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
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
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.discovery_Discovery),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                StaticSearchBar(
                    modifier = Modifier
                        .height(48.dp)
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
                                Modifier
                                    .fillMaxSize(), contentAlignment = Alignment.Center
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
                                                text = stringResource(id = R.string.discovery_discovery_new),
                                                color = MaterialTheme.appColors.textPrimary,
                                                style = MaterialTheme.appTypography.displaySmall
                                            )
                                            Text(
                                                modifier = Modifier.padding(top = 4.dp),
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
                                            onClick = { courseId ->
                                                onItemClick(course)
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
                                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                            }
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
            onClick = {})
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
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters()
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
            appUpgradeParameters = AppUpdateState.AppUpgradeParameters()
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