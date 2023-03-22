package com.raccoongang.profile.presentation.profile

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExitToApp
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
import coil.compose.rememberAsyncImagePainter
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.domain.model.ProfileImage
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.profile.presentation.ProfileRouter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

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
            NewEdxTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val logoutSuccess by viewModel.successLogout.observeAsState(false)
                val uiMessage by viewModel.uiMessage.observeAsState()
                ProfileScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    logout = {
                        viewModel.logout()
                    },
                    editAccountClicked = {
                        router.navigateToEditProfile(
                            requireParentFragment().parentFragmentManager,
                            it
                        )
                    },
                    onVideoSettingsClick = {
                        router.navigateToVideoSettings(
                            requireParentFragment().parentFragmentManager
                        )
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


@Composable
private fun ProfileScreen(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    uiMessage: UIMessage?,
    onVideoSettingsClick: () -> Unit,
    logout: () -> Unit,
    editAccountClicked: (Account) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

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
                .statusBarsInset(),
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

                IconButton(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    onClick = {
                        if (uiState is ProfileUIState.Data) {
                            editAccountClicked(uiState.account)
                        }
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.core_ic_edit),
                        tint = MaterialTheme.appColors.onBackground,
                        contentDescription = null
                    )
                }
            }
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
                            CircularProgressIndicator()
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
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = uiState.account.profileImage.imageUrlFull,
                                    placeholder = painterResource(id = R.drawable.core_ic_default_profile_picture),
                                    error = painterResource(id = R.drawable.core_ic_default_profile_picture)
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .border(
                                        2.dp,
                                        MaterialTheme.appColors.onSurface,
                                        CircleShape
                                    )
                                    .padding(2.dp)
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = uiState.account.name,
                                color = MaterialTheme.appColors.textPrimary,
                                style = MaterialTheme.appTypography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@${uiState.account.username}",
                                color = MaterialTheme.appColors.textPrimaryVariant,
                                style = MaterialTheme.appTypography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(36.dp))

                            Column(
                                Modifier
                                    .fillMaxWidth()
                            ) {
                                ProfileInfoSection(uiState.account)

                                Spacer(modifier = Modifier.height(24.dp))

                                SettingsSection(onVideoSettingsClick = {
                                    onVideoSettingsClick()
                                })

                                Spacer(modifier = Modifier.height(24.dp))

                                SupportInfoSection()

                                Spacer(modifier = Modifier.height(24.dp))

                                LogoutButton(
                                    onClick = { showLogoutDialog = true }
                                )

                                Spacer(Modifier.height(30.dp))
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoSection(account: Account) {
    Column {
        Text(
            text = stringResource(id = com.raccoongang.profile.R.string.profile_prof_info),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                MaterialTheme.appShapes.cardShape
            ),
            shape = MaterialTheme.appShapes.cardShape,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        id = com.raccoongang.profile.R.string.profile_year_of_birth,
                        if (account.yearOfBirth != null) {
                            account.yearOfBirth.toString()
                        } else ""
                    ),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = stringResource(
                        id = com.raccoongang.profile.R.string.profile_bio,
                        account.bio
                    ),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary
                )
            }
        }
    }
}

@Composable
fun SettingsSection(onVideoSettingsClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(id = com.raccoongang.profile.R.string.profile_settings),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                MaterialTheme.appShapes.cardShape
            ),
            shape = MaterialTheme.appShapes.cardShape,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileInfoItem(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_video_settings),
                    onClick = onVideoSettingsClick
                )
            }
        }
    }
}

@Composable
private fun SupportInfoSection() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    Column {
        Text(
            text = stringResource(id = com.raccoongang.profile.R.string.profile_support_info),
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                MaterialTheme.appShapes.cardShape
            ),
            shape = MaterialTheme.appShapes.cardShape,
            backgroundColor = MaterialTheme.appColors.cardViewBackground
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileInfoItem(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_contact_support),
                    onClick = {
                        uriHandler.openUri(context.getString(R.string.contact_us_link))
                    }
                )
                ProfileInfoItem(
                    text = stringResource(id = R.string.core_terms_of_use),
                    onClick = {
                        uriHandler.openUri(context.getString(R.string.terms_of_service_link))
                    }
                )
                ProfileInfoItem(
                    text = stringResource(id = R.string.core_privacy_policy),
                    onClick = {
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
            .border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                MaterialTheme.appShapes.cardShape
            )
            .clickable {
                onClick()
            },
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.cardViewBackground
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = com.raccoongang.profile.R.string.profile_logout),
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
                    .padding(horizontal = 40.dp)
                    .padding(top = 48.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier
                        .width(88.dp)
                        .height(85.dp),
                    painter = painterResource(com.raccoongang.profile.R.drawable.profile_ic_exit),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.onBackground
                )
                Spacer(Modifier.size(40.dp))
                Text(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_logout_dialog_body),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(44.dp))
                NewEdxButton(
                    text = stringResource(id = com.raccoongang.profile.R.string.profile_logout),
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
                                text = stringResource(id = com.raccoongang.profile.R.string.profile_logout),
                                color = MaterialTheme.appColors.textDark,
                                style = MaterialTheme.appTypography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                painter = painterResource(id = com.raccoongang.profile.R.drawable.profile_ic_logout),
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

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    NewEdxTheme {
        ProfileScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = ProfileUIState.Data(mockAccount),
            uiMessage = null,
            logout = {},
            editAccountClicked = {},
            onVideoSettingsClick = {}
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    NewEdxTheme {
        ProfileScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = ProfileUIState.Data(mockAccount),
            uiMessage = null,
            logout = {},
            editAccountClicked = {},
            onVideoSettingsClick = {}
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
