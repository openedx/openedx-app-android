package org.openedx.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.HyperlinkText
import org.openedx.core.ui.SheetContent
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.tagId

@Composable
fun RequiredFields(
    fields: List<RegistrationField>,
    showErrorMap: MutableMap<String, Boolean?>,
    selectableNamesMap: MutableMap<String, String?>,
    onFieldUpdated: (String, String) -> Unit,
    onSelectClick: (String, RegistrationField, List<RegistrationField.Option>) -> Unit,
) {
    fields.forEach { field ->
        when (field.type) {
            RegistrationFieldType.TEXT,
            RegistrationFieldType.EMAIL,
            RegistrationFieldType.CONFIRM_EMAIL,
            RegistrationFieldType.PASSWORD,
            -> {
                InputRegistrationField(
                    modifier = Modifier.fillMaxWidth(),
                    isErrorShown = showErrorMap[field.name] ?: true,
                    registrationField = field,
                    onValueChanged = { serverName, value, isErrorShown ->
                        if (!isErrorShown) {
                            showErrorMap[serverName] = isErrorShown
                        }
                        onFieldUpdated(serverName, value)
                    }
                )
            }

            RegistrationFieldType.PLAINTEXT -> {
                val linkedText =
                    TextConverter.htmlTextToLinkedText(field.label)
                HyperlinkText(
                    modifier = Modifier.testTag("txt_${field.name.tagId()}"),
                    fullText = linkedText.text,
                    hyperLinks = linkedText.links,
                    linkTextColor = MaterialTheme.appColors.primary
                )
            }

            RegistrationFieldType.CHECKBOX -> {
                CheckboxField(text = field.label, defaultValue = field.defaultValue) {
                    onFieldUpdated(field.name, it.toString())
                }
            }

            RegistrationFieldType.SELECT -> {
                SelectableRegisterField(
                    registrationField = field,
                    isErrorShown = showErrorMap[field.name] ?: true,
                    initialValue = selectableNamesMap[field.name] ?: "",
                    onClick = { serverName, list ->
                        onSelectClick(serverName, field, list)
                    }
                )
            }

            RegistrationFieldType.TEXTAREA -> {
                InputRegistrationField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    isErrorShown = showErrorMap[field.name] ?: true,
                    registrationField = field,
                    onValueChanged = { serverName, value, isErrorShown ->
                        if (!isErrorShown) {
                            showErrorMap[serverName] = isErrorShown
                        }
                        onFieldUpdated(serverName, value)
                    }
                )
            }

            RegistrationFieldType.UNKNOWN -> {}
        }
    }
}

@Composable
fun OptionalFields(
    fields: List<RegistrationField>,
    showErrorMap: MutableMap<String, Boolean?>,
    selectableNamesMap: MutableMap<String, String?>,
    onSelectClick: (String, RegistrationField, List<RegistrationField.Option>) -> Unit,
    onFieldUpdated: (String, String) -> Unit,
    hyperLinkAction: ((Map<String, String>, String) -> Unit)? = null,
) {
    Column {
        fields.forEach { field ->
            when (field.type) {
                RegistrationFieldType.TEXT, RegistrationFieldType.EMAIL,
                RegistrationFieldType.CONFIRM_EMAIL, RegistrationFieldType.PASSWORD -> {
                    InputRegistrationField(
                        modifier = Modifier.fillMaxWidth(),
                        isErrorShown = showErrorMap[field.name]
                            ?: true,
                        registrationField = field,
                        onValueChanged = { serverName, value, isErrorShown ->
                            if (!isErrorShown) {
                                showErrorMap[serverName] =
                                    isErrorShown
                            }
                            onFieldUpdated(serverName, value)
                        }
                    )
                }

                RegistrationFieldType.PLAINTEXT -> {
                    val linkedText =
                        TextConverter.htmlTextToLinkedText(
                            field.label
                        )
                    HyperlinkText(
                        fullText = linkedText.text,
                        hyperLinks = linkedText.links,
                        linkTextColor = MaterialTheme.appColors.textHyperLink,
                        linkTextDecoration = TextDecoration.Underline,
                        action = {
                            hyperLinkAction?.invoke(linkedText.links, it)
                        },
                    )
                }

                RegistrationFieldType.CHECKBOX -> {
                    CheckboxField(text = field.label, defaultValue = field.defaultValue) {
                        onFieldUpdated(field.name, it.toString())
                    }
                }

                RegistrationFieldType.SELECT -> {
                    SelectableRegisterField(
                        registrationField = field,
                        isErrorShown = showErrorMap[field.name]
                            ?: true,
                        initialValue = selectableNamesMap[field.name]
                            ?: "",
                        onClick = { serverName, list ->
                            onSelectClick(serverName, field, list)
                        }
                    )
                }

                RegistrationFieldType.TEXTAREA -> {
                    InputRegistrationField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        isErrorShown = showErrorMap[field.name]
                            ?: true,
                        registrationField = field,
                        onValueChanged = { serverName, value, isErrorShown ->
                            if (!isErrorShown) {
                                showErrorMap[serverName] = isErrorShown
                            }
                            onFieldUpdated(serverName, value)
                        }
                    )
                }

                RegistrationFieldType.UNKNOWN -> {
                }
            }
        }
    }
}

