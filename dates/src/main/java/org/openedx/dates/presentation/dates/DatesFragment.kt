package org.openedx.dates.presentation.dates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import org.openedx.core.presentation.ListItemPosition
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.MainScreenTitle
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.dates.R
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
                    onAction = { action ->
                        when (action) {
                            DatesViewActions.OpenSettings -> {
                                viewModel.onSettingsClick(requireActivity().supportFragmentManager)
                            }

                            DatesViewActions.SwipeRefresh -> {
                                viewModel.refreshData()
                            }

                            is DatesViewActions.OpenEvent -> {

                            }
                        }
                    }
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DatesScreen(
    uiState: DatesUIState,
    uiMessage: UIMessage?,
    hasInternetConnection: Boolean,
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
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                            modifier = contentWidth,
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            uiState.dates.keys.forEach { dueDateCategory ->
                                val dates = uiState.dates[dueDateCategory] ?: emptyList()
                                if (dates.isNotEmpty()) {
                                    item {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp, top = 20.dp),
                                            text = stringResource(id = dueDateCategory.label),
                                            color = MaterialTheme.appColors.textDark,
                                            style = MaterialTheme.appTypography.titleMedium,
                                        )
                                    }
                                    itemsIndexed(dates) { index, date ->
                                        val itemPosition =
                                            ListItemPosition.detectPosition(index, dates)
                                        DateItem(
                                            date = date,
                                            lineColor = dueDateCategory.color,
                                            itemPosition = itemPosition,
                                            onClick = {
                                                onAction(DatesViewActions.OpenEvent())
                                            }
                                        )
                                    }
                                }
                            }
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
private fun DateItem(
    modifier: Modifier = Modifier,
    date: String,
    lineColor: Color,
    itemPosition: ListItemPosition,
    onClick: () -> Unit,
) {
    val boxCornerWidth = 8.dp
    val boxCornerRadius = boxCornerWidth / 2
    val infoPadding = 8.dp

    val boxCornerShape = remember(itemPosition) {
        when (itemPosition) {
            ListItemPosition.SINGLE -> RoundedCornerShape(boxCornerRadius)
            ListItemPosition.MIDDLE -> RectangleShape
            ListItemPosition.FIRST -> RoundedCornerShape(
                topStart = boxCornerRadius,
                topEnd = boxCornerRadius
            )

            ListItemPosition.LAST -> RoundedCornerShape(
                bottomStart = boxCornerRadius,
                bottomEnd = boxCornerRadius
            )
        }
    }

    val infoPaddingModifier = remember(itemPosition) {
        when (itemPosition) {
            ListItemPosition.SINGLE -> Modifier
            ListItemPosition.FIRST -> Modifier.padding(bottom = infoPadding)
            ListItemPosition.LAST -> Modifier.padding(top = infoPadding)
            ListItemPosition.MIDDLE -> Modifier.padding(vertical = infoPadding)
        }
    }

    val arrowOffset = remember(itemPosition) {
        when (itemPosition) {
            ListItemPosition.FIRST -> Modifier.padding(bottom = infoPadding)
            ListItemPosition.LAST -> Modifier.padding(top = infoPadding)
            else -> Modifier
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored line box
        Box(
            modifier = Modifier
                .width(boxCornerWidth)
                .fillMaxHeight()
                .background(color = lineColor, shape = boxCornerShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .then(infoPaddingModifier),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textDark
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = org.openedx.core.R.drawable.core_ic_assignment),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = date,
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textDark
                )
            }
            Text(
                text = date,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.appColors.textDark,
            modifier = arrowOffset.size(16.dp)
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
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
            onAction = {}
        )
    }
}
