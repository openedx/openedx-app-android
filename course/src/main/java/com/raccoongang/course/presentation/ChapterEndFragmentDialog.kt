package com.raccoongang.course.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.raccoongang.core.ui.NewEdxButton
import com.raccoongang.core.ui.NewEdxOutlinedButton
import com.raccoongang.core.ui.TextIcon
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.R
import com.raccoongang.course.presentation.section.CourseSectionFragment

class ChapterEndFragmentDialog : DialogFragment() {

    var listener: DialogListener? = null

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
            NewEdxTheme {
                ChapterEndDialogScreen(
                    sectionName = requireArguments().getString(ARG_SECTION_NAME) ?: "",
                    nextSectionName = requireArguments().getString(ARG_NEXT_SECTION_NAME) ?: "",
                    onBackButtonClick = {
                        dismiss()
                        listener?.onDismiss()
                        requireActivity().supportFragmentManager.popBackStack(
                            CourseSectionFragment::class.java.simpleName,
                            0
                        )
                    },
                    onProceedButtonClick = {
                        dismiss()
                        listener?.onClick(true)
                    }
                )
            }
        }
    }

    companion object {
        private const val ARG_SECTION_NAME = "sectionName"
        private const val ARG_NEXT_SECTION_NAME = "nexSectionName"
        fun newInstance(
            sectionName: String,
            nextSectionName: String
        ): ChapterEndFragmentDialog {
            val dialog = ChapterEndFragmentDialog()
            dialog.arguments = bundleOf(
                ARG_SECTION_NAME to sectionName,
                ARG_NEXT_SECTION_NAME to nextSectionName
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
    onBackButtonClick: () -> Unit,
    onProceedButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .clip(MaterialTheme.appShapes.courseImageShape),
        backgroundColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.courseImageShape
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .padding(top = 48.dp, bottom = 38.dp),
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
                text = stringResource(id = R.string.course_good_work),
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
                NewEdxButton(
                    text = stringResource(id = R.string.course_next_section),
                    content = {
                        TextIcon(
                            text = stringResource(id = R.string.course_next_section),
                            painter = painterResource(com.raccoongang.core.R.drawable.core_ic_forward),
                            color = MaterialTheme.appColors.buttonText,
                            textStyle = MaterialTheme.appTypography.labelLarge
                        )
                    },
                    onClick = onProceedButtonClick
                )
                Spacer(Modifier.height(16.dp))
            }
            NewEdxOutlinedButton(
                borderColor = MaterialTheme.appColors.buttonBackground,
                textColor = MaterialTheme.appColors.buttonBackground,
                text = stringResource(id = R.string.course_back_to_outline),
                onClick = onBackButtonClick
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

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ChapterEndDialogScreenPreview() {
    NewEdxTheme {
        ChapterEndDialogScreen(
            sectionName = "Section",
            nextSectionName = "Section2",
            onBackButtonClick = {},
            onProceedButtonClick = {}
        )
    }
}