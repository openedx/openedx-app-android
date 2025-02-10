package org.openedx.auth.presentation.signup.compose

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.openedx.auth.R
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.presentation.signup.SignUpUIState
import org.openedx.auth.presentation.ui.ExpandableText
import org.openedx.auth.presentation.ui.OptionalFields
import org.openedx.auth.presentation.ui.RequiredFields
import org.openedx.auth.presentation.ui.SocialAuthView
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.SheetContent
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.isImeVisibleState
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.rememberSaveableMap
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SignUpView(
    windowSize: WindowSize,
    uiState: SignUpUIState,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit,
    onFieldUpdated: (String, String) -> Unit,
    onRegisterClick: (authType: AuthType) -> Unit,
    onHyperLinkClick: (Map<String, String>, String) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val focusManager = LocalFocusManager.current
    val bottomSheetScaffoldState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val coroutine = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var expandedList by rememberSaveable {
        mutableStateOf(emptyList<RegistrationField.Option>())
    }
    val selectableNamesMap = rememberSaveableMap {
        mutableStateMapOf<String, String?>()
    }
    val serverFieldName = rememberSaveable {
        mutableStateOf("")
    }
    var showOptionalFields by rememberSaveable {
        mutableStateOf(false)
    }
    val showErrorMap = rememberSaveableMap {
        mutableStateMapOf<String, Boolean?>()
    }
    val scrollState = rememberScrollState()

    val haptic = LocalHapticFeedback.current

    val listState = rememberLazyListState()

    var bottomDialogTitle by rememberSaveable {
        mutableStateOf("")
    }

    var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val isImeVisible by isImeVisibleState()

    LaunchedEffect(uiState.validationError) {
        if (uiState.validationError) {
            coroutine.launch {
                scrollState.animateScrollTo(0, tween(durationMillis = 300))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    LaunchedEffect(uiState.socialAuth) {
        if (uiState.socialAuth != null) {
            coroutine.launch {
                showErrorMap.clear()
                scrollState.animateScrollTo(0, tween(durationMillis = 300))
            }
        }
    }

    LaunchedEffect(bottomSheetScaffoldState.isVisible) {
        if (!bottomSheetScaffoldState.isVisible) {
            focusManager.clearFocus()
            searchValue = TextFieldValue("")
        }
    }

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
        val topBarPadding by remember {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .width(560.dp)
                        .padding(bottom = 24.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            )
        }
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

        ModalBottomSheetLayout(
            modifier = Modifier
                .padding(bottom = if (isImeVisible && bottomSheetScaffoldState.isVisible) 120.dp else 0.dp)
                .noRippleClickable {
                    if (bottomSheetScaffoldState.isVisible) {
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                        }
                    }
                },
            sheetState = bottomSheetScaffoldState,
            sheetShape = MaterialTheme.appShapes.screenBackgroundShape,
            scrimColor = Color.Black.copy(alpha = 0.4f),
            sheetBackgroundColor = MaterialTheme.appColors.background,
            sheetContent = {
                SheetContent(
                    title = bottomDialogTitle,
                    searchValue = searchValue,
                    expandedList = expandedList,
                    listState = listState,
                    onItemClick = { item ->
                        onFieldUpdated(serverFieldName.value, item.value)
                        selectableNamesMap[serverFieldName.value] = item.name
                        coroutine.launch {
                            bottomSheetScaffoldState.hide()
                        }
                    },
                    searchValueChanged = {
                        searchValue = TextFieldValue(it)
                    }
                )
            }
        ) {
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(it)
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .then(topBarPadding),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier
                            .testTag("txt_screen_title")
                            .fillMaxWidth(),
                        text = stringResource(id = coreR.string.core_register),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    BackBtn(
                        modifier = Modifier.padding(end = 16.dp),
                        tint = Color.White
                    ) {
                        onBackClick()
                    }
                }
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.appColors.background),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        } else {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(scrollState)
                                    .displayCutoutForLandscape()
                                    .then(contentPaddings),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column {
                                    if (uiState.socialAuth != null) {
                                        SocialSignedView(uiState.socialAuth.authType)
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            text = stringResource(
                                                id = R.string.auth_compete_registration
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.appColors.textPrimary,
                                            style = MaterialTheme.appTypography.titleSmall
                                        )
                                    } else {
                                        Text(
                                            modifier = Modifier
                                                .testTag("txt_sign_up_title")
                                                .fillMaxWidth(),
                                            text = stringResource(id = coreR.string.core_register),
                                            color = MaterialTheme.appColors.textPrimary,
                                            style = MaterialTheme.appTypography.displaySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .testTag("txt_sign_up_description")
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            text = stringResource(
                                                id = R.string.auth_create_new_account
                                            ),
                                            color = MaterialTheme.appColors.textPrimary,
                                            style = MaterialTheme.appTypography.titleSmall
                                        )
                                    }
                                }
                                RequiredFields(
                                    fields = uiState.requiredFields,
                                    showErrorMap = showErrorMap,
                                    selectableNamesMap = selectableNamesMap,
                                    onSelectClick = { serverName, field, list ->
                                        keyboardController?.hide()
                                        serverFieldName.value = serverName
                                        expandedList = list
                                        coroutine.launch {
                                            if (bottomSheetScaffoldState.isVisible) {
                                                bottomSheetScaffoldState.hide()
                                            } else {
                                                bottomDialogTitle = field.label
                                                showErrorMap[field.name] = false
                                                bottomSheetScaffoldState.show()
                                            }
                                        }
                                    },
                                    onFieldUpdated = onFieldUpdated
                                )
                                if (uiState.optionalFields.isNotEmpty()) {
                                    ExpandableText(
                                        modifier = Modifier.testTag("txt_optional_field"),
                                        isExpanded = showOptionalFields,
                                        onClick = {
                                            showOptionalFields = !showOptionalFields
                                        }
                                    )
                                    AnimatedVisibility(visible = showOptionalFields) {
                                        OptionalFields(
                                            fields = uiState.optionalFields,
                                            showErrorMap = showErrorMap,
                                            selectableNamesMap = selectableNamesMap,
                                            onSelectClick = { serverName, field, list ->
                                                keyboardController?.hide()
                                                serverFieldName.value =
                                                    serverName
                                                expandedList = list
                                                coroutine.launch {
                                                    if (bottomSheetScaffoldState.isVisible) {
                                                        bottomSheetScaffoldState.hide()
                                                    } else {
                                                        bottomDialogTitle = field.label
                                                        showErrorMap[field.name] = false
                                                        bottomSheetScaffoldState.show()
                                                    }
                                                }
                                            },
                                            onFieldUpdated = onFieldUpdated,
                                        )
                                    }
                                }
                                if (uiState.agreementFields.isNotEmpty()) {
                                    OptionalFields(
                                        fields = uiState.agreementFields,
                                        showErrorMap = showErrorMap,
                                        selectableNamesMap = selectableNamesMap,
                                        onSelectClick = { serverName, field, list ->
                                            keyboardController?.hide()
                                            serverFieldName.value = serverName
                                            expandedList = list
                                            coroutine.launch {
                                                if (bottomSheetScaffoldState.isVisible) {
                                                    bottomSheetScaffoldState.hide()
                                                } else {
                                                    bottomDialogTitle = field.label
                                                    showErrorMap[field.name] = false
                                                    bottomSheetScaffoldState.show()
                                                }
                                            }
                                        },
                                        onFieldUpdated = onFieldUpdated,
                                        hyperLinkAction = { links, link ->
                                            onHyperLinkClick(links, link)
                                        },
                                    )
                                }

                                if (uiState.isButtonLoading) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                    }
                                } else {
                                    OpenEdXButton(
                                        modifier = buttonWidth.testTag("btn_create_account"),
                                        text = stringResource(id = R.string.auth_create_account),
                                        textColor = MaterialTheme.appColors.primaryButtonText,
                                        backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
                                        onClick = {
                                            keyboardController?.hide()
                                            showErrorMap.clear()
                                            onRegisterClick(AuthType.PASSWORD)
                                        }
                                    )
                                }
                                if (uiState.isSocialAuthEnabled && uiState.socialAuth == null) {
                                    SocialAuthView(
                                        modifier = buttonWidth,
                                        isGoogleAuthEnabled = uiState.isGoogleAuthEnabled,
                                        isFacebookAuthEnabled = uiState.isFacebookAuthEnabled,
                                        isMicrosoftAuthEnabled = uiState.isMicrosoftAuthEnabled,
                                        isSignIn = false,
                                    ) {
                                        keyboardController?.hide()
                                        onRegisterClick(it)
                                    }
                                }
                                Spacer(Modifier.height(70.dp))
                            }
                        }
                    }
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
private fun RegistrationScreenPreview() {
    OpenEdXTheme {
        SignUpView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = SignUpUIState(
                allFields = listOf(field),
                requiredFields = listOf(field, field),
                optionalFields = listOf(field, field),
                agreementFields = listOf(field),
            ),
            uiMessage = null,
            onBackClick = {},
            onRegisterClick = {},
            onFieldUpdated = { _, _ -> },
            onHyperLinkClick = { _, _ -> },
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrationScreenTabletPreview() {
    OpenEdXTheme {
        SignUpView(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = SignUpUIState(
                allFields = listOf(field),
                requiredFields = listOf(field, field),
                optionalFields = listOf(field, field),
                agreementFields = listOf(field),
            ),
            uiMessage = null,
            onBackClick = {},
            onRegisterClick = {},
            onFieldUpdated = { _, _ -> },
            onHyperLinkClick = { _, _ -> },
        )
    }
}

private val option = RegistrationField.Option("def", "Bachelor", "Android")

private val field = RegistrationField(
    "Fullname",
    "Fullname",
    RegistrationFieldType.TEXT,
    "Fullname",
    instructions = "Enter your fullname",
    exposed = false,
    required = true,
    restrictions = RegistrationField.Restrictions(),
    options = listOf(option, option),
    errorInstructions = ""
)
