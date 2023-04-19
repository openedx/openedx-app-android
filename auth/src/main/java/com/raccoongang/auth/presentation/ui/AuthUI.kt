package com.raccoongang.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raccoongang.auth.R
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.domain.model.RegistrationFieldType
import com.raccoongang.core.extension.TextConverter
import com.raccoongang.core.ui.HyperlinkText
import com.raccoongang.core.ui.SheetContent
import com.raccoongang.core.ui.noRippleClickable
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography

@Composable
fun RequiredFields(
    fields: List<RegistrationField>,
    mapFields: MutableMap<String, String?>,
    showErrorMap: MutableMap<String, Boolean?>,
    selectableNamesMap: MutableMap<String, String?>,
    onSelectClick: (String, RegistrationField, List<RegistrationField.Option>) -> Unit
) {
    fields.forEach { field ->
        when (field.type) {
            RegistrationFieldType.TEXT, RegistrationFieldType.EMAIL, RegistrationFieldType.CONFIRM_EMAIL, RegistrationFieldType.PASSWORD -> {
                InputRegistrationField(
                    modifier = Modifier.fillMaxWidth(),
                    isErrorShown = showErrorMap[field.name] ?: true,
                    registrationField = field,
                    onValueChanged = { serverName, value, isErrorShown ->
                        if (!isErrorShown) {
                            showErrorMap[serverName] = isErrorShown
                        }
                        mapFields[serverName] = value
                    }
                )
            }
            RegistrationFieldType.PLAINTEXT -> {
                val linkedText =
                    TextConverter.htmlTextToLinkedText(field.label)
                HyperlinkText(
                    fullText = linkedText.text,
                    hyperLinks = linkedText.links,
                    linkTextColor = MaterialTheme.appColors.primary
                )
            }
            RegistrationFieldType.CHECKBOX -> {
                //Text("checkbox")
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
                        mapFields[serverName] = value
                    }
                )
            }
            RegistrationFieldType.UNKNOWN -> {

            }
        }
    }
}

@Composable
fun OptionalFields(
    fields: List<RegistrationField>,
    mapFields: MutableMap<String, String?>,
    showErrorMap: MutableMap<String, Boolean?>,
    selectableNamesMap: MutableMap<String, String?>,
    onSelectClick: (String, RegistrationField, List<RegistrationField.Option>) -> Unit
) {
    Column() {
        fields.forEach { field ->
            when (field.type) {
                RegistrationFieldType.TEXT, RegistrationFieldType.EMAIL, RegistrationFieldType.CONFIRM_EMAIL, RegistrationFieldType.PASSWORD -> {
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
                            mapFields[serverName] =
                                value
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
                        linkTextColor = MaterialTheme.appColors.primary
                    )
                }
                RegistrationFieldType.CHECKBOX -> {
                    //Text("checkbox")
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
                        })
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
                                showErrorMap[serverName] =
                                    isErrorShown
                            }
                            mapFields[serverName] =
                                value
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
    onValueChanged: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: (FocusManager) -> Unit = { it.moveFocus(FocusDirection.Down) }
) {
    var loginTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }
    val focusManager = LocalFocusManager.current
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.auth_email),
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
            unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
            backgroundColor = MaterialTheme.appColors.textFieldBackground
        ),
        shape = MaterialTheme.appShapes.textFieldShape,
        placeholder = {
            Text(
                text = stringResource(id = R.string.auth_example_email),
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
        modifier = modifier
    )
}

@Composable
fun InputRegistrationField(
    modifier: Modifier,
    isErrorShown: Boolean,
    registrationField: RegistrationField,
    onValueChanged: (String, String, Boolean) -> Unit
) {
    var inputRegistrationFieldValue by rememberSaveable {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val visualTransformation = if (registrationField.type == RegistrationFieldType.PASSWORD) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
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
    Column {
        Text(
            modifier = Modifier.fillMaxWidth(),
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
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                backgroundColor = MaterialTheme.appColors.textFieldBackground
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            placeholder = {
                Text(
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
            textStyle = MaterialTheme.appTypography.bodyMedium,
            singleLine = isSingleLine,
            modifier = modifier
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
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
    onClick: (String, List<RegistrationField.Option>) -> Unit
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
            modifier = Modifier.fillMaxWidth(),
            text = registrationField.label,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            readOnly = true,
            enabled = false,
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
                .fillMaxWidth()
                .noRippleClickable {
                    onClick(registrationField.name, registrationField.options)
                },
            placeholder = {
                Text(
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
            text = helperText,
            style = MaterialTheme.appTypography.bodySmall,
            color = helperTextColor
        )
    }
}

@Composable
fun ExpandableText(
    isExpanded: Boolean,
    onClick: (Boolean) -> Unit
) {
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply {
            targetState = !isExpanded
        }
    }
    val transition = updateTransition(transitionState, label = "")
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
    val icon = Icons.Filled.ChevronRight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable {
                onClick(isExpanded)
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //TODO: textStyle
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SelectRegistrationFieldPreview() {
    NewEdxTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            SelectableRegisterField(
                field,
                false,
                initialValue = "",
                onClick = { _, _ ->

                }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun InputRegistrationFieldPreview() {
    NewEdxTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            InputRegistrationField(
                modifier = Modifier.fillMaxWidth(),
                isErrorShown = false,
                registrationField = field,
                onValueChanged = { _, _, _ ->

                }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OptionalFieldsPreview() {
    NewEdxTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            OptionalFields(
                fields = listOf(field, field, field),
                mapFields = SnapshotStateMap(),
                showErrorMap = SnapshotStateMap(),
                selectableNamesMap = SnapshotStateMap(),
                onSelectClick = { _, _, _ ->

                }

            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequiredFieldsPreview() {
    NewEdxTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            RequiredFields(
                fields = listOf(field, field, field),
                mapFields = SnapshotStateMap(),
                showErrorMap = SnapshotStateMap(),
                selectableNamesMap = SnapshotStateMap(),
                onSelectClick = { _, _, _ ->

                }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SheetContentPreview() {
    NewEdxTheme {
        Column(Modifier.background(MaterialTheme.appColors.background)) {
            SheetContent(
                expandedList = listOf(option, option, option),
                onItemClick = {},
                listState = rememberLazyListState()
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