package org.openedx.profile.presentation.profile

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.AppDataHolder
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.EmailUtil
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModel()
    private val router by inject<ProfileRouter>()

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
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val logoutSuccess by viewModel.successLogout.observeAsState(false)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)

                ProfileScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    appData = (requireActivity() as AppDataHolder).appData,
                    refreshing = refreshing,
                    logout = {
                        viewModel.logout()
                    },
                    editAccountClicked = {
                        viewModel.profileEditClickedEvent()
                        router.navigateToEditProfile(
                            requireParentFragment().parentFragmentManager,
                            it
                        )
                    },
                    onSwipeRefresh = {
                        viewModel.updateAccount()
                    },
                    onVideoSettingsClick = {
                        viewModel.profileVideoSettingsClickedEvent()
                        router.navigateToVideoSettings(
                            requireParentFragment().parentFragmentManager
                        )
                    },
                    onSupportClick = { action ->
                        when (action) {
                            SupportClickAction.SUPPORT -> viewModel.emailSupportClickedEvent()
                            SupportClickAction.COOKIE_POLICY -> viewModel.cookiePolicyClickedEvent()
                            SupportClickAction.PRIVACY_POLICY -> viewModel.privacyPolicyClickedEvent()
                        }
                    }
                )

                LaunchedEffect(logoutSuccess) {
                    if (logoutSuccess) {
                        router.restartApp(requireParentFragment().parentFragmentManager)
                    }
                }
            }
        }
    }
}

private enum class SupportClickAction {
    SUPPORT, PRIVACY_POLICY, COOKIE_POLICY
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ProfileScreen(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    appData: AppData,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    onVideoSettingsClick: () -> Unit,
    logout: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onSupportClick: (SupportClickAction) -> Unit,
    editAccountClicked: (Account) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

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
                    logout()
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
                            editAccountClicked(uiState.account)
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
                                        onVideoSettingsClick()
                                    })

                                    Spacer(modifier = Modifier.height(24.dp))

                                    SupportInfoSection(appData, onClick = onSupportClick)

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
fun SettingsSection(onVideoSettingsClick: () -> Unit) {
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
    appData: AppData,
    onClick: (SupportClickAction) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileInfoItem(
                    text = stringResource(id = org.openedx.profile.R.string.profile_contact_support),
                    onClick = {
                        onClick(SupportClickAction.SUPPORT)
                        EmailUtil.showFeedbackScreen(
                            context,
                            context.getString(R.string.core_email_subject),
                            appData.versionName
                        )
                    }
                )
                Divider(color = MaterialTheme.appColors.divider)
                ProfileInfoItem(
                    text = stringResource(id = R.string.core_terms_of_use),
                    onClick = {
                        onClick(SupportClickAction.COOKIE_POLICY)
                        uriHandler.openUri(context.getString(R.string.terms_of_service_link))
                    }
                )
                Divider(color = MaterialTheme.appColors.divider)
                ProfileInfoItem(
                    text = stringResource(id = R.string.core_privacy_policy),
                    onClick = {
                        onClick(SupportClickAction.PRIVACY_POLICY)
                        uriHandler.openUri(context.getString(R.string.privacy_policy_link))
                    }
                )
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
private fun ProfileInfoItem(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun LogoutDialogPreview() {
    LogoutDialog({}, {})
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    OpenEdXTheme {
        ProfileScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = ProfileUIState.Data(mockAccount),
            uiMessage = null,
            refreshing = false,
            logout = {},
            onSwipeRefresh = {},
            editAccountClicked = {},
            onVideoSettingsClick = {},
            onSupportClick = {},
            appData = AppData("1")
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    OpenEdXTheme {
        ProfileScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = ProfileUIState.Data(mockAccount),
            uiMessage = null,
            refreshing = false,
            logout = {},
            onSwipeRefresh = {},
            editAccountClicked = {},
            onVideoSettingsClick = {},
            onSupportClick = {},
            appData = AppData("1")
        )
    }
}

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
