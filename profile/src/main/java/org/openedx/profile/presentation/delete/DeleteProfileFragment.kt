package org.openedx.profile.presentation.delete

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.R
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedTextField
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
import org.openedx.foundation.presentation.rememberWindowSize
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.presentation.settings.SettingsViewModel
import org.openedx.profile.R as profileR

class DeleteProfileFragment : Fragment() {

    private val viewModel by viewModel<DeleteProfileViewModel>()
    private val logoutViewModel by viewModel<SettingsViewModel>()
    private val router by inject<ProfileRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(logoutViewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(DeleteProfileFragmentUIState.Initial)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val logoutSuccess by logoutViewModel.successLogout.collectAsState(false)

                DeleteProfileScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onDeleteClick = {
                        viewModel.deleteProfile(it)
                    }
                )

                LaunchedEffect(logoutSuccess) {
                    if (logoutSuccess) {
                        router.restartApp(
                            requireActivity().supportFragmentManager,
                            logoutViewModel.isLogistrationEnabled
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeleteProfileScreen(
    windowSize: WindowSize,
    uiState: DeleteProfileFragmentUIState,
    uiMessage: UIMessage?,
    onDeleteClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()

    val errorText = if (uiState is DeleteProfileFragmentUIState.Error) {
        uiState.message
    } else {
        null
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState
    ) { paddingValues ->

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
                )
            )
        }

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

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .settingsHeaderBackground()
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Toolbar(
                    modifier = topBarWidth
                        .displayCutoutForLandscape(),
                    label = stringResource(id = profileR.string.profile_delete_account),
                    labelTint = MaterialTheme.appColors.settingsTitleContent,
                    iconTint = MaterialTheme.appColors.settingsTitleContent,
                    canShowBackBtn = true,
                    onBackClick = onBackClick
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.appShapes.screenBackgroundShape)
                        .background(MaterialTheme.appColors.background)
                        .displayCutoutForLandscape()
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = contentWidth
                    ) {
                        Spacer(Modifier.height(48.dp))
                        Image(
                            modifier = Modifier
                                .size(145.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = profileR.drawable.profile_delete_box),
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            modifier = Modifier
                                .testTag("txt_delete_account_title")
                                .fillMaxWidth(),
                            text = buildAnnotatedString {
                                append(stringResource(id = profileR.string.profile_you_want_to))
                                append(" ")
                                append(stringResource(id = profileR.string.profile_delete_your_account))
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.appColors.textPrimary
                                    ),
                                    start = 0,
                                    end = stringResource(id = profileR.string.profile_you_want_to).length
                                )
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.appColors.error
                                    ),
                                    start = stringResource(id = profileR.string.profile_you_want_to).length + 1,
                                    end = this.length
                                )
                            },
                            style = MaterialTheme.appTypography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            modifier = Modifier
                                .testTag("txt_delete_account_description")
                                .fillMaxWidth(),
                            text = stringResource(id = profileR.string.profile_confirm_action),
                            style = MaterialTheme.appTypography.labelLarge,
                            color = MaterialTheme.appColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(40.dp))
                        OpenEdXOutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            title = stringResource(id = R.string.core_password),
                            onValueChanged = {
                                password = it
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            keyboardActions = {
                                it.clearFocus()
                                onDeleteClick(password)
                            },
                            errorText = errorText
                        )
                        Spacer(Modifier.height(38.dp))
                        OpenEdXButton(
                            text = stringResource(id = profileR.string.profile_yes_delete_account),
                            enabled = uiState !is DeleteProfileFragmentUIState.Loading && password.isNotEmpty(),
                            backgroundColor = MaterialTheme.appColors.error,
                            onClick = {
                                onDeleteClick(password)
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(
    name = "PIXEL_3A_Light",
    device = Devices.PIXEL_3A,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "PIXEL_3A_Dark",
    device = Devices.PIXEL_3A,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DeleteProfileScreenPreview() {
    OpenEdXTheme {
        DeleteProfileScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = DeleteProfileFragmentUIState.Initial,
            uiMessage = null,
            onBackClick = {},
            onDeleteClick = {}
        )
    }
}
