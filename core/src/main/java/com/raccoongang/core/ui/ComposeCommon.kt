package com.raccoongang.core.ui

import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.extension.LinkedImageText
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography

@Composable
fun StaticSearchBar(
    modifier: Modifier,
    text: String = stringResource(id = R.string.core_search),
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.then(Modifier
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
            .padding(horizontal = 20.dp)),
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
                modifier = Modifier.fillMaxWidth(),
                text = text,
                color = MaterialTheme.appColors.textFieldHint
            )
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchBar(
    modifier: Modifier,
    searchValue: TextFieldValue,
    requestFocus: Boolean = false,
    label: String = stringResource(id = R.string.core_search),
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
            backgroundColor = if (isFocused) MaterialTheme.appColors.background else MaterialTheme.appColors.textFieldBackground,
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
                val message = uiMessage.message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.appColors.textPrimary,
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
                    textDecoration = linkTextDecoration
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

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = textStyle,
        onClick = {
            annotatedString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        }
    )
}

@Composable
fun HyperlinkImageText(
    modifier: Modifier = Modifier,
    title: String = "",
    imageText: LinkedImageText,
    textStyle: TextStyle = TextStyle.Default,
    linkTextColor: Color = MaterialTheme.appColors.primary,
    linkTextFontWeight: FontWeight = FontWeight.Normal,
    linkTextDecoration: TextDecoration = TextDecoration.None,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val fullText = imageText.text
    val hyperLinks = imageText.links
    val annotatedString = buildAnnotatedString {
        if(title.isNotEmpty()) {
            append(title)
            append("\n\n")
        }
        append(fullText)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.appColors.textPrimary,
                fontSize = fontSize
            ),
            start = 0,
            end = this.length
        )

        for ((key, value) in hyperLinks) {
            val startIndex = this.toString().indexOf(key)
            if (startIndex == -1) continue
            val endIndex = startIndex + key.length
            addStyle(
                style = SpanStyle(
                    color = linkTextColor,
                    fontSize = fontSize,
                    fontWeight = linkTextFontWeight,
                    textDecoration = linkTextDecoration
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
        if (title.isNotEmpty()) {
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.appColors.textPrimary,
                    fontSize = MaterialTheme.appTypography.titleLarge.fontSize,
                    fontWeight = MaterialTheme.appTypography.titleLarge.fontWeight
                ),
                start = 0,
                end = title.length
            )
        }
        for (item in imageText.headers) {
            val startIndex = this.toString().indexOf(item)
            if (startIndex == -1) continue
            val endIndex = startIndex + item.length
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.appColors.textPrimary,
                    fontSize = MaterialTheme.appTypography.titleLarge.fontSize,
                    fontWeight = MaterialTheme.appTypography.titleLarge.fontWeight
                ),
                start = startIndex,
                end = endIndex
            )
        }
        addStyle(
            style = SpanStyle(
                fontSize = fontSize
            ),
            start = 0,
            end = this.length
        )
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Column(Modifier.fillMaxWidth()) {
        ClickableText(
            modifier = modifier,
            text = annotatedString,
            style = textStyle,
            onClick = {
                annotatedString
                    .getStringAnnotations("URL", it, it)
                    .firstOrNull()?.let { stringAnnotation ->
                        uriHandler.openUri(stringAnnotation.item)
                    }
            }
        )
        imageText.imageLinks.values.forEach {
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(0.dp, 360.dp),
                contentScale = ContentScale.Fit,
                model = it,
                contentDescription = null,
                imageLoader = imageLoader
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SheetContent(
    expandedList: List<RegistrationField.Option>,
    onItemClick: (RegistrationField.Option) -> Unit,
    listState: LazyListState,
) {
    Column(
        Modifier
            .height(300.dp)
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.background)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.appTypography.titleMedium,
            text = stringResource(id = R.string.core_select_value)
        )
        LazyColumn(Modifier.fillMaxWidth(), listState) {
            items(expandedList) { item ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            onItemClick(item)
                        }
                        .padding(vertical = 12.dp),
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
    expandedList: List<Pair<String, String>>,
    onItemClick: (Pair<String, String>) -> Unit,
) {
    Column(
        Modifier
            .height(300.dp)
            .padding(top = 16.dp)
            .background(MaterialTheme.appColors.background)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.appTypography.titleMedium,
            text = stringResource(id = R.string.core_select_value)
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            items(expandedList) { item ->
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
fun NewEdxOutlinedTextField(
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = modifier
        )
        if (!errorText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
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
            if (textLayoutResult.didOverflowWidth) {
                scaledTextStyle =
                    scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun DiscoveryCourseItem(course: Course, windowSize: WindowSize, onClick: (String) -> Unit) {
    val imageWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = 170.dp,
                compact = 105.dp
            )
        )
    }

    val imageUrl = BuildConfig.BASE_URL.dropLast(1) + course.media.courseImage?.uri
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick(course.courseId) }
            .background(MaterialTheme.appColors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .error(R.drawable.core_no_image_course)
                    .placeholder(R.drawable.core_no_image_course)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(imageWidth)
                    .height(105.dp)
                    .clip(MaterialTheme.appShapes.courseImageShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
            ) {
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = course.org, color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.labelMedium
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = course.name,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
        Modifier.noRippleClickable { onClick.invoke() }
    }
    Row(
        modifier = modifier.then(modifierClickable),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size((textStyle.fontSize.value + 4).dp),
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(text = text, color = color, style = textStyle)
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
        Modifier.noRippleClickable { onClick.invoke() }
    }
    Row(
        modifier = modifier.then(modifierClickable),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size((textStyle.fontSize.value + 4).dp),
            painter = painter,
            contentDescription = null,
            tint = color
        )
        Text(text = text, color = color, style = textStyle)
    }
}

@Composable
fun TextIcon(
    text: String,
    icon: ImageVector,
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
            modifier = Modifier.size((textStyle.fontSize.value + 4).dp),
            imageVector = icon,
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.core_offline),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.clickable { onDismissCLick() },
                    text = stringResource(id = R.string.core_dismiss),
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.primary
                )
                Text(
                    modifier = Modifier.clickable { onReloadClick() },
                    text = stringResource(id = R.string.core_reload),
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.primary
                )
            }
        }
    }
}

@Composable
fun NewEdxButton(
    width: Modifier = Modifier.fillMaxWidth(),
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.appColors.buttonBackground,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Button(
        modifier = Modifier
            .then(width)
            .height(42.dp),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor
        ),
        enabled = enabled,
        onClick = onClick
    ) {
        if (content == null) {
            Text(
                text = text,
                color = MaterialTheme.appColors.buttonText,
                style = MaterialTheme.appTypography.labelLarge
            )
        } else {
            content()
        }
    }
}

@Composable
fun NewEdxOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    backgroundColor: Color = Color.Transparent,
    borderColor: Color,
    textColor: Color,
    text: String,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    OutlinedButton(
        modifier = Modifier
            .then(modifier)
            .height(42.dp),
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor)
    ) {
        if (content == null) {
            Text(
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
    onBackClick: () -> Unit
) {
    IconButton(modifier = modifier,
        onClick = { onBackClick() }) {
        Icon(
            painter = painterResource(id = R.drawable.core_ic_back),
            contentDescription = "back",
            tint = tint
        )
    }
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
