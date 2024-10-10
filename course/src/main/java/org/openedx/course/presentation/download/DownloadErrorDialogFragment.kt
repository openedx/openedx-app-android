package org.openedx.course.presentation.download

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.domain.model.DownloadDialogResource
import org.openedx.foundation.extension.parcelable
import org.openedx.foundation.system.PreviewFragmentManager
import org.openedx.core.R as coreR

class DownloadErrorDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val dialogType =
                    requireArguments().parcelable<DownloadErrorDialogType>(ARG_DIALOG_TYPE) ?: return@OpenEdXTheme
                val uiState = requireArguments().parcelable<DownloadDialogUIState>(ARG_UI_STATE) ?: return@OpenEdXTheme
                val downloadDialogResource = when (dialogType) {
                    DownloadErrorDialogType.NO_CONNECTION -> DownloadDialogResource(
                        title = stringResource(id = coreR.string.core_no_internet_connection),
                        description = stringResource(id = R.string.course_download_no_internet_dialog_description),
                        icon = painterResource(id = R.drawable.course_ic_error),
                    )

                    DownloadErrorDialogType.WIFI_REQUIRED -> DownloadDialogResource(
                        title = stringResource(id = R.string.course_wifi_required),
                        description = stringResource(id = R.string.course_download_wifi_required_dialog_description),
                        icon = painterResource(id = R.drawable.course_ic_error),
                    )

                    DownloadErrorDialogType.DOWNLOAD_FAILED -> DownloadDialogResource(
                        title = stringResource(id = R.string.course_download_failed),
                        description = stringResource(id = R.string.course_download_failed_dialog_description),
                        icon = painterResource(id = R.drawable.course_ic_error),
                    )
                }

                DownloadErrorDialogView(
                    downloadDialogResource = downloadDialogResource,
                    uiState = uiState,
                    dialogType = dialogType,
                    onTryAgainClick = {
                        uiState.saveDownloadModels()
                        dismiss()
                    },
                    onCancelClick = {
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        const val DIALOG_TAG = "DownloadErrorDialogFragment"
        const val ARG_DIALOG_TYPE = "dialogType"
        const val ARG_UI_STATE = "uiState"

        fun newInstance(
            dialogType: DownloadErrorDialogType,
            uiState: DownloadDialogUIState
        ): DownloadErrorDialogFragment {
            val dialog = DownloadErrorDialogFragment()
            dialog.arguments = bundleOf(
                ARG_DIALOG_TYPE to dialogType,
                ARG_UI_STATE to uiState
            )
            return dialog
        }
    }
}

@Composable
private fun DownloadErrorDialogView(
    modifier: Modifier = Modifier,
    uiState: DownloadDialogUIState,
    downloadDialogResource: DownloadDialogResource,
    dialogType: DownloadErrorDialogType,
    onTryAgainClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val dismissButtonText = when (dialogType) {
        DownloadErrorDialogType.DOWNLOAD_FAILED -> stringResource(id = coreR.string.core_cancel)
        else -> stringResource(id = coreR.string.core_close)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                downloadDialogResource.icon?.let { icon ->
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                AutoSizeText(
                    text = downloadDialogResource.title,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textDark,
                    minSize = MaterialTheme.appTypography.titleLarge.fontSize.value - 1
                )
            }
            Column {
                uiState.downloadDialogItems.forEach {
                    DownloadDialogItem(downloadDialogItem = it)
                }
            }
            Text(
                text = downloadDialogResource.description,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
            if (dialogType == DownloadErrorDialogType.DOWNLOAD_FAILED) {
                OpenEdXButton(
                    text = stringResource(id = coreR.string.core_error_try_again),
                    backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
                    onClick = onTryAgainClick,
                )
            }
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = dismissButtonText,
                backgroundColor = MaterialTheme.appColors.background,
                borderColor = MaterialTheme.appColors.primaryButtonBackground,
                textColor = MaterialTheme.appColors.primaryButtonBackground,
                onClick = {
                    onCancelClick()
                }
            )
        }
    }
}

@Preview
@Composable
private fun DownloadErrorDialogViewPreview() {
    OpenEdXTheme {
        DownloadErrorDialogView(
            downloadDialogResource = DownloadDialogResource(
                title = "Title",
                description = "Description Description Description Description Description Description Description ",
                icon = painterResource(id = R.drawable.course_ic_error)
            ),
            uiState = DownloadDialogUIState(
                downloadDialogItems = listOf(
                    DownloadDialogItem(
                        title = "Subsection title 1",
                        size = 20000
                    ),
                    DownloadDialogItem(
                        title = "Subsection title 2",
                        size = 10000000
                    )
                ),
                sizeSum = 100000,
                isAllBlocksDownloaded = false,
                isDownloadFailed = false,
                fragmentManager = PreviewFragmentManager,
                removeDownloadModels = {},
                saveDownloadModels = {}
            ),
            onCancelClick = {},
            onTryAgainClick = {},
            dialogType = DownloadErrorDialogType.DOWNLOAD_FAILED
        )
    }
}
