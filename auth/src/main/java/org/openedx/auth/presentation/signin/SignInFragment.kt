package org.openedx.auth.presentation.signin

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.openedx.auth.R
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.ui.LoginTextField
import org.openedx.core.UIMessage
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModel()
    private val router: AuthRouter by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val showProgress by viewModel.showProgress.observeAsState(initial = false)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val loginSuccess by viewModel.loginSuccess.observeAsState(initial = false)

                LoginScreen(
                    windowSize = windowSize,
                    showProgress = showProgress,
                    uiMessage = uiMessage,
                    onLoginClick = { login, password ->
                        viewModel.login(login, password)
                    },
                    onRegisterClick = {
                        viewModel.signUpClickedEvent()
                        router.navigateToSignUp(parentFragmentManager)
                    },
                    onForgotPasswordClick = {
                        viewModel.forgotPasswordClickedEvent()
                        router.navigateToRestorePassword(parentFragmentManager)
                    }
                )

                LaunchedEffect(loginSuccess) {
                    if (loginSuccess) {
                        router.navigateToMain(parentFragmentManager)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    windowSize: WindowSize,
    showProgress: Boolean,
    uiMessage: UIMessage?,
    onLoginClick: (login: String, password: String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) {

        val contentPaddings by remember {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .widthIn(Dp.Unspecified, 420.dp)
                        .padding(
                            top = 32.dp,
                            bottom = 40.dp
                        ),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                )
            )
        }
        val buttonWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(232.dp, Dp.Unspecified),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f),
            painter = painterResource(id = org.openedx.core.R.drawable.core_top_header),
            contentScale = ContentScale.FillBounds,
            contentDescription = null
        )
        HandleUIMessage(
            uiMessage = uiMessage,
            scaffoldState = scaffoldState
        )

        Column(
            Modifier.padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = org.openedx.core.R.drawable.core_ic_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .width(170.dp)
                        .height(48.dp)
                )
            }
            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.background)
                            .then(contentPaddings),
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_sign_in),
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.displaySmall
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = stringResource(id = R.string.auth_welcome_back),
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AuthForm(
                            buttonWidth,
                            showProgress,
                            onLoginClick,
                            onRegisterClick,
                            onForgotPasswordClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthForm(
    buttonWidth: Modifier,
    isLoading: Boolean = false,
    onLoginClick: (login: String, password: String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoginTextField(
            modifier = Modifier
                .fillMaxWidth(),
            onValueChanged = {
                login = it
            })

        Spacer(modifier = Modifier.height(18.dp))
        PasswordTextField(
            modifier = Modifier
                .fillMaxWidth(),
            onValueChanged = {
                password = it
            },
            onPressDone = {
                onLoginClick(login, password)
            }
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 36.dp)
        ) {
            Text(
                modifier = Modifier.noRippleClickable {
                    onRegisterClick()
                },
                text = stringResource(id = R.string.auth_register),
                color = MaterialTheme.appColors.primary,
                style = MaterialTheme.appTypography.labelLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.noRippleClickable {
                    onForgotPasswordClick()
                },
                text = stringResource(id = R.string.auth_forgot_password),
                color = MaterialTheme.appColors.primary,
                style = MaterialTheme.appTypography.labelLarge
            )
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
        } else {
            OpenEdXButton(
                width = buttonWidth,
                text = stringResource(id = R.string.auth_sign_in),
                onClick = {
                    onLoginClick(login, password)
                }
            )
        }
    }
}


@Composable
private fun PasswordTextField(
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit,
    onPressDone: () -> Unit,
) {
    var passwordTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }
    val focusManager = LocalFocusManager.current
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = org.openedx.core.R.string.core_password),
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.labelLarge
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        modifier = modifier,
        value = passwordTextFieldValue,
        onValueChange = {
            passwordTextFieldValue = it
            onValueChanged(it.text.trim())
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            backgroundColor = MaterialTheme.appColors.textFieldBackground
        ),
        shape = MaterialTheme.appShapes.textFieldShape,
        placeholder = {
            Text(
                text = stringResource(id = R.string.auth_enter_password),
                color = MaterialTheme.appColors.textFieldHint,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        visualTransformation = PasswordVisualTransformation(),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus()
            onPressDone()
        },
        textStyle = MaterialTheme.appTypography.bodyMedium,
        singleLine = true
    )
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SignInScreenPreview() {
    OpenEdXTheme {
        LoginScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            showProgress = false,
            uiMessage = null,
            onLoginClick = { _, _ ->

            },
            onRegisterClick = {},
            onForgotPasswordClick = {}
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Night", device = Devices.NEXUS_9, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SignInScreenTabletPreview() {
    OpenEdXTheme {
        LoginScreen(
            windowSize = WindowSize(WindowType.Expanded, WindowType.Expanded),
            showProgress = false,
            uiMessage = null,
            onLoginClick = { _, _ ->

            },
            onRegisterClick = {},
            onForgotPasswordClick = {}
        )
    }
}