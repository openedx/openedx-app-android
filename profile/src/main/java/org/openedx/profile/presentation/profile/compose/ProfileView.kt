package org.openedx.profile.presentation.profile.compose

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.AgreementUrls
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.profile.ProfileUIState
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ProfileView(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    appUpgradeEvent: AppUpgradeEvent?,
    onAction: (ProfileViewAction) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onAction(ProfileViewAction.SwipeRefresh) })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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

        if (showLogoutDialog) {
            LogoutDialog(
                onDismissRequest = {
                    showLogoutDialog = false
                },
                onLogoutClick = {
                    showLogoutDialog = false
                    onAction(ProfileViewAction.LogoutClick)
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = topBarWidth,
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.core_profile),
                    color = MaterialTheme.appColors.textPrimary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.appTypography.titleMedium
                )

                IconText(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(end = 24.dp),
                    text = stringResource(org.openedx.profile.R.string.profile_edit),
                    painter = painterResource(id = R.drawable.core_ic_edit),
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.primary,
                    onClick = {
                        if (uiState is ProfileUIState.Data) {
                            onAction(ProfileViewAction.EditAccountClick)
                        }
                    }
                )
            }
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
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ProfileTopic(uiState.account)

                                    Spacer(modifier = Modifier.height(36.dp))

                                    ProfileInfoSection(uiState.account)

                                    Spacer(modifier = Modifier.height(24.dp))

                                    SettingsSection(onVideoSettingsClick = {
                                        onAction(ProfileViewAction.VideoSettingsClick)
                                    })

                                    Spacer(modifier = Modifier.height(24.dp))

                                    SupportInfoSection(
                                        uiState = uiState,
                                        onAction = onAction,
                                        appUpgradeEvent = appUpgradeEvent,
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    LogoutButton(
                                        onClick = { showLogoutDialog = true }
                                    )

                                    Spacer(Modifier.height(30.dp))
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

@Composable
private fun SettingsSection(onVideoSettingsClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(id = org.openedx.profile.R.string.profile_settings),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier,
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(Modifier.fillMaxWidth()) {
                ProfileInfoItem(
                    text = stringResource(id = org.openedx.profile.R.string.profile_video_settings),
                    onClick = onVideoSettingsClick
                )
            }
        }
    }
}

