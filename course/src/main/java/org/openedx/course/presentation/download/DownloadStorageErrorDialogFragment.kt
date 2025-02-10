package org.openedx.course.presentation.download

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.system.StorageManager
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.domain.model.DownloadDialogResource
import org.openedx.course.presentation.download.DownloadDialogManager.Companion.DOWNLOAD_SIZE_FACTOR
import org.openedx.course.presentation.download.DownloadStorageErrorDialogFragment.Companion.STORAGE_BAR_MIN_SIZE
import org.openedx.foundation.extension.parcelable
import org.openedx.foundation.extension.toFileSize
import org.openedx.foundation.system.PreviewFragmentManager
import org.openedx.core.R as coreR

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
                    title = stringResource(id = R.string.course_device_storage_full),
                    description = stringResource(id = R.string.course_download_device_storage_full_dialog_description),
                    icon = painterResource(id = R.drawable.course_ic_error),
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
        const val STORAGE_BAR_MIN_SIZE = 0.1f

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
                AutoSizeText(
                    text = downloadDialogResource.title,
                    style = MaterialTheme.appTypography.titleLarge,
                    color = MaterialTheme.appColors.textDark,
                    minSize = MaterialTheme.appTypography.titleLarge.fontSize.value - 1
                )
            }
            Column {
                uiState.downloadDialogItems.forEach {
                    DownloadDialogItem(downloadDialogItem = it.copy(size = it.size * DOWNLOAD_SIZE_FACTOR))
                }
            }
            StorageBar(
                freeSpace = StorageManager.getFreeStorage(),
                totalSpace = StorageManager.getTotalStorage(),
                requiredSpace = uiState.sizeSum * DOWNLOAD_SIZE_FACTOR
            )
            Text(
                text = downloadDialogResource.description,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
            OpenEdXOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = coreR.string.core_cancel),
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
    freeSpace: Long,
    totalSpace: Long,
    requiredSpace: Long
) {
    val cornerRadius = 2.dp
    val boxPadding = 1.dp
    val usedSpace = totalSpace - freeSpace
    val freePercentage = freeSpace / requiredSpace.toFloat() + STORAGE_BAR_MIN_SIZE
    val reqPercentage = (requiredSpace - freeSpace) / requiredSpace.toFloat() + STORAGE_BAR_MIN_SIZE

    val animReqPercentage = remember { Animatable(Float.MIN_VALUE) }
    LaunchedEffect(Unit) {
        animReqPercentage.animateTo(
            targetValue = reqPercentage,
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearOutSlowInEasing
            )
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.appColors.background)
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    2.dp,
                    MaterialTheme.appColors.cardViewBorder,
                    RoundedCornerShape(cornerRadius * 2)
                )
                .padding(2.dp)
                .background(MaterialTheme.appColors.background),
        ) {
            Box(
                modifier = Modifier
                    .weight(freePercentage)
                    .fillMaxHeight()
                    .padding(top = boxPadding, bottom = boxPadding, start = boxPadding, end = boxPadding / 2)
                    .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
                    .background(MaterialTheme.appColors.cardViewBorder)
            )
            Box(
                modifier = Modifier
                    .weight(animReqPercentage.value)
                    .fillMaxHeight()
                    .padding(top = boxPadding, bottom = boxPadding, end = boxPadding, start = boxPadding / 2)
                    .clip(RoundedCornerShape(topEnd = cornerRadius, bottomEnd = cornerRadius))
                    .background(MaterialTheme.appColors.error)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    R.string.course_used_free_storage,
                    usedSpace.toFileSize(1, false),
                    freeSpace.toFileSize(1, false)
                ),
                style = MaterialTheme.appTypography.labelSmall,
                color = MaterialTheme.appColors.textFieldHint,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = requiredSpace.toFileSize(1, false),
                style = MaterialTheme.appTypography.labelSmall,
                color = MaterialTheme.appColors.error,
            )
        }
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
            onCancelClick = {}
        )
    }
}
