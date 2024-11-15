package org.openedx.profile.presentation.manageaccount.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.presentation.manageaccount.ManageAccountUIState
import org.openedx.profile.presentation.ui.ProfileTopic
import org.openedx.profile.presentation.ui.mockAccount
import org.openedx.profile.R as ProfileR

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ManageAccountView(
    windowSize: WindowSize,
    uiState: ManageAccountUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    onAction: (ManageAccountViewAction) -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onAction(ManageAccountViewAction.SwipeRefresh) }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            )
        }

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .settingsHeaderBackground()
                .statusBarsInset(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                Toolbar(
                    modifier = topBarWidth
                        .displayCutoutForLandscape(),
                    label = stringResource(id = R.string.core_manage_account),
                    canShowBackBtn = true,
                    labelTint = MaterialTheme.appColors.settingsTitleContent,
                    iconTint = MaterialTheme.appColors.settingsTitleContent,
                    onBackClick = {
                        onAction(ManageAccountViewAction.BackClick)
                    }
                )
            }
            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape,
            ) {
                Box(
                    modifier = Modifier
                        .pullRefresh(pullRefreshState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .displayCutoutForLandscape(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState) {
                            is ManageAccountUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is ManageAccountUIState.Data -> {
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .then(contentWidth)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ProfileTopic(
                                        image = uiState.account.profileImage.imageUrlFull,
                                        title = uiState.account.name,
                                        subtitle = uiState.account.email ?: ""
                                    )
                                    OpenEdXOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        text = stringResource(id = ProfileR.string.profile_edit_profile),
                                        onClick = {
                                            onAction(ManageAccountViewAction.EditAccountClick)
                                        },
                                        borderColor = MaterialTheme.appColors.primaryButtonBackground,
                                        textColor = MaterialTheme.appColors.textAccent
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    IconText(
                                        text = stringResource(id = ProfileR.string.profile_delete_profile),
                                        painter = painterResource(id = ProfileR.drawable.profile_ic_trash),
                                        textStyle = MaterialTheme.appTypography.labelLarge,
                                        color = MaterialTheme.appColors.error,
                                        onClick = {
                                            onAction(ManageAccountViewAction.DeleteAccount)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManageAccountViewPreview() {
    OpenEdXTheme {
        ManageAccountView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ManageAccountViewTabletPreview() {
    OpenEdXTheme {
        ManageAccountView(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {}
        )
    }
}

private val mockUiState = ManageAccountUIState.Data(
    account = mockAccount
)

internal interface ManageAccountViewAction {
    object EditAccountClick : ManageAccountViewAction
    object SwipeRefresh : ManageAccountViewAction
    object DeleteAccount : ManageAccountViewAction
    object BackClick : ManageAccountViewAction
}
