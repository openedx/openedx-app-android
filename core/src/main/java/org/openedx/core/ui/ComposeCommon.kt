package org.openedx.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.openedx.core.NoContentScreenType
import org.openedx.core.R
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.tagId
import org.openedx.foundation.extension.toastMessage
import org.openedx.foundation.presentation.UIMessage

@Composable
fun StaticSearchBar(
    modifier: Modifier,
    text: String = stringResource(id = R.string.core_search),
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .testTag("tf_search")
            .then(
                Modifier
                    .background(
                        MaterialTheme.appColors.textFieldBackground,
                        MaterialTheme.appShapes.textFieldShape
                    )
                    .clip(MaterialTheme.appShapes.textFieldShape)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.textFieldBorder,
                        MaterialTheme.appShapes.textFieldShape
                    )
                    .clickable { onClick() }
                    .padding(horizontal = 20.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.appColors.textPrimary
        )
        Spacer(Modifier.width(10.dp))
        Box {
            Text(
                modifier = Modifier
                    .testTag("txt_search")
                    .fillMaxWidth(),
                text = text,
                color = MaterialTheme.appColors.textFieldHint
            )
        }
    }
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    label: String,
    canShowBackBtn: Boolean = false,
    canShowSettingsIcon: Boolean = false,
    labelTint: Color = MaterialTheme.appColors.textPrimary,
    iconTint: Color = MaterialTheme.appColors.textPrimary,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (canShowBackBtn) {
            BackBtn(
                tint = iconTint,
                onBackClick = onBackClick
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txt_toolbar_title")
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            text = label,
            color = labelTint,
            style = MaterialTheme.appTypography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (canShowSettingsIcon) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                onClick = { onSettingsClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    tint = MaterialTheme.appColors.textAccent,
                    contentDescription = stringResource(id = R.string.core_accessibility_settings)
                )
            }
        }
    }
}

@Composable
fun MainToolbar(
    modifier: Modifier = Modifier,
    label: String,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = label,
            color = MaterialTheme.appColors.textDark,
            style = MaterialTheme.appTypography.headlineBold
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            onClick = {
                onSettingsClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.ManageAccounts,
                tint = MaterialTheme.appColors.textAccent,
                contentDescription = stringResource(id = R.string.core_accessibility_settings)
            )
        }
    }
}

