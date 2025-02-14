package org.openedx.course.presentation

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.foundation.extension.setWidthPercent

class ChapterEndFragmentDialog : DialogFragment() {

    var listener: DialogListener? = null

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setWidthPercent(percentage = 66)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val configuration = LocalConfiguration.current
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    ChapterEndDialogScreen(
                        sectionName = requireArguments().getString(ARG_SECTION_NAME) ?: "",
                        nextSectionName = requireArguments().getString(ARG_NEXT_SECTION_NAME) ?: "",
                        isVerticalNavigation = requireArguments().getBoolean(
                            ARG_IS_VERTICAL_NAVIGATION
                        ),
                        onBackButtonClick = {
                            dismiss()
                            listener?.onDismiss()
                        },
                        onProceedButtonClick = {
                            dismiss()
                            listener?.onClick(true)
                        },
                        onCancelButtonClick = {
                            dismiss()
                        }
                    )
                } else {
                    ChapterEndDialogScreenLandscape(
                        sectionName = requireArguments().getString(ARG_SECTION_NAME) ?: "",
                        nextSectionName = requireArguments().getString(ARG_NEXT_SECTION_NAME) ?: "",
                        onBackButtonClick = {
                            dismiss()
                            listener?.onDismiss()
                        },
                        onProceedButtonClick = {
                            dismiss()
                            listener?.onClick(true)
                        },
                        onCancelButtonClick = {
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        listener = null
        super.onDestroy()
    }

    companion object {
        private const val ARG_SECTION_NAME = "sectionName"
        private const val ARG_NEXT_SECTION_NAME = "nexSectionName"
        private const val ARG_IS_VERTICAL_NAVIGATION = "isVerticalNavigation"
        fun newInstance(
            sectionName: String,
            nextSectionName: String,
            isVerticalNavigation: Boolean
        ): ChapterEndFragmentDialog {
            val dialog = ChapterEndFragmentDialog()
            dialog.arguments = bundleOf(
                ARG_SECTION_NAME to sectionName,
                ARG_NEXT_SECTION_NAME to nextSectionName,
                ARG_IS_VERTICAL_NAVIGATION to isVerticalNavigation
            )
            return dialog
        }
    }
}

interface DialogListener {
    fun <T> onClick(value: T)
    fun onDismiss()
}

@Composable
private fun ChapterEndDialogScreen(
    sectionName: String,
    nextSectionName: String,
    isVerticalNavigation: Boolean,
    onBackButtonClick: () -> Unit,
    onProceedButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    val nextSectionButtonIcon = if (isVerticalNavigation) {
        Icons.Default.ArrowDownward
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }
    Card(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.95f)
            .clip(MaterialTheme.appShapes.courseImageShape),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape
    ) {
        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onCancelButtonClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = org.openedx.core.R.string.core_cancel),
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
            Icon(
                modifier = Modifier
                    .width(76.dp)
                    .height(72.dp),
                painter = painterResource(id = R.drawable.course_id_diamond),
                contentDescription = null,
                tint = MaterialTheme.appColors.onBackground
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = stringResource(id = R.string.course_good_job),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.course_section_finished, sectionName),
                color = MaterialTheme.appColors.textFieldText,
                style = MaterialTheme.appTypography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(42.dp))
            if (nextSectionName.isNotEmpty()) {
                OpenEdXButton(
                    text = stringResource(id = R.string.course_next_section),
                    content = {
                        TextIcon(
                            text = stringResource(id = R.string.course_next_section),
                            icon = nextSectionButtonIcon,
                            color = MaterialTheme.appColors.primaryButtonText,
                            textStyle = MaterialTheme.appTypography.labelLarge,
                        )
                    },
                    onClick = onProceedButtonClick
                )
                Spacer(Modifier.height(16.dp))
            }
            OpenEdXOutlinedButton(
                borderColor = MaterialTheme.appColors.primaryButtonBackground,
                textColor = MaterialTheme.appColors.primaryButtonBackground,
                text = stringResource(id = R.string.course_back_to_outline),
                onClick = onBackButtonClick,
                content = {
                    AutoSizeText(
                        text = stringResource(id = R.string.course_back_to_outline),
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.primaryButtonBorderedText
                    )
                }
            )
            if (nextSectionName.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.course_to_proceed, nextSectionName),
                    color = MaterialTheme.appColors.textPrimaryVariant,
                    style = MaterialTheme.appTypography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChapterEndDialogScreenLandscape(
    sectionName: String,
    nextSectionName: String,
    onBackButtonClick: () -> Unit,
    onProceedButtonClick: () -> Unit,
    onCancelButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.courseImageShape),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape
    ) {
        Column(
            modifier = Modifier.padding(38.dp)
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onCancelButtonClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = org.openedx.core.R.string.core_cancel),
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier
                            .width(76.dp)
                            .height(72.dp),
                        painter = painterResource(id = R.drawable.course_id_diamond),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onBackground
                    )
                    Spacer(Modifier.height(36.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.course_good_job),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.course_section_finished, sectionName),
                        color = MaterialTheme.appColors.textFieldText,
                        style = MaterialTheme.appTypography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.width(42.dp))
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (nextSectionName.isNotEmpty()) {
                        OpenEdXButton(
                            text = stringResource(id = R.string.course_next_section),
                            content = {
                                TextIcon(
                                    text = stringResource(id = R.string.course_next_section),
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    color = MaterialTheme.appColors.primaryButtonText,
                                    textStyle = MaterialTheme.appTypography.labelLarge
                                )
                            },
                            onClick = onProceedButtonClick
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    OpenEdXOutlinedButton(
                        borderColor = MaterialTheme.appColors.primaryButtonBackground,
                        textColor = MaterialTheme.appColors.primaryButtonBackground,
                        text = stringResource(id = R.string.course_back_to_outline),
                        onClick = onBackButtonClick,
                        content = {
                            AutoSizeText(
                                text = stringResource(id = R.string.course_back_to_outline),
                                style = MaterialTheme.appTypography.bodyMedium,
                                color = MaterialTheme.appColors.primaryButtonBorderedText
                            )
                        }
                    )
                    if (nextSectionName.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = R.string.course_to_proceed, nextSectionName),
                            color = MaterialTheme.appColors.textPrimaryVariant,
                            style = MaterialTheme.appTypography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ChapterEndDialogScreenPreview() {
    OpenEdXTheme {
        ChapterEndDialogScreen(
            sectionName = "Section",
            nextSectionName = "Section2",
            isVerticalNavigation = true,
            onBackButtonClick = {},
            onProceedButtonClick = {},
            onCancelButtonClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ChapterEndDialogScreenLandscapePreview() {
    OpenEdXTheme {
        ChapterEndDialogScreenLandscape(
            sectionName = "Section",
            nextSectionName = "Section2",
            onBackButtonClick = {},
            onProceedButtonClick = {},
            onCancelButtonClick = {}
        )
    }
}