@Composable
fun LoginTextField(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    isError: Boolean = false,
    errorMessages: String = "",
    onValueChanged: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: (FocusManager) -> Unit = { it.moveFocus(FocusDirection.Down) },
) {
    var loginTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }
    val focusManager = LocalFocusManager.current
    Text(
        modifier = Modifier
            .testTag("txt_email_label")
            .fillMaxWidth(),
        text = title,
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.labelLarge
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = loginTextFieldValue,
        onValueChange = {
            loginTextFieldValue = it
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
                modifier = Modifier.testTag("txt_email_placeholder"),
                text = description,
                color = MaterialTheme.appColors.textFieldHint,
                style = MaterialTheme.appTypography.bodyMedium
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions {
            keyboardActions(focusManager)
        },
        textStyle = MaterialTheme.appTypography.bodyMedium,
        singleLine = true,
        modifier = modifier.testTag("tf_email"),
        isError = isError
    )
    if (isError) {
        Text(
            modifier = Modifier
                .testTag("txt_email_error")
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = errorMessages,
            style = MaterialTheme.appTypography.bodySmall,
            color = MaterialTheme.appColors.error,
        )
    }
}

@Composable
fun InputRegistrationField(
    modifier: Modifier,
    isErrorShown: Boolean,
    registrationField: RegistrationField,
    onValueChanged: (String, String, Boolean) -> Unit,
) {
    var inputRegistrationFieldValue by rememberSaveable {
        mutableStateOf(registrationField.placeholder)
    }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val visualTransformation = remember(isPasswordVisible) {
        if (registrationField.type == RegistrationFieldType.PASSWORD && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    }
    val keyboardType = when (registrationField.type) {
        RegistrationFieldType.CONFIRM_EMAIL, RegistrationFieldType.EMAIL -> KeyboardType.Email
        RegistrationFieldType.PASSWORD -> KeyboardType.Password
        else -> KeyboardType.Text
    }
    val isSingleLine = registrationField.type != RegistrationFieldType.TEXTAREA
    val helperTextColor = if (registrationField.errorInstructions.isEmpty()) {
        MaterialTheme.appColors.textSecondary
    } else if (isErrorShown) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textSecondary
    }
    val helperText = if (registrationField.errorInstructions.isEmpty()) {
        registrationField.instructions
    } else if (isErrorShown) {
        registrationField.errorInstructions
    } else {
        registrationField.instructions
    }
    val trailingIcon: @Composable (() -> Unit)? =
        if (registrationField.type == RegistrationFieldType.PASSWORD) {
            {
                PasswordVisibilityIcon(
                    isPasswordVisible = isPasswordVisible,
                    onClick = { isPasswordVisible = !isPasswordVisible }
                )
            }
        } else {
            null
        }

    Column {
        Text(
            modifier = Modifier
                .testTag("txt_${registrationField.name.tagId()}_label")
                .fillMaxWidth(),
            text = registrationField.label,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = inputRegistrationFieldValue,
            onValueChange = {
                inputRegistrationFieldValue = it
                if (registrationField.errorInstructions.isNotEmpty() && isErrorShown) {
                    onValueChanged(registrationField.name, it.trim(), false)
                } else {
                    onValueChanged(registrationField.name, it.trim(), true)
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.appColors.textFieldText,
                backgroundColor = MaterialTheme.appColors.textFieldBackground,
                focusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                cursorColor = MaterialTheme.appColors.textFieldText,
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            placeholder = {
                Text(
                    modifier = modifier.testTag("txt_${registrationField.name.tagId()}_placeholder"),
                    text = registrationField.label,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            visualTransformation = visualTransformation,
            keyboardActions = KeyboardActions {
                focusManager.moveFocus(FocusDirection.Down)
            },
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.appTypography.bodyMedium,
            singleLine = isSingleLine,
            modifier = modifier.testTag("tf_${registrationField.name.tagId()}")
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            modifier = Modifier.testTag("txt_${registrationField.name.tagId()}_description"),
            text = helperText,
            style = MaterialTheme.appTypography.bodySmall,
            color = helperTextColor
        )
    }
}

@Composable
fun SelectableRegisterField(
    registrationField: RegistrationField,
    isErrorShown: Boolean,
    initialValue: String,
    onClick: (String, List<RegistrationField.Option>) -> Unit,
) {
    val helperTextColor = if (registrationField.errorInstructions.isEmpty()) {
        MaterialTheme.appColors.textSecondary
    } else if (isErrorShown) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textSecondary
    }
    val helperText = if (registrationField.errorInstructions.isEmpty()) {
        registrationField.instructions
    } else if (isErrorShown) {
        registrationField.errorInstructions
    } else {
        registrationField.instructions
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .focusTarget()
            .onFocusChanged {
                if (it.isFocused) {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            }
    ) {
        Text(
            modifier = Modifier
                .testTag("txt_${registrationField.name.tagId()}_label")
                .fillMaxWidth(),
            text = registrationField.label,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            readOnly = true,
            enabled = false,
            singleLine = true,
            value = initialValue,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                disabledBorderColor = MaterialTheme.appColors.textFieldBorder,
                disabledTextColor = MaterialTheme.appColors.textPrimary,
                backgroundColor = MaterialTheme.appColors.textFieldBackground,
                disabledPlaceholderColor = MaterialTheme.appColors.textFieldHint
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            textStyle = MaterialTheme.appTypography.bodyMedium,
            onValueChange = { },
            modifier = Modifier
                .testTag("tf_${registrationField.name.tagId()}")
                .fillMaxWidth()
                .noRippleClickable {
                    onClick(registrationField.name, registrationField.options)
                },
            placeholder = {
                Text(
                    modifier = Modifier.testTag("txt_${registrationField.name.tagId()}_placeholder"),
                    text = registrationField.label,
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimaryVariant
                )
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            modifier = Modifier.testTag("txt_${registrationField.name.tagId()}_description"),
            text = helperText,
            style = MaterialTheme.appTypography.bodySmall,
            color = helperTextColor
        )
    }
}

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onClick: (Boolean) -> Unit,
) {
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply {
            targetState = !isExpanded
        }
    }
    val transition = rememberTransition(transitionState, label = "")
    val arrowRotationDegree by transition.animateFloat({
        tween(durationMillis = 300)
    }, label = "") {
        it
        if (!isExpanded) 0f else 90f
    }
    val text = if (isExpanded) {
        stringResource(id = R.string.auth_hide_optional_fields)
    } else {
        stringResource(id = R.string.auth_show_optional_fields)
    }
    val icon = Icons.AutoMirrored.Filled.KeyboardArrowRight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .noRippleClickable {
                onClick(isExpanded)
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier,
            text = text,
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.primary
        )
        Icon(
            modifier = Modifier.rotate(arrowRotationDegree),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.appColors.background
        )
    }
}

@Composable
internal fun PasswordVisibilityIcon(
    isPasswordVisible: Boolean,
    onClick: () -> Unit,
) {
    val (image, description) = if (isPasswordVisible) {
        Icons.Filled.VisibilityOff to stringResource(R.string.auth_accessibility_hide_password)
    } else {
        Icons.Filled.Visibility to stringResource(R.string.auth_accessibility_show_password)
    }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = MaterialTheme.appColors.onSurface
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SelectRegistrationFieldPreview() {
    OpenEdXTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            SelectableRegisterField(
                field,
                false,
                initialValue = "",
                onClick = { _, _ -> }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun InputRegistrationFieldPreview() {
    OpenEdXTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            InputRegistrationField(
                modifier = Modifier.fillMaxWidth(),
                isErrorShown = false,
                registrationField = field,
                onValueChanged = { _, _, _ -> }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OptionalFieldsPreview() {
    OpenEdXTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            val optionalField = field.copy(required = false)
            OptionalFields(
                fields = List(size = 3) { optionalField },
                showErrorMap = SnapshotStateMap(),
                selectableNamesMap = SnapshotStateMap(),
                onSelectClick = { _, _, _ -> },
                onFieldUpdated = { _, _ -> }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequiredFieldsPreview() {
    OpenEdXTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            RequiredFields(
                fields = listOf(field, field, field),
                showErrorMap = SnapshotStateMap(),
                selectableNamesMap = SnapshotStateMap(),
                onSelectClick = { _, _, _ -> },
                onFieldUpdated = { _, _ -> }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetContentPreview() {
    OpenEdXTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            SheetContent(
                searchValue = TextFieldValue(),
                expandedList = listOf(option, option, option),
                onItemClick = {},
                listState = rememberLazyListState(),
                searchValueChanged = {}
            )
        }
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
