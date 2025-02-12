package org.openedx.profile.presentation.calendar

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.koin.androidx.compose.koinViewModel
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.crop
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.parcelable
import org.openedx.foundation.extension.toastMessage
import org.openedx.profile.R
import org.openedx.profile.presentation.calendar.NewCalendarDialogFragment.Companion.MAX_CALENDAR_TITLE_LENGTH
import androidx.compose.ui.graphics.Color as ComposeColor
import org.openedx.core.R as CoreR

class NewCalendarDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val viewModel: NewCalendarDialogViewModel = koinViewModel()

                LaunchedEffect(Unit) {
                    viewModel.uiMessage.collect { message ->
                        if (message.isNotEmpty()) {
                            context.toastMessage(message)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.isSuccess.collect { isSuccess ->
                        if (isSuccess) {
                            dismiss()
                        }
                    }
                }

                NewCalendarDialog(
                    newCalendarDialogType = requireArguments().parcelable<NewCalendarDialogType>(ARG_DIALOG_TYPE)
                        ?: NewCalendarDialogType.CREATE_NEW,
                    onCancelClick = {
                        dismiss()
                    },
                    onBeginSyncingClick = { calendarTitle, calendarColor ->
                        viewModel.createCalendar(calendarTitle, calendarColor)
                    }
                )
            }
        }
    }

    companion object {
        const val DIALOG_TAG = "NewCalendarDialogFragment"
        const val ARG_DIALOG_TYPE = "ARG_DIALOG_TYPE"
        const val MAX_CALENDAR_TITLE_LENGTH = 40

        fun newInstance(
            newCalendarDialogType: NewCalendarDialogType
        ): NewCalendarDialogFragment {
            val fragment = NewCalendarDialogFragment()
            fragment.arguments = bundleOf(
                ARG_DIALOG_TYPE to newCalendarDialogType
            )
            return fragment
        }

        fun getDefaultCalendarTitle(context: Context): String {
            return "${context.getString(CoreR.string.app_name)} ${context.getString(R.string.profile_course_dates)}"
        }
    }
}

@Composable
private fun NewCalendarDialog(
    modifier: Modifier = Modifier,
    newCalendarDialogType: NewCalendarDialogType,
    onCancelClick: () -> Unit,
    onBeginSyncingClick: (calendarTitle: String, calendarColor: CalendarColor) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val title = when (newCalendarDialogType) {
        NewCalendarDialogType.CREATE_NEW -> stringResource(id = R.string.profile_new_calendar)
        NewCalendarDialogType.UPDATE -> stringResource(id = R.string.profile_change_sync_options)
    }
    var calendarTitle by rememberSaveable {
        mutableStateOf("")
    }
    var calendarColor by rememberSaveable {
        mutableStateOf(CalendarColor.ACCENT)
    }
    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onCancelClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.titleLarge
                )
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onCancelClick()
                        },
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.primary
                )
            }
            CalendarTitleTextField(
                onValueChanged = {
                    calendarTitle = it
                }
            )
            ColorDropdown(
                onValueChanged = {
                    calendarColor = it
                }
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.profile_new_calendar_description),
                style = MaterialTheme.appTypography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.appColors.textDark
            )
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = CoreR.string.core_cancel),
                backgroundColor = MaterialTheme.appColors.background,
                borderColor = MaterialTheme.appColors.primaryButtonBackground,
                textColor = MaterialTheme.appColors.primaryButtonBackground,
                onClick = {
                    onCancelClick()
                }
            )
            OpenEdXButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.profile_begin_syncing),
                onClick = {
                    onBeginSyncingClick(
                        calendarTitle.ifEmpty { NewCalendarDialogFragment.getDefaultCalendarTitle(context) },
                        calendarColor
                    )
                }
            )
        }
    }
}

@Composable
private fun CalendarTitleTextField(
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }

    Column {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.profile_calendar_name),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp),
            value = textFieldValue,
            onValueChange = {
                if (it.text.length <= MAX_CALENDAR_TITLE_LENGTH) textFieldValue = it
                onValueChanged(it.text.trim())
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                textColor = MaterialTheme.appColors.textPrimary
            ),
            shape = MaterialTheme.appShapes.textFieldShape,
            placeholder = {
                Text(
                    text = NewCalendarDialogFragment.getDefaultCalendarTitle(LocalContext.current),
                    color = MaterialTheme.appColors.textFieldHint,
                    style = MaterialTheme.appTypography.bodyMedium
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions {
                focusManager.clearFocus()
            },
            textStyle = MaterialTheme.appTypography.bodyMedium,
            singleLine = true
        )
    }
}

@Composable
private fun ColorDropdown(
    modifier: Modifier = Modifier,
    onValueChanged: (CalendarColor) -> Unit
) {
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(CalendarColor.ACCENT) }
    var dropdownWidth by remember { mutableStateOf(300.dp) }
    val colorArrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = ""
    )

    Column(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.profile_color),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(MaterialTheme.appShapes.textFieldShape)
                .border(
                    1.dp,
                    MaterialTheme.appColors.textFieldBorder,
                    MaterialTheme.appShapes.textFieldShape
                )
                .onSizeChanged {
                    dropdownWidth = with(density) { it.width.toDp() }
                }
                .clickable {
                    expanded = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorCircle(
                modifier = Modifier
                    .padding(start = 16.dp),
                color = ComposeColor(currentValue.color)
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                text = stringResource(id = currentValue.title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.bodyMedium
            )
            Icon(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .rotate(colorArrowRotation),
                imageVector = Icons.Default.ExpandMore,
                tint = MaterialTheme.appColors.textDark,
                contentDescription = null
            )
        }

        MaterialTheme(
            colors = MaterialTheme.colors.copy(surface = MaterialTheme.appColors.background),
            shapes = MaterialTheme.shapes.copy(MaterialTheme.appShapes.textFieldShape)
        ) {
            Spacer(modifier = Modifier.padding(top = 4.dp))
            DropdownMenu(
                modifier = Modifier
                    .crop(vertical = 8.dp)
                    .height(240.dp)
                    .width(dropdownWidth)
                    .border(
                        1.dp,
                        MaterialTheme.appColors.textFieldBorder,
                        MaterialTheme.appShapes.textFieldShape
                    )
                    .crop(vertical = 8.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for ((index, calendarColor) in CalendarColor.entries.withIndex()) {
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.background),
                        onClick = {
                            currentValue = calendarColor
                            expanded = false
                            onValueChanged(CalendarColor.entries[index])
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ColorCircle(
                                color = ComposeColor(calendarColor.color)
                            )
                            Text(
                                text = stringResource(id = calendarColor.title),
                                style = MaterialTheme.appTypography.titleSmall,
                                color = MaterialTheme.appColors.textDark
                            )
                        }
                    }
                    if (index < CalendarColor.entries.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.appColors.divider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCircle(
    modifier: Modifier = Modifier,
    color: ComposeColor
) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewCalendarDialogPreview() {
    OpenEdXTheme {
        NewCalendarDialog(
            newCalendarDialogType = NewCalendarDialogType.CREATE_NEW,
            onCancelClick = { },
            onBeginSyncingClick = { _, _ -> }
        )
    }
}