@Composable
private fun SupportInfoSection(
    uiState: ProfileUIState.Data,
    appUpgradeEvent: AppUpgradeEvent?,
    onAction: (ProfileViewAction) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = org.openedx.profile.R.string.profile_support_info),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier,
            shape = MaterialTheme.appShapes.cardShape,
            elevation = 0.dp,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (uiState.supportEmail.isNotBlank()) {
                    ProfileInfoItem(text = stringResource(id = org.openedx.profile.R.string.profile_contact_support)) {
                        onAction(ProfileViewAction.SupportClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                if (uiState.agreementUrls.tosUrl.isNotBlank()) {
                    ProfileInfoItem(text = stringResource(id = R.string.core_terms_of_use)) {
                        onAction(ProfileViewAction.TermsClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                if (uiState.agreementUrls.privacyPolicyUrl.isNotBlank()) {
                    ProfileInfoItem(text = stringResource(id = R.string.core_privacy_policy)) {
                        onAction(ProfileViewAction.PrivacyPolicyClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                if (uiState.agreementUrls.cookiePolicyUrl.isNotBlank()) {
                    ProfileInfoItem(text = stringResource(id = R.string.core_cookie_policy)) {
                        onAction(ProfileViewAction.CookiePolicyClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                if (uiState.agreementUrls.dataSellConsentUrl.isNotBlank()) {
                    ProfileInfoItem(text = stringResource(id = R.string.core_data_sell)) {
                        onAction(ProfileViewAction.DataSellClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                if (uiState.faqUrl.isNotBlank()) {
                    val uriHandler = LocalUriHandler.current
                    ProfileInfoItem(
                        text = stringResource(id = R.string.core_faq),
                        external = true,
                    ) {
                        uriHandler.openUri(uiState.faqUrl)
                        onAction(ProfileViewAction.FaqClick)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.appColors.divider
                    )
                }
                AppVersionItem(
                    versionName = uiState.versionName,
                    appUpgradeEvent = appUpgradeEvent,
                ) {
                    onAction(ProfileViewAction.AppVersionClick)
                }
            }
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = MaterialTheme.appShapes.cardShape,
        elevation = 0.dp,
        backgroundColor = MaterialTheme.appColors.cardViewBackground
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = org.openedx.profile.R.string.profile_logout),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.error
            )
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.appColors.error
            )
        }
    }
}

@Composable
private fun LogoutDialog(
    onDismissRequest: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.appColors.background,
                        MaterialTheme.appShapes.cardShape
                    )
                    .clip(MaterialTheme.appShapes.cardShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.cardViewBorder,
                        MaterialTheme.appShapes.cardShape
                    )
                    .padding(horizontal = 40.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = onDismissRequest
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.core_cancel),
                            tint = MaterialTheme.appColors.primary
                        )
                    }
                }
                Icon(
                    modifier = Modifier
                        .width(88.dp)
                        .height(85.dp),
                    painter = painterResource(org.openedx.profile.R.drawable.profile_ic_exit),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.onBackground
                )
                Spacer(Modifier.size(36.dp))
                Text(
                    text = stringResource(id = org.openedx.profile.R.string.profile_logout_dialog_body),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(36.dp))
                OpenEdXButton(
                    text = stringResource(id = org.openedx.profile.R.string.profile_logout),
                    backgroundColor = MaterialTheme.appColors.warning,
                    onClick = onLogoutClick,
                    content = {
                        Box(
                            Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = org.openedx.profile.R.string.profile_logout),
                                color = MaterialTheme.appColors.textDark,
                                style = MaterialTheme.appTypography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                painter = painterResource(id = org.openedx.profile.R.drawable.profile_ic_logout),
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun ProfileInfoItem(
    text: String,
    external: Boolean = false,
    onClick: () -> Unit
) {
    val icon = if (external) {
        Icons.Filled.OpenInNew
    } else {
        Icons.Filled.ArrowForwardIos
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null
        )
    }
}

@Composable
private fun AppVersionItem(
    versionName: String,
    appUpgradeEvent: AppUpgradeEvent?,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(20.dp)) {
        when (appUpgradeEvent) {
            is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                AppVersionItemUpgradeRecommended(
                    versionName = versionName,
                    appUpgradeEvent = appUpgradeEvent,
                    onClick = onClick
                )
            }

            is AppUpgradeEvent.UpgradeRequiredEvent -> {
                AppVersionItemUpgradeRequired(
                    versionName = versionName,
                    onClick = onClick
                )
            }

            else -> {
                AppVersionItemAppToDate(
                    versionName = versionName
                )
            }
        }
    }
}

@Composable
private fun AppVersionItemAppToDate(versionName: String) {
    Column(
        Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.core_version, versionName),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(
                    (MaterialTheme.appTypography.labelLarge.fontSize.value + 4).dp
                ),
                painter = painterResource(id = R.drawable.core_ic_check),
                contentDescription = null,
                tint = MaterialTheme.appColors.accessGreen
            )
            Text(
                text = stringResource(id = R.string.core_up_to_date),
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
private fun AppVersionItemUpgradeRecommended(
    versionName: String,
    appUpgradeEvent: AppUpgradeEvent.UpgradeRecommendedEvent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.core_version, versionName),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary
            )
            Text(
                text = stringResource(
                    id = R.string.core_tap_to_update_to_version,
                    appUpgradeEvent.newVersionName
                ),
                color = MaterialTheme.appColors.textAccent,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
            tint = MaterialTheme.appColors.primary,
            contentDescription = null
        )
    }
}

@Composable
fun AppVersionItemUpgradeRequired(
    versionName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
                    modifier = Modifier
                        .size((MaterialTheme.appTypography.labelLarge.fontSize.value + 8).dp),
                    painter = painterResource(id = R.drawable.core_ic_warning),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.core_version, versionName),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary
                )
            }
            Text(
                text = stringResource(id = R.string.core_tap_to_install_required_app_update),
                color = MaterialTheme.appColors.textAccent,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(id = R.drawable.core_ic_icon_upgrade),
            tint = MaterialTheme.appColors.primary,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun AppVersionItemAppToDatePreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = null,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun AppVersionItemUpgradeRecommendedPreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = AppUpgradeEvent.UpgradeRecommendedEvent("1.0.1"),
            onClick = {}
        )
    }
}

@Preview
@Composable
fun AppVersionItemUpgradeRequiredPreview() {
    OpenEdXTheme {
        AppVersionItem(
            versionName = mockAppData.versionName,
            appUpgradeEvent = AppUpgradeEvent.UpgradeRequiredEvent,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun LogoutDialogPreview() {
    LogoutDialog({}, {})
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
            appUpgradeEvent = null,
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
            appUpgradeEvent = null,
        )
    }
}

private val mockAppData = AppData(
    versionName = "1.0.0",
)

private val mockAccount = Account(
    username = "thom84",
    bio = "He as compliment unreserved projecting. Between had observe pretend delight for believe. Do newspaper questions consulted sweetness do. Our sportsman his unwilling fulfilled departure law.",
    requiresParentalConsent = true,
    name = "Thomas",
    country = "Ukraine",
    isActive = true,
    profileImage = ProfileImage("", "", "", "", false),
    yearOfBirth = 2000,
    levelOfEducation = "Bachelor",
    goals = "130",
    languageProficiencies = emptyList(),
    gender = "male",
    mailingAddress = "",
    "",
    null,
    accountPrivacy = Account.Privacy.ALL_USERS
)

private val mockUiState = ProfileUIState.Data(
    account = mockAccount,
    agreementUrls = AgreementUrls(),
    faqUrl = "https://example.com/faq",
    supportEmail = "test@example.com",
    versionName = mockAppData.versionName,
)

internal interface ProfileViewAction {
    object AppVersionClick : ProfileViewAction
    object EditAccountClick : ProfileViewAction
    object LogoutClick : ProfileViewAction
    object PrivacyPolicyClick : ProfileViewAction
    object CookiePolicyClick : ProfileViewAction
    object DataSellClick : ProfileViewAction
    object FaqClick : ProfileViewAction
    object TermsClick : ProfileViewAction
    object SupportClick : ProfileViewAction
    object VideoSettingsClick : ProfileViewAction
    object SwipeRefresh : ProfileViewAction
}
