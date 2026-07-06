package org.openedx.profile.presentation.reportlms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openedx.core.lmsdirectory.ReportCategory
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.R

private const val MAX_SCREENSHOT_DIMENSION = 1280
private const val SCREENSHOT_JPEG_QUALITY = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLmsBottomSheet(
    viewModel: ReportLmsViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
    ) {
        if (state.submitted) {
            SuccessContent(lmsTitle = viewModel.lmsTitle)
        } else {
            FormContent(
                state = state,
                lmsTitle = viewModel.lmsTitle,
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
    lmsTitle: String,
    onCategoryChanged: (ReportCategory) -> Unit,
    onMessageChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onAttachClick: () -> Unit,
    onRemoveScreenshot: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.profile_report_lms_title),
                style = MaterialTheme.appTypography.titleLarge,
                color = MaterialTheme.appColors.textPrimary,
            )
            Text(
                text = lmsTitle,
                style = MaterialTheme.appTypography.bodyLarge,
                color = MaterialTheme.appColors.textSecondary,
            )
        }

        SectionLabel(stringResource(id = R.string.profile_report_lms_whats_wrong))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportCategory.entries.forEach { category ->
                CategoryRow(
                    category = category,
                    selected = state.category == category,
                    onSelect = { onCategoryChanged(category) },
                )
            }
        }

        SectionLabel(stringResource(id = R.string.profile_report_lms_tell_us_more))
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
                )
            },
            colors = reportTextFieldColors(),
        )

        SectionLabel(stringResource(id = R.string.profile_report_lms_screenshot))
        val preview = state.screenshotPreview
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            TextButton(onClick = onRemoveScreenshot) {
                Text(
                    text = stringResource(id = R.string.profile_report_lms_remove_screenshot),
                    color = MaterialTheme.appColors.error,
                )
            }
        } else {
            TextButton(onClick = onAttachClick) {
                Text(
                    text = stringResource(id = R.string.profile_report_lms_attach_screenshot),
                    color = MaterialTheme.appColors.primary,
                )
            }
        }

        SectionLabel(stringResource(id = R.string.profile_report_lms_email))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.email,
            onValueChange = onEmailChanged,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.profile_report_lms_email_hint),
                    color = MaterialTheme.appColors.textFieldHint,
                )
            },
            colors = reportTextFieldColors(),
        )

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
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.appColors.primaryButtonText,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: ReportCategory, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = stringResource(id = category.titleRes()),
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.textPrimary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SuccessContent(lmsTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.appColors.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(id = R.string.profile_report_lms_thanks),
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textPrimary,
        )
        Text(
            text = stringResource(id = R.string.profile_report_lms_thanks_body, lmsTitle),
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.textSecondary,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.appTypography.labelLarge,
        color = MaterialTheme.appColors.textSecondary,
    )
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