@Composable
fun SearchBar(
    modifier: Modifier,
    searchValue: TextFieldValue,
    requestFocus: Boolean = false,
    label: String = stringResource(id = R.string.core_search),
    clearOnSubmit: Boolean = false,
    keyboardActions: () -> Unit,
    onValueChanged: (TextFieldValue) -> Unit = {},
    onClearValue: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    var isFocused by rememberSaveable {
        mutableStateOf(false)
    }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(searchValue)
    }
    OutlinedTextField(
        modifier = Modifier
            .testTag("tf_search")
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.hasFocus
            }
            .clip(MaterialTheme.appShapes.textFieldShape)
            .then(modifier),
        shape = MaterialTheme.appShapes.textFieldShape,
        value = textFieldValue,
        onValueChange = {
            if (it.text != textFieldValue.text) {
                textFieldValue = it
                onValueChanged(textFieldValue)
            } else {
                textFieldValue = it
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.appColors.textPrimary,
            backgroundColor = if (isFocused) {
                MaterialTheme.appColors.background
            } else {
                MaterialTheme.appColors.textFieldBackground
            },
            focusedBorderColor = MaterialTheme.appColors.primary,
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            cursorColor = MaterialTheme.appColors.primary,
            leadingIconColor = MaterialTheme.appColors.textPrimary
        ),
        placeholder = {
            Text(
                modifier = Modifier
                    .testTag("txt_search_placeholder")
                    .fillMaxWidth(),
                text = label,
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(start = 16.dp),
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.appColors.primary else MaterialTheme.appColors.onSurface
            )
        },
        trailingIcon = {
            if (searchValue.text.isNotEmpty()) {
                IconButton(onClick = {
                    textFieldValue = TextFieldValue("")
                    onClearValue()
                }) {
                    Icon(
                        modifier = Modifier.padding(end = 16.dp),
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onSurface
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions {
            keyboardController?.hide()
            keyboardActions()
            if (clearOnSubmit) {
                textFieldValue = TextFieldValue("")
                onClearValue()
            }
        },
        textStyle = MaterialTheme.appTypography.bodyMedium,
        maxLines = 1
    )
}

@Composable
fun SearchBarStateless(
    modifier: Modifier,
    searchValue: String,
    requestFocus: Boolean = false,
    label: String = stringResource(id = R.string.core_search),
    keyboardActions: () -> Unit,
    onValueChanged: (String) -> Unit = {},
    onClearValue: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    var isFocused by rememberSaveable {
        mutableStateOf(false)
    }
    OutlinedTextField(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.hasFocus
            }
            .clip(MaterialTheme.appShapes.textFieldShape)
            .then(modifier),
        shape = MaterialTheme.appShapes.textFieldShape,
        value = searchValue,
        onValueChange = {
            if (it != searchValue) {
                onValueChanged(it)
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.appColors.textPrimary,
            backgroundColor = if (isFocused) {
                MaterialTheme.appColors.background
            } else {
                MaterialTheme.appColors.textFieldBackground
            },
            focusedBorderColor = MaterialTheme.appColors.primary,
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            cursorColor = MaterialTheme.appColors.primary,
            leadingIconColor = MaterialTheme.appColors.textPrimary
        ),
        placeholder = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(start = 16.dp),
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.appColors.primary else MaterialTheme.appColors.onSurface
            )
        },
        trailingIcon = {
            if (searchValue.isNotEmpty()) {
                IconButton(onClick = {
                    onClearValue()
                }) {
                    Icon(
                        modifier = Modifier.padding(end = 16.dp),
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onSurface
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions {
            keyboardController?.hide()
            keyboardActions()
        },
        textStyle = MaterialTheme.appTypography.bodyMedium,
        maxLines = 1
    )
}

@Composable
@NonRestartableComposable
fun HandleUIMessage(
    uiMessage: UIMessage?,
    scaffoldState: ScaffoldState,
) {
    val context = LocalContext.current
    LaunchedEffect(uiMessage) {
        when (uiMessage) {
            is UIMessage.SnackBarMessage -> {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = uiMessage.message,
                    duration = uiMessage.duration
                )
            }

            is UIMessage.ToastMessage -> {
                context.toastMessage(uiMessage.message)
            }

            else -> {}
        }
    }
}

@Composable
fun HyperlinkText(
    modifier: Modifier = Modifier,
    fullText: String,
    hyperLinks: Map<String, String>,
    textStyle: TextStyle = TextStyle.Default,
    linkTextColor: Color = MaterialTheme.appColors.primary,
    linkTextFontWeight: FontWeight = FontWeight.Normal,
    linkTextDecoration: TextDecoration = TextDecoration.None,
    fontSize: TextUnit = TextUnit.Unspecified,
    action: ((String) -> Unit)? = null,
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.appColors.textPrimaryLight,
                fontSize = fontSize
            ),
            start = 0,
            end = fullText.length
        )

        for ((key, value) in hyperLinks) {
            val startIndex = fullText.indexOf(key)
            val endIndex = startIndex + key.length
            addStyle(
                style = SpanStyle(
                    color = linkTextColor,
                    fontSize = fontSize,
                    fontWeight = linkTextFontWeight,
                    textDecoration = linkTextDecoration,
                ),
                start = startIndex,
                end = endIndex
            )
            addStringAnnotation(
                tag = "URL",
                annotation = value,
                start = startIndex,
                end = endIndex
            )
        }
        addStyle(
            style = SpanStyle(
                fontSize = fontSize
            ),
            start = 0,
            end = fullText.length
        )
    }

    val uriHandler = LocalUriHandler.current

    BasicText(
        text = annotatedString,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val position = offset.x.toInt()
                annotatedString.getStringAnnotations("URL", position, position)
                    .firstOrNull()?.let { stringAnnotation ->
                        action?.invoke(stringAnnotation.item)
                            ?: uriHandler.openUri(stringAnnotation.item)
                    }
            }
        },
        style = textStyle
    )
}

