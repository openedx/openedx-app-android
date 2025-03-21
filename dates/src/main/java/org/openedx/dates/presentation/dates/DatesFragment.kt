package org.openedx.dates.presentation.dates

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.presentation.dates.CourseDateBlockSection
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.MainScreenTitle
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.shouldLoadMore
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.dates.R
import org.openedx.dates.presentation.dates.DatesFragment.Companion.LOAD_MORE_THRESHOLD
import org.openedx.foundation.extension.isNotEmptyThenLet
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue

class DatesFragment : Fragment() {

    private val viewModel by viewModel<DatesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                DatesScreen(
                    uiState = uiState,
                    uiMessage = uiMessage,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    useRelativeDates = viewModel.useRelativeDates,
                    onAction = { action ->
                        when (action) {
                            DatesViewActions.OpenSettings -> {
                                viewModel.onSettingsClick(requireActivity().supportFragmentManager)
                            }

                            DatesViewActions.SwipeRefresh -> {
                                viewModel.refreshData()
                            }

                            DatesViewActions.LoadMore -> {
                                viewModel.fetchMore()
                            }

                            DatesViewActions.ShiftDueDate -> {
                                viewModel.shiftDueDate()
                            }

                            is DatesViewActions.OpenEvent -> {
                                viewModel.navigateToCourseOutline(
                                    requireActivity().supportFragmentManager,
                                    action.date
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val LOAD_MORE_THRESHOLD = 4
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DatesScreen(
    uiState: DatesUIState,
    uiMessage: UIMessage?,
    hasInternetConnection: Boolean,
    useRelativeDates: Boolean,
    onAction: (DatesViewActions) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val windowSize = rememberWindowSize()
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { onAction(DatesViewActions.SwipeRefresh) }
    )
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollState = rememberLazyListState()
    val firstVisibleIndex = remember {
        mutableIntStateOf(scrollState.firstVisibleItemIndex)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background,
        topBar = {
            MainScreenTitle(
                modifier = Modifier
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                label = stringResource(id = R.string.dates),
                onSettingsClick = {
                    onAction(DatesViewActions.OpenSettings)
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                if (uiState.isLoading && uiState.dates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                    }
                } else if (uiState.dates.isEmpty()) {
                    EmptyState()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutForLandscape()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = contentWidth.fillMaxSize(),
                            state = scrollState,
                            contentPadding = PaddingValues(bottom = 48.dp)
                        ) {
                            uiState.dates.keys.forEach { sectionKey ->
                                val dates = uiState.dates[sectionKey].orEmpty()
                                dates.isNotEmptyThenLet { sectionDates ->
                                    val isHavePastRelatedDates =
                                        sectionKey == DatesSection.PAST_DUE && dates.any { it.relative }
                                    if (isHavePastRelatedDates) {
                                        item {
                                            ShiftDueDatesCard(
                                                modifier = Modifier.padding(top = 12.dp),
                                                isButtonEnabled = !uiState.isShiftDueDatesPressed,
                                                onClick = {
                                                    onAction(DatesViewActions.ShiftDueDate)
                                                }
                                            )
                                        }
                                    }
                                    item {
                                        CourseDateBlockSection(
                                            sectionKey = sectionKey,
                                            sectionDates = sectionDates,
                                            onItemClick = {
                                                onAction(DatesViewActions.OpenEvent(it))
                                            },
                                            useRelativeDates = useRelativeDates
                                        )
                                    }
                                }
                            }
                            if (uiState.canLoadMore) {
                                item {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                    }
                                }
                            }
                        }
                        if (scrollState.shouldLoadMore(firstVisibleIndex, LOAD_MORE_THRESHOLD)) {
                            onAction(DatesViewActions.LoadMore)
                        }
                    }
                }

                HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

                PullRefreshIndicator(
                    uiState.isRefreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )

                if (!isInternetConnectionShown && !hasInternetConnection) {
                    OfflineModeDialog(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        onDismissCLick = {
                            isInternetConnectionShown = true
                        },
                        onReloadClick = {
                            isInternetConnectionShown = true
                            onAction(DatesViewActions.SwipeRefresh)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ShiftDueDatesCard(
    modifier: Modifier = Modifier,
    isButtonEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.appColors.cardViewBackground,
        shape = MaterialTheme.appShapes.cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.dates_shift_due_date_card_title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.dates_shift_due_date_card_description),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelLarge,
            )
            OpenEdXButton(
                text = stringResource(id = R.string.dates_shift_due_date),
                enabled = isButtonEnabled,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = org.openedx.core.R.drawable.core_ic_book),
                tint = MaterialTheme.appColors.textFieldBorder,
                contentDescription = null
            )
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_title")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dates_empty_state_title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dates_empty_state_description),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun DatesScreenPreview() {
    OpenEdXTheme {
        DatesScreen(
            uiState = DatesUIState(isLoading = false),
            uiMessage = null,
            hasInternetConnection = true,
            useRelativeDates = true,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ShiftDueDatesCardPreview() {
    OpenEdXTheme {
        ShiftDueDatesCard(
            isButtonEnabled = true,
            onClick = {}
        )
    }
}
