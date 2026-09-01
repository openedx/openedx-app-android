package org.openedx.core.presentation.dialog.downloaddialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.R
import org.openedx.core.domain.model.DownloadDialogResource
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.parcelable
import org.openedx.foundation.extension.toFileSize
import org.openedx.foundation.system.PreviewFragmentManager
import androidx.compose.ui.graphics.Color as ComposeColor

class DownloadConfirmDialogFragment : DialogFragment(), DownloadDialog {

    override var listener: DownloadDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val dialogType =
                    requireArguments().parcelable<DownloadConfirmDialogType>(ARG_DIALOG_TYPE)
                        ?: return@OpenEdXTheme
                val uiState = requireArguments().parcelable<DownloadDialogUIState>(ARG_UI_STATE)
                    ?: return@OpenEdXTheme
                val sizeSumString = uiState.sizeSum.toFileSize(1, false)
                val dialogData = when (dialogType) {
                    DownloadConfirmDialogType.CONFIRM -> DownloadDialogResource(
                        title = stringResource(id = R.string.course_confirm_download),
                        description = stringResource(
                            id = R.string.core_download_confirm_dialog_description,
                            sizeSumString
                        ),
                    )

                    DownloadConfirmDialogType.DOWNLOAD_ON_CELLULAR -> DownloadDialogResource(
                        title = stringResource(id = R.string.core_download_on_cellural),
                        description = stringResource(
                            id = R.string.core_download_on_cellural_dialog_description,
                            sizeSumString
                        ),
                        icon = painterResource(id = R.drawable.core_ic_warning),
                    )

                    DownloadConfirmDialogType.REMOVE -> DownloadDialogResource(
                        title = stringResource(id = R.string.core_download_remove_offline_content),
                        description = stringResource(
                            id = R.string.core_download_remove_dialog_description,
                            sizeSumString
                        )
                    )
                }

                DownloadConfirmDialogView(
                    downloadDialogResource = dialogData,
                    uiState = uiState,
                    dialogType = dialogType,
                    onConfirmClick = {
                        uiState.saveDownloadModels()
                        dismiss()
                        listener?.onConfirmClick()
                    },
                    onRemoveClick = {
                        uiState.removeDownloadModels()
                        dismiss()
                    },
                    onCancelClick = {
                        dismiss()
                        listener?.onCancelClick()
                    }
                )
            }
        }
    }

    companion object {
        const val ARG_DIALOG_TYPE = "dialogType"
        const val ARG_UI_STATE = "uiState"

        fun newInstance(
            dialogType: DownloadConfirmDialogType,
            uiState: DownloadDialogUIState
        ): DownloadConfirmDialogFragment {
            val dialog = DownloadConfirmDialogFragment()
            dialog.arguments = bundleOf(
                ARG_DIALOG_TYPE to dialogType,
                ARG_UI_STATE to uiState
            )
            return dialog
        }
    }
}

@Composable
private fun DownloadConfirmDialogView(
    modifier: Modifier = Modifier,
    uiState: DownloadDialogUIState,
    downloadDialogResource: DownloadDialogResource,
    dialogType: DownloadConfirmDialogType,
    onRemoveClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onCancelClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
            Column(
                modifier = Modifier
                    .heightIn(max = DownloadDialogManager.listMaxSize)
                    .verticalScroll(scrollState)
            ) {
                uiState.downloadDialogItems.forEach {
                    DownloadDialogItem(downloadDialogItem = it)
                }
            }
            Text(
                text = downloadDialogResource.description,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )

            val buttonText: String
            val buttonIcon: ImageVector
            val buttonColor: ComposeColor
            val onClick: () -> Unit
            when (dialogType) {
                DownloadConfirmDialogType.REMOVE -> {
                    buttonText = stringResource(id = R.string.core_remove)
                    buttonIcon = Icons.Rounded.Delete
                    buttonColor = MaterialTheme.appColors.error
                    onClick = onRemoveClick
                }

                else -> {
                    buttonText = stringResource(id = R.string.core_download)
                    buttonIcon = Icons.Outlined.CloudDownload
                    buttonColor = MaterialTheme.appColors.secondaryButtonBackground
                    onClick = onConfirmClick
                }
            }
            OpenEdXButton(
                text = buttonText,
                backgroundColor = buttonColor,
                onClick = onClick,
                content = {
                    IconText(
                        text = buttonText,
                        icon = buttonIcon,
                        color = MaterialTheme.appColors.primaryButtonText,
                        textStyle = MaterialTheme.appTypography.labelLarge
                    )
                }
            )
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.core_cancel),
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
private fun DownloadConfirmDialogViewPreview() {
    OpenEdXTheme {
        DownloadConfirmDialogView(
            downloadDialogResource = DownloadDialogResource(
                title = "Title",
                description = "Description Description Description Description Description Description Description "
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
                sizeSum = 1000000,
                isAllBlocksDownloaded = false,
                isDownloadFailed = false,
                saveDownloadModels = {},
                removeDownloadModels = {},
                fragmentManager = PreviewFragmentManager
            ),
            dialogType = DownloadConfirmDialogType.CONFIRM,
            onConfirmClick = {},
            onRemoveClick = {},
            onCancelClick = {}
        )
    }
}
