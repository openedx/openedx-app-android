@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

package com.raccoongang.auth.presentation.signup

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.raccoongang.auth.presentation.AuthRouter
import com.raccoongang.auth.presentation.ui.ExpandableText
import com.raccoongang.auth.presentation.ui.OptionalFields
import com.raccoongang.auth.presentation.ui.RequiredFields
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.domain.model.RegistrationFieldType
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignUpFragment : Fragment() {

    private val viewModel by viewModel<SignUpViewModel>()
    private val router by inject<AuthRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getRegistrationFields()
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
                val uiMessage by viewModel.uiMessage.observeAsState()
                val isButtonClicked by viewModel.isButtonLoading.observeAsState(false)
                val successLogin by viewModel.successLogin.observeAsState()
                val validationError by viewModel.validationError.observeAsState(false)

                RegistrationScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    uiMessage = uiMessage,
                    isButtonClicked = isButtonClicked,
                    validationError,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onRegisterClick = { map ->
                        viewModel.register(map.mapValues { it.value ?: "" })
                    }
                )

                LaunchedEffect(successLogin) {
                    if (successLogin == true) {
                        router.navigateToMain(parentFragmentManager)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun RegistrationScreen(
    windowSize: WindowSize,
    uiState: SignUpUIState,
    uiMessage: UIMessage?,
    isButtonClicked: Boolean,
    validationError: Boolean,
    onBackClick: () -> Unit,
    onRegisterClick: (Map<String, String?>) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
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
    val mapFields = rememberSaveableMap {
        mutableStateMapOf<String, String?>()
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

    LaunchedEffect(validationError) {
        if (validationError) {
            coroutine.launch {
                scrollState.animateScrollTo(0, tween(300))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        mapFields[serverFieldName.value] = item.value
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
                    .fillMaxHeight(0.3f),
                painter = painterResource(id = R.drawable.core_top_header),
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
                            .fillMaxWidth(),
                        text = stringResource(id = com.raccoongang.auth.R.string.auth_register),
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
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState) {
                            is SignUpUIState.Loading -> {
                                Box(
                                    Modifier
                                        .fillMaxSize(), contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }
                            is SignUpUIState.Fields -> {
                                mapFields.let {
                                    if (it.isEmpty()) {
                                        it.putAll(uiState.fields.associate { it.name to "" })
                                        it["honor_code"] = true.toString()
                                    }
                                }
                                Column(
                                    Modifier
                                        .fillMaxHeight()
                                        .verticalScroll(scrollState)
                                        .then(contentPaddings),
                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Column() {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = stringResource(id = com.raccoongang.auth.R.string.auth_sign_up),
                                            color = MaterialTheme.appColors.textPrimary,
                                            style = MaterialTheme.appTypography.displaySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            text = stringResource(id = com.raccoongang.auth.R.string.auth_create_new_account),
                                            color = MaterialTheme.appColors.textPrimary,
                                            style = MaterialTheme.appTypography.titleSmall
                                        )
                                    }
                                    RequiredFields(
                                        fields = uiState.fields,
                                        mapFields = mapFields,
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
                                        }
                                    )
                                    if (uiState.optionalFields.isNotEmpty()) {
                                        ExpandableText(isExpanded = showOptionalFields, onClick = {
                                            showOptionalFields = !showOptionalFields
                                        })
                                        Surface(color = MaterialTheme.appColors.background) {
                                            AnimatedVisibility(visible = showOptionalFields) {
                                                OptionalFields(
                                                    fields = uiState.optionalFields,
                                                    mapFields = mapFields,
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
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (isButtonClicked) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(42.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                        }
                                    } else {
                                        NewEdxButton(
                                            width = buttonWidth,
                                            text = stringResource(id = com.raccoongang.auth.R.string.auth_create_account),
                                            onClick = {
                                                showErrorMap.clear()
                                                onRegisterClick(mapFields.toMap())
                                            }
                                        )
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
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrationScreenPreview() {
    NewEdxTheme {
        RegistrationScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = SignUpUIState.Fields(
                fields = listOf(field, field, field),
                optionalFields = listOf(
                    field
                )
            ),
            uiMessage = null,
            isButtonClicked = false,
            validationError = false,
            onBackClick = {},
            onRegisterClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegistrationScreenTabletPreview() {
    NewEdxTheme {
        RegistrationScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = SignUpUIState.Fields(
                fields = listOf(field, field, field),
                optionalFields = listOf(
                    field
                )
            ),
            uiMessage = null,
            isButtonClicked = false,
            validationError = false,
            onBackClick = {},
            onRegisterClick = {}
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