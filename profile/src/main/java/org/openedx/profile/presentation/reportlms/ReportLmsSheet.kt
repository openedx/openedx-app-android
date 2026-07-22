package org.openedx.profile.presentation.reportlms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openedx.core.lmsdirectory.ReportCategory
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.R

private const val MAX_SCREENSHOT_DIMENSION = 1280
private const val SCREENSHOT_JPEG_QUALITY = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLmsSheet(
    viewModel: ReportLmsViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Present as a bottom sheet that slides up from the bottom — matches iOS's
    // native "Report a problem" sheet. skipPartiallyExpanded so the tall form
    // opens fully expanded instead of at a half-height detent.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeSampledBitmap(context, uri)
            if (bitmap != null) {
                val base64 = bitmap.toBase64Jpeg()
                viewModel.onScreenshotPicked(base64, bitmap.asImageBitmap())
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.appColors.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        if (state.submitted) {
            SuccessContent(host = viewModel.displayHost, onDismiss = onDismiss)
        } else {
            FormContent(
                state = state,
                subtitle = viewModel.displayHost,
                onCategoryChanged = viewModel::onCategoryChanged,
                onMessageChanged = viewModel::onMessageChanged,
                onEmailChanged = viewModel::onEmailChanged,
                onAttachClick = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveScreenshot = viewModel::onScreenshotRemoved,
                onSubmit = viewModel::submit,
            )
        }
    }
}

@Composable
private fun FormContent(
    state: ReportLmsUiState,
    subtitle: String,
    onCategoryChanged: (ReportCategory) -> Unit,
    onMessageChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveScreenshot: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.profile_report_lms_title),
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.appTypography.bodyLarge,
                    color = MaterialTheme.appColors.textSecondary,
                )
            }
        }

        ReportSection(title = stringResource(id = R.string.profile_report_lms_section_whats_wrong)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportCategory.entries.forEach { category ->
                    CategoryRow(
                        category = category,
                        selected = state.category == category,
                        onSelect = { onCategoryChanged(category) },
                    )
                }
            }
        }

        ReportSection(title = stringResource(id = R.string.profile_report_lms_section_tell_us_more)) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                value = state.message,
                onValueChange = onMessageChanged,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.profile_report_lms_describe),
                        color = MaterialTheme.appColors.textFieldHint,
                        style = MaterialTheme.appTypography.bodyLarge,
                    )
                },
                textStyle = MaterialTheme.appTypography.bodyLarge,
                colors = reportTextFieldColors(),
            )
        }

        ReportSection(title = stringResource(id = R.string.profile_report_lms_section_screenshot)) {
            val preview = state.screenshotPreview
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                TextButton(onClick = onRemoveScreenshot) {
                    Text(
                        text = stringResource(id = R.string.profile_report_lms_remove_screenshot),
                        color = MaterialTheme.appColors.error,
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAttachClick() },
                    shape = MaterialTheme.appShapes.textFieldShape,
                    color = MaterialTheme.appColors.textFieldBackground,
                    border = BorderStroke(1.dp, MaterialTheme.appColors.textFieldBorder.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.primary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(id = R.string.profile_report_lms_attach_screenshot),
                            style = MaterialTheme.appTypography.bodyLarge,
                            color = MaterialTheme.appColors.primary,
                        )
                    }
                }
            }
        }

        ReportSection(title = stringResource(id = R.string.profile_report_lms_section_email)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.email,
                onValueChange = onEmailChanged,
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.profile_report_lms_email_hint),
                        color = MaterialTheme.appColors.textFieldHint,
                        style = MaterialTheme.appTypography.bodyLarge,
                    )
                },
                textStyle = MaterialTheme.appTypography.bodyLarge,
                colors = reportTextFieldColors(),
            )
        }

        if (!state.error.isNullOrEmpty()) {
            Text(
                text = state.error,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.error,
            )
        }

        OpenEdXButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
            backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
            textColor = MaterialTheme.appColors.primaryButtonText,
            onClick = onSubmit,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = stringResource(id = R.string.profile_report_lms_send),
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.labelLarge,
                )
                if (state.submitting) {
                    Spacer(modifier = Modifier.size(12.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.appColors.primaryButtonText,
                    )
                }
            }
        }
    }
}

/** Labelled section (grey title above content) — mirrors iOS's `section(title:)`. */
@Composable
private fun ReportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textSecondary,
        )
        content()
    }
}

@Composable
private fun CategoryRow(category: ReportCategory, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = MaterialTheme.appShapes.textFieldShape,
        color = if (selected) {
            MaterialTheme.appColors.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.appColors.textFieldBackground
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.appColors.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.appColors.textFieldBorder.copy(alpha = 0.4f)
            }
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = category.titleRes()),
                style = MaterialTheme.appTypography.bodyLarge,
                color = MaterialTheme.appColors.textPrimary,
            )
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.appColors.primary else MaterialTheme.appColors.textSecondary,
            )
        }
    }
}

@Composable
private fun SuccessContent(host: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.appColors.primary,
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = stringResource(id = R.string.profile_report_lms_thanks),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
        )
        Text(
            text = stringResource(id = R.string.profile_report_lms_thanks_body, host),
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        OpenEdXButton(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
            textColor = MaterialTheme.appColors.primaryButtonText,
            onClick = onDismiss,
        ) {
            Text(
                text = stringResource(id = org.openedx.core.R.string.core_ok),
                color = MaterialTheme.appColors.primaryButtonText,
                style = MaterialTheme.appTypography.labelLarge,
            )
        }
    }
}

@Composable
private fun reportTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.appColors.textFieldText,
    unfocusedTextColor = MaterialTheme.appColors.textFieldText,
    focusedContainerColor = MaterialTheme.appColors.textFieldBackground,
    unfocusedContainerColor = MaterialTheme.appColors.textFieldBackground,
    focusedBorderColor = MaterialTheme.appColors.textFieldBorder,
    unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
    cursorColor = MaterialTheme.appColors.primary,
)

private fun ReportCategory.titleRes(): Int = when (this) {
    ReportCategory.INAPPROPRIATE -> R.string.profile_report_lms_category_inappropriate
    ReportCategory.SCAM -> R.string.profile_report_lms_category_scam
    ReportCategory.IMPERSONATION -> R.string.profile_report_lms_category_impersonation
    ReportCategory.SPAM -> R.string.profile_report_lms_category_spam
    ReportCategory.BROKEN -> R.string.profile_report_lms_category_broken
    ReportCategory.OTHER -> R.string.profile_report_lms_category_other
}

/** Decode a picked image, downscaled so its largest side is ~[MAX_SCREENSHOT_DIMENSION]px. */
private fun decodeSampledBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val largest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (largest / sample > MAX_SCREENSHOT_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }.getOrNull()
}

private fun Bitmap.toBase64Jpeg(): String {
    val stream = java.io.ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, SCREENSHOT_JPEG_QUALITY, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}
