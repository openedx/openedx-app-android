package org.openedx.auth.presentation.signin.compose

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.auth.presentation.signin.AuthEvent
import org.openedx.auth.presentation.signin.SignInUIState
import org.openedx.auth.presentation.ui.LoginTextField
import org.openedx.auth.presentation.ui.PasswordVisibilityIcon
import org.openedx.auth.presentation.ui.SocialAuthView
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.HyperlinkText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.theme.compose.SignInLogoView
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LoginScreen(
    windowSize: WindowSize,
    state: SignInUIState,
    uiMessage: UIMessage?,
    onEvent: (AuthEvent) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .semantics {
                testTagsAsResourceId = true
            }
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
                .fillMaxHeight(fraction = 0.3f),
            painter = painterResource(id = coreR.drawable.core_top_header),
            contentScale = ContentScale.FillBounds,
            contentDescription = null
        )
        HandleUIMessage(
            uiMessage = uiMessage,
            scaffoldState = scaffoldState
        )
        if (state.isLogistrationEnabled) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                BackBtn(
                    modifier = Modifier.padding(end = 16.dp),
                    tint = Color.White
                ) {
                    onEvent(AuthEvent.BackClick)
                }
            }
        }
        Column(
            Modifier.padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignInLogoView()
            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.background)
                            .verticalScroll(scrollState)
                            .displayCutoutForLandscape()
                            .then(contentPaddings),
                    ) {
                        Text(
                            modifier = Modifier.testTag("txt_sign_in_title"),
                            text = stringResource(id = coreR.string.core_sign_in),
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.displaySmall
                        )
                        Text(
                            modifier = Modifier
                                .testTag("txt_sign_in_description")
                                .padding(top = 4.dp),
                            text = stringResource(id = R.string.auth_welcome_back),
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AuthForm(
                            buttonWidth,
                            state,
                            onEvent,
                        )
                        state.agreement?.let {
                            Spacer(modifier = Modifier.height(24.dp))
                            val linkedText =
                                TextConverter.htmlTextToLinkedText(state.agreement.label)
                            HyperlinkText(
                                modifier = Modifier.testTag("txt_${state.agreement.name}"),
                                fullText = linkedText.text,
                                hyperLinks = linkedText.links,
                                linkTextColor = MaterialTheme.appColors.textHyperLink,
                                linkTextDecoration = TextDecoration.Underline,
                                action = { link ->
                                    onEvent(AuthEvent.OpenLink(linkedText.links, link))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthForm(
    buttonWidth: Modifier,
    state: SignInUIState,
    onEvent: (AuthEvent) -> Unit,
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEmailError by rememberSaveable { mutableStateOf(false) }
    var isPasswordError by rememberSaveable { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!state.isBrowserLoginEnabled) {
            LoginTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                title = stringResource(id = R.string.auth_email_username),
                description = stringResource(id = R.string.auth_enter_email_username),
                onValueChanged = {
                    login = it
                    isEmailError = false
                },
                isError = isEmailError,
                errorMessages = stringResource(id = R.string.auth_error_empty_username_email)
            )

            Spacer(modifier = Modifier.height(18.dp))
            PasswordTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                onValueChanged = {
                    password = it
                    isPasswordError = false
                },
                onPressDone = {
                    keyboardController?.hide()
                    if (password.isNotEmpty()) {
                        onEvent(AuthEvent.SignIn(login = login, password = password))
                    } else {
                        isEmailError = login.isEmpty()
                        isPasswordError = password.isEmpty()
                    }
                },
                isError = isPasswordError,
            )
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 36.dp)
        ) {
            if (!state.isBrowserLoginEnabled) {
                if (state.isLogistrationEnabled.not() && state.isRegistrationEnabled) {
                    Text(
                        modifier = Modifier
                            .testTag("txt_register")
                            .noRippleClickable {
                                onEvent(AuthEvent.RegisterClick)
                            },
                        text = stringResource(id = coreR.string.core_register),
                        color = MaterialTheme.appColors.primary,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .testTag("txt_forgot_password")
                        .noRippleClickable {
                            onEvent(AuthEvent.ForgotPasswordClick)
                        },
                    text = stringResource(id = R.string.auth_forgot_password),
                    color = MaterialTheme.appColors.infoVariant,
                    style = MaterialTheme.appTypography.labelLarge
                )
            }
        }

        if (state.showProgress) {
            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
        } else {
            OpenEdXButton(
                modifier = buttonWidth.testTag("btn_sign_in"),
                text = stringResource(id = coreR.string.core_sign_in),
                textColor = MaterialTheme.appColors.primaryButtonText,
                backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
                onClick = {
                    if (state.isBrowserLoginEnabled) {
                        onEvent(AuthEvent.SignInBrowser)
                    } else {
                        keyboardController?.hide()
                        if (login.isNotEmpty() && password.isNotEmpty()) {
                            onEvent(AuthEvent.SignIn(login = login, password = password))
                        } else {
                            isEmailError = login.isEmpty()
                            isPasswordError = password.isEmpty()
                        }
                    }
                }
            )
        }
        if (state.isSocialAuthEnabled) {
            SocialAuthView(
                modifier = buttonWidth,
                isGoogleAuthEnabled = state.isGoogleAuthEnabled,
                isFacebookAuthEnabled = state.isFacebookAuthEnabled,
                isMicrosoftAuthEnabled = state.isMicrosoftAuthEnabled,
                isSignIn = true,
            ) {
                keyboardController?.hide()
                onEvent(AuthEvent.SocialSignIn(it))
            }
        }
    }
}

@Composable
private fun PasswordTextField(
    modifier: Modifier = Modifier,
    isError: Boolean,
    onValueChanged: (String) -> Unit,
    onPressDone: () -> Unit,
) {
    var passwordTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Text(
        modifier = Modifier
            .testTag("txt_password_label")
            .fillMaxWidth(),
        text = stringResource(id = coreR.string.core_password),
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.labelLarge
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        modifier = modifier.testTag("tf_password"),
        value = passwordTextFieldValue,
        onValueChange = {
            passwordTextFieldValue = it
            onValueChanged(it.text.trim())
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.appColors.textFieldText,
            backgroundColor = MaterialTheme.appColors.textFieldBackground,
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            cursorColor = MaterialTheme.appColors.textFieldText,
        ),
        shape = MaterialTheme.appShapes.textFieldShape,
        placeholder = {
            Text(
                modifier = Modifier.testTag("txt_password_placeholder"),
                text = stringResource(id = R.string.auth_enter_password),
                color = MaterialTheme.appColors.textFieldHint,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        trailingIcon = {
            PasswordVisibilityIcon(
                isPasswordVisible = isPasswordVisible,
                onClick = { isPasswordVisible = !isPasswordVisible }
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        visualTransformation = if (isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardActions = KeyboardActions {
            focusManager.clearFocus()
            onPressDone()
        },
        isError = isError,
        textStyle = MaterialTheme.appTypography.bodyMedium,
        singleLine = true,
    )
    if (isError) {
        Text(
            modifier = Modifier
                .testTag("txt_password_error")
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = stringResource(id = R.string.auth_error_empty_password),
            style = MaterialTheme.appTypography.bodySmall,
            color = MaterialTheme.appColors.error,
        )
    }
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
            state = SignInUIState(),
            uiMessage = null,
            onEvent = {},
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SignInUsingBrowserScreenPreview() {
    OpenEdXTheme {
        LoginScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = SignInUIState().copy(
                isBrowserLoginEnabled = true,
            ),
            uiMessage = null,
            onEvent = {},
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
            state = SignInUIState().copy(
                isSocialAuthEnabled = true,
                isFacebookAuthEnabled = true,
                isGoogleAuthEnabled = true,
                isMicrosoftAuthEnabled = true,
            ),
            uiMessage = null,
            onEvent = {},
        )
    }
}