@Composable
fun SheetContent(
    searchValue: TextFieldValue,
    title: String = stringResource(id = R.string.core_select_value),
    expandedList: List<RegistrationField.Option>,
    onItemClick: (RegistrationField.Option) -> Unit,
    listState: LazyListState,
    searchValueChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .height(400.dp)
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.background)
    ) {
        Text(
            modifier = Modifier
                .testTag("txt_selection_title")
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.appTypography.titleMedium,
            text = title,
            color = MaterialTheme.appColors.onBackground
        )
        SearchBarStateless(
            modifier = Modifier
                .testTag("sb_search")
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            searchValue = searchValue.text,
            keyboardActions = {
                focusManager.clearFocus()
            },
            onValueChanged = { textField ->
                searchValueChanged(textField)
            },
            onClearValue = {
                searchValueChanged("")
            }
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            items(
                expandedList.filter {
                    it.name.startsWith(searchValue.text, true)
                }
            ) { item ->
                Text(
                    modifier = Modifier
                        .testTag("txt_${item.value.tagId()}_title")
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            onItemClick(item)
                        }
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.appColors.onBackground,
                    text = item.name,
                    style = MaterialTheme.appTypography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun SheetContent(
    searchValue: TextFieldValue,
    title: String = stringResource(id = R.string.core_select_value),
    expandedList: List<Pair<String, String>>,
    onItemClick: (Pair<String, String>) -> Unit,
    searchValueChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .height(400.dp)
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.background)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.appTypography.titleMedium,
            text = title
        )
        SearchBarStateless(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            searchValue = searchValue.text,
            keyboardActions = {
                focusManager.clearFocus()
            },
            onValueChanged = { textField ->
                searchValueChanged(textField)
            },
            onClearValue = {
                searchValueChanged("")
            }
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            Modifier.fillMaxWidth()
        ) {
            items(
                expandedList.filter {
                    it.first.startsWith(searchValue.text, true)
                }
            ) { item ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            onItemClick(item)
                        }
                        .padding(vertical = 12.dp),
                    text = item.first,
                    style = MaterialTheme.appTypography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun OpenEdXOutlinedTextField(
    modifier: Modifier,
    title: String,
    isSingleLine: Boolean = true,
    withRequiredMark: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: (FocusManager) -> Unit = { it.moveFocus(FocusDirection.Down) },
    onValueChanged: (String) -> Unit,
) {
    var inputFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }
    val focusManager = LocalFocusManager.current

    Column {
        Text(
            modifier = Modifier
                .testTag("txt_${title.tagId()}_label")
                .fillMaxWidth(),
            text = buildAnnotatedString {
                if (withRequiredMark) {
                    append(title)
                    withStyle(SpanStyle(color = MaterialTheme.appColors.error)) {
                        append("*")
                    }
                } else {
                    append(title)
                }
            },
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = inputFieldValue,
            onValueChange = {
                inputFieldValue = it
                onValueChanged(it.text)
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                textColor = MaterialTheme.appColors.textFieldText,
                backgroundColor = MaterialTheme.appColors.textFieldBackground,
                errorBorderColor = MaterialTheme.appColors.error,
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            placeholder = {
                Text(
                    text = title,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            visualTransformation = visualTransformation,
            keyboardActions = KeyboardActions {
                keyboardActions(focusManager)
            },
            textStyle = MaterialTheme.appTypography.bodyMedium,
            singleLine = isSingleLine,
            isError = !errorText.isNullOrEmpty(),
            modifier = modifier.testTag("tf_${title.tagId()}_input")
        )
        if (!errorText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                modifier = Modifier.testTag("txt_${title.tagId()}_error"),
                text = errorText,
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.error
            )
        }
    }
}

@Composable
fun AutoSizeText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    minSize: Float = 0f
) {
    var scaledTextStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text,
        modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = scaledTextStyle,
        color = color,
        softWrap = false,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && scaledTextStyle.fontSize.value > minSize) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun IconText(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.appTypography.bodySmall,
    onClick: (() -> Unit)? = null,
) {
    val modifierClickable = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .testTag("btn_${text.tagId()}")
            .noRippleClickable { onClick.invoke() }
    }
    Row(
        modifier = modifier.then(modifierClickable),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier
                .testTag("ic_${text.tagId()}")
                .size(size = (textStyle.fontSize.value + 4).dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            modifier = Modifier.testTag("txt_${text.tagId()}"),
            text = text,
            color = color,
            style = textStyle
        )
    }
}

@Composable
fun IconText(
    modifier: Modifier = Modifier,
    text: String,
    painter: Painter,
    color: Color,
    textStyle: TextStyle = MaterialTheme.appTypography.bodySmall,
    onClick: (() -> Unit)? = null,
) {
    val modifierClickable = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .testTag("btn_${text.tagId()}")
            .noRippleClickable { onClick.invoke() }
    }
    Row(
        modifier = modifier.then(modifierClickable),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier
                .testTag("ic_${text.tagId()}")
                .size(size = (textStyle.fontSize.value + 4).dp),
            painter = painter,
            contentDescription = null,
            tint = color
        )
        Text(
            modifier = Modifier.testTag("txt_${text.tagId()}"),
            text = text,
            color = color,
            style = textStyle
        )
    }
}

@Composable
fun TextIcon(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    color: Color,
    textStyle: TextStyle = MaterialTheme.appTypography.bodySmall,
    iconModifier: Modifier? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable { onClick.invoke() }
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = text, color = color, style = textStyle)
        Icon(
            modifier = iconModifier ?: Modifier.size(size = (textStyle.fontSize.value + 4).dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
    }
}

