package org.openedx.dates.presentation.dates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.presentation.dates.CourseDateBlockSection
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.MainScreenToolbar
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.displayCutoutForLandscape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesScreen(
    uiState: DatesUIState,
    uiMessage: UIMessage?,
    hasInternetConnection: Boolean,
    useRelativeDates: Boolean,
    onAction: (DatesViewActions) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSize = rememberWindowSize()
    val contentWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }
    val pullToRefreshState = rememberPullToRefreshState()
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollState = rememberLazyListState()
    val layoutInfo by remember { derivedStateOf { scrollState.layoutInfo } }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.appColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainScreenToolbar(
                modifier = Modifier
                    .statusBarsInset()
                    .displayCutoutForLandscape(),
                label = stringResource(id = R.string.dates_title),
                onSettingsClick = {
                    onAction(DatesViewActions.OpenSettings)
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onAction(DatesViewActions.SwipeRefresh) },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
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
                                contentPadding = PaddingValues(bottom = 48.dp, top = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                uiState.dates.keys.forEach { sectionKey ->
                                    val dates = uiState.dates[sectionKey].orEmpty()
                                    dates.isNotEmptyThenLet { sectionDates ->
                                        val isHavePastRelatedDates =
                                            sectionKey == DatesSection.PAST_DUE && dates.any { it.relative }
                                        if (isHavePastRelatedDates) {
                                            item {
                                                ShiftDueDatesCard(
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
                            val lastVisibleItemIndex =
                                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItemsCount = layoutInfo.totalItemsCount
                            val shouldLoadMore = totalItemsCount > 0 &&
                                    lastVisibleItemIndex >= (totalItemsCount * LOAD_MORE_THRESHOLD).toInt()
                            LaunchedEffect(shouldLoadMore) {
                                if (shouldLoadMore) {
                                    onAction(DatesViewActions.LoadMore)
                                }
                            }
                        }
                    }
                }

                HandleUIMessage(uiMessage = uiMessage, snackbarHostState = snackbarHostState)

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.cardViewBackground),
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
                modifier = Modifier.size(100.dp),
                imageVector = Icons.Outlined.CalendarMonth,
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
