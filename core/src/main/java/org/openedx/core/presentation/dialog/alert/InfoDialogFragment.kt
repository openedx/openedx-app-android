package org.openedx.core.presentation.dialog.alert

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.R
import org.openedx.core.presentation.global.appupgrade.DefaultTextButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

class InfoDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                InfoDialog(
                    title = requireArguments().getString(ARG_TITLE, ""),
                    message = requireArguments().getString(ARG_MESSAGE, ""),
                    onClick = {
                        dismiss()
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        fun newInstance(
            title: String,
            message: String,
        ): InfoDialogFragment {
            val fragment = InfoDialogFragment()
            fragment.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_MESSAGE to message,
            )
            return fragment
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.cardShape)
            .background(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.cardShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = message,
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )
            DefaultTextButton(
                text = stringResource(R.string.core_ok),
                onClick = onClick
            )
        }
    }
}

@Preview
@Composable
private fun SimpleDialogPreview() {
    InfoDialog(
        title = "Important Notice",
        message = "This is an important announcement.",
        onClick = {}
    )
}