@Composable
fun TextIcon(
    iconModifier: Modifier = Modifier,
    text: String,
    painter: Painter,
    color: Color,
    textStyle: TextStyle = MaterialTheme.appTypography.bodySmall,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.noRippleClickable { onClick.invoke() }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = text, color = color, style = textStyle)
        Icon(
            modifier = iconModifier
                .size(size = (textStyle.fontSize.value + 4).dp),
            painter = painter,
            contentDescription = null,
            tint = color
        )
    }
}

@Composable
fun OfflineModeDialog(
    modifier: Modifier,
    onDismissCLick: () -> Unit,
    onReloadClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.appColors.warning
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = stringResource(id = R.string.core_offline),
                painter = painterResource(id = R.drawable.core_ic_offline),
                color = Color.Black,
                textStyle = MaterialTheme.appTypography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                IconButton(
                    modifier = Modifier.size(20.dp),
                    onClick = {
                        onReloadClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.core_ic_reload),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primary
                    )
                }
                IconButton(
                    modifier = Modifier.size(20.dp),
                    onClick = {
                        onDismissCLick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun OpenEdXButton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(42.dp),
    text: String = "",
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.primaryButtonText,
    backgroundColor: Color = MaterialTheme.appColors.primaryButtonBackground,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Button(
        modifier = Modifier
            .testTag("btn_${text.tagId()}")
            .then(modifier),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor
        ),
        enabled = enabled,
        onClick = onClick
    ) {
        if (content == null) {
            Text(
                modifier = Modifier.testTag("txt_${text.tagId()}"),
                text = text,
                color = textColor,
                style = MaterialTheme.appTypography.labelLarge
            )
        } else {
            content()
        }
    }
}

@Composable
fun OpenEdXOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    backgroundColor: Color = Color.Transparent,
    borderColor: Color,
    textColor: Color,
    text: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(
        modifier = Modifier
            .testTag("btn_${text.tagId()}")
            .then(modifier)
            .height(42.dp),
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor)
    ) {
        if (content == null) {
            Text(
                modifier = Modifier.testTag("txt_${text.tagId()}"),
                text = text,
                style = MaterialTheme.appTypography.labelLarge,
                color = textColor
            )
        } else {
            content()
        }
    }
}

@Composable
fun BackBtn(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.appColors.primary,
    onBackClick: () -> Unit,
) {
    IconButton(
        modifier = modifier.testTag("ib_back"),
        onClick = {
            onBackClick()
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.core_accessibility_btn_back),
            tint = tint
        )
    }
}

@Composable
fun ConnectionErrorView(onReloadClick: () -> Unit) {
    FullScreenErrorView(errorType = ErrorType.CONNECTION_ERROR, onReloadClick = onReloadClick)
}

@Composable
fun FullScreenErrorView(
    modifier: Modifier = Modifier,
    errorType: ErrorType,
    onReloadClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = errorType.iconResId),
            contentDescription = null,
            tint = MaterialTheme.appColors.onSurface
        )
        Spacer(Modifier.height(28.dp))
        Text(
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
            text = stringResource(id = errorType.titleResId),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
            text = stringResource(id = errorType.descriptionResId),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OpenEdXButton(
            modifier = Modifier
                .widthIn(Dp.Unspecified, 162.dp),
            text = stringResource(id = errorType.actionResId),
            textColor = MaterialTheme.appColors.secondaryButtonText,
            backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
            onClick = onReloadClick,
        )
    }
}

@Composable
fun NoContentScreen(noContentScreenType: NoContentScreenType) {
    NoContentScreen(
        message = stringResource(id = noContentScreenType.messageResId),
        icon = painterResource(id = noContentScreenType.iconResId)
    )
}

