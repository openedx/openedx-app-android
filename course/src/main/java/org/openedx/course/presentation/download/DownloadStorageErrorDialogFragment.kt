package org.openedx.course.presentation.download

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.R
import org.openedx.core.extension.parcelable
import org.openedx.core.extension.toFileSize
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.system.PreviewFragmentManager
import org.openedx.core.system.StorageManager
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.domain.model.DownloadDialogResource

class DownloadStorageErrorDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val uiState = requireArguments().parcelable<DownloadDialogUIState>(ARG_UI_STATE) ?: return@OpenEdXTheme
                val downloadDialogResource = DownloadDialogResource(
                    title = stringResource(id = org.openedx.course.R.string.course_device_storage_full),
                    description = stringResource(id = org.openedx.course.R.string.course_download_device_storage_full_dialog_description),
                    icon = painterResource(id = org.openedx.course.R.drawable.course_ic_error),
                )

                DownloadStorageErrorDialogView(
                    uiState = uiState,
                    downloadDialogResource = downloadDialogResource,
                    onCancelClick = {
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        const val DIALOG_TAG = "DownloadStorageErrorDialogFragment"
        const val ARG_UI_STATE = "uiState"

        fun newInstance(
            uiState: DownloadDialogUIState
        ): DownloadStorageErrorDialogFragment {
            val dialog = DownloadStorageErrorDialogFragment()
            dialog.arguments = bundleOf(
                ARG_UI_STATE to uiState
            )
            return dialog
        }
    }
}

@Composable
private fun DownloadStorageErrorDialogView(
    modifier: Modifier = Modifier,
    uiState: DownloadDialogUIState,
    downloadDialogResource: DownloadDialogResource,
    onCancelClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
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
                Text(
                    text = downloadDialogResource.title,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textDark
                )
            }
            Column {
                uiState.downloadDialogItems.forEach {
                    DownloadDialogItem(title = it.title, size = it.size.toFileSize(0, false))
                }
            }
            StorageBar(
                usedSpace = StorageManager.getTotalStorage() - StorageManager.getFreeStorage(),
                freeSpace = StorageManager.getFreeStorage(),
                totalSpace = StorageManager.getTotalStorage(),
                requiredSpace = (StorageManager.getFreeStorage() * 1.3f).toLong()
            )
            Text(
                text = downloadDialogResource.description,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
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

@Composable
private fun StorageBar(
    usedSpace: Long,
    freeSpace: Long,
    totalSpace: Long,
    requiredSpace: Long
) {
    val usedPercentage = (totalSpace + requiredSpace - freeSpace) / totalSpace.toFloat()
    val requiredPercentage = (requiredSpace - freeSpace) / totalSpace.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(androidx.compose.ui.graphics.Color.Gray)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .weight(usedPercentage)
                    .fillMaxHeight()
            ) {
                drawRoundRect(color = androidx.compose.ui.graphics.Color.Gray)
            }
            Canvas(
                modifier = Modifier
                    .weight(requiredPercentage)
                    .fillMaxHeight()
            ) {
                drawRoundRect(color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(
                org.openedx.course.R.string.course_used_free_storage,
                usedSpace.toFileSize(0, false),
                freeSpace.toFileSize(0, false)
            ),
            style = MaterialTheme.typography.body1.copy(fontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = requiredSpace.toFileSize(0, false),
            style = MaterialTheme.typography.body1.copy(
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 14.sp
            )
        )
    }
}

@Preview
@Composable
private fun DownloadStorageErrorDialogViewPreview() {
    OpenEdXTheme {
        DownloadStorageErrorDialogView(
            downloadDialogResource = DownloadDialogResource(
                title = "Title",
                description = "Description Description Description Description Description Description Description ",
                icon = painterResource(id = org.openedx.course.R.drawable.course_ic_error)
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
                fragmentManager = PreviewFragmentManager,
                removeDownloadModels = {},
                saveDownloadModels = {}
            ),
            onCancelClick = {}
        )
    }
}