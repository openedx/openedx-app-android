package org.openedx.profile.presentation.profile.compose

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.presentation.profile.ProfileUIState
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic
import org.openedx.profile.presentation.ui.mockAccount

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ProfileView(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    onAction: (ProfileViewAction) -> Unit,
    onSettingsClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onAction(ProfileViewAction.SwipeRefresh) }
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

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Toolbar(
                label = stringResource(id = R.string.core_profile),
                canShowSettingsIcon = true,
                onSettingsClick = onSettingsClick
            )

            Surface(
                color = MaterialTheme.appColors.background
            ) {
                Box(
                    modifier = Modifier.pullRefresh(pullRefreshState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState) {
                            is ProfileUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is ProfileUIState.Data -> {
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
                                        subtitle = "@${uiState.account.username}"
                                    )
                                    ProfileInfoSection(uiState.account)
                                    OpenEdXOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        text = stringResource(id = org.openedx.profile.R.string.profile_edit_profile),
                                        onClick = {
                                            onAction(ProfileViewAction.EditAccountClick)
                                        },
                                        borderColor = MaterialTheme.appColors.primaryButtonBackground,
                                        textColor = MaterialTheme.appColors.textAccent
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
private fun ProfileScreenPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {},
            onSettingsClick = {},
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            onAction = {},
            onSettingsClick = {},
        )
    }
}

private val mockUiState = ProfileUIState.Data(
    account = mockAccount
)

internal interface ProfileViewAction {
    object EditAccountClick : ProfileViewAction
    object SwipeRefresh : ProfileViewAction
}