@Composable
fun NoContentScreen(message: String, icon: Painter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .sizeIn(
                    maxWidth = 80.dp,
                    maxHeight = 80.dp
                ),
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.appColors.progressBarBackgroundColor,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
            text = message,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AuthButtonsPanel(
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    showRegisterButton: Boolean,
) {
    Row {
        OpenEdXOutlinedButton(
            modifier = Modifier
                .testTag("btn_sign_in")
                .then(
                    if (showRegisterButton) {
                        Modifier
                            .width(100.dp)
                            .padding(end = 16.dp)
                    } else {
                        Modifier.weight(1f)
                    }
                ),
            text = stringResource(id = R.string.core_sign_in),
            onClick = { onSignInClick() },
            textColor = MaterialTheme.appColors.secondaryButtonBorderedText,
            backgroundColor = MaterialTheme.appColors.secondaryButtonBorderedBackground,
            borderColor = MaterialTheme.appColors.secondaryButtonBorder,
        )
        if (showRegisterButton) {
            OpenEdXButton(
                modifier = Modifier
                    .testTag("btn_register")
                    .width(0.dp)
                    .weight(1f),
                text = stringResource(id = R.string.core_register),
                textColor = MaterialTheme.appColors.primaryButtonText,
                backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
                onClick = { onRegisterClick() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoundTabsBar(
    modifier: Modifier = Modifier,
    items: List<TabItem>,
    pagerState: PagerState,
    contentPadding: PaddingValues = PaddingValues(),
    withPager: Boolean = false,
    rowState: LazyListState = rememberLazyListState(),
    onTabClicked: (Int) -> Unit = { }
) {
    // The pager state does not work without the pager and the tabs do not change.
    if (!withPager) {
        HorizontalPager(state = pagerState) { }
    }

    val scope = rememberCoroutineScope()
    LazyRow(
        modifier = modifier,
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = pagerState.currentPage == index
            val backgroundColor = if (isSelected) {
                MaterialTheme.appColors.primary
            } else {
                MaterialTheme.appColors.tabUnselectedBtnBackground
            }
            val contentColor = if (isSelected) {
                MaterialTheme.appColors.tabSelectedBtnContent
            } else {
                MaterialTheme.appColors.tabUnselectedBtnContent
            }
            val border = if (!isSystemInDarkTheme()) {
                Modifier.border(
                    1.dp,
                    MaterialTheme.appColors.primary,
                    CircleShape
                )
            } else {
                Modifier
            }

            RoundTab(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .then(border)
                    .clickable {
                        scope.launch {
                            onTabClicked(index)
                            pagerState.scrollToPage(index)
                            rowState.animateScrollToItem(index)
                        }
                    }
                    .padding(horizontal = 16.dp),
                item = item,
                contentColor = contentColor
            )
        }
    }
}

@Composable
fun CircularProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
    }
}

@Composable
private fun RoundTab(
    modifier: Modifier = Modifier,
    item: TabItem,
    contentColor: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val icon = item.icon
        if (icon != null) {
            Icon(
                painter = rememberVectorPainter(icon),
                tint = contentColor,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = stringResource(item.labelResId),
            color = contentColor
        )
    }
}

@Composable
fun OpenEdXDropdownMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        text = text,
        style = MaterialTheme.appTypography.labelLarge,
        color = MaterialTheme.appColors.textDark,
    )
}

@Preview
@Composable
private fun StaticSearchBarPreview() {
    StaticSearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

@Preview
@Composable
private fun SearchBarPreview() {
    SearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        searchValue = TextFieldValue(),
        keyboardActions = {},
        onClearValue = {}
    )
}

@Preview
@Composable
private fun ToolbarPreview() {
    Toolbar(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.background)
            .height(48.dp),
        label = "Toolbar",
        canShowBackBtn = true,
        onBackClick = {}
    )
}

@Preview
@Composable
private fun AuthButtonsPanelPreview() {
    AuthButtonsPanel(onRegisterClick = {}, onSignInClick = {}, showRegisterButton = true)
}

@Preview
@Composable
private fun OpenEdXOutlinedTextFieldPreview() {
    OpenEdXTheme(darkTheme = true) {
        OpenEdXOutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            title = "OpenEdXOutlinedTextField",
            onValueChanged = {},
            keyboardActions = {},
        )
    }
}

@Preview
@Composable
private fun IconTextPreview() {
    IconText(
        text = "IconText",
        icon = Icons.Filled.Close,
        color = MaterialTheme.appColors.primary
    )
}

@Preview
@Composable
private fun ConnectionErrorViewPreview() {
    OpenEdXTheme(darkTheme = true) {
        ConnectionErrorView(onReloadClick = {})
    }
}

val mockTab = object : TabItem {
    override val labelResId: Int = R.string.app_name
    override val icon: ImageVector = Icons.Default.AccountCircle
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun RoundTabsBarPreview() {
    OpenEdXTheme {
        RoundTabsBar(
            items = listOf(mockTab, mockTab, mockTab),
            rowState = rememberLazyListState(),
            pagerState = rememberPagerState(pageCount = { 3 }),
            onTabClicked = { }
        )
    }
}

@Preview
@Composable
private fun PreviewNoContentScreen() {
    OpenEdXTheme(darkTheme = true) {
        NoContentScreen(
            "No Content available",
            rememberVectorPainter(image = Icons.Filled.Info)
        )
    }
}
