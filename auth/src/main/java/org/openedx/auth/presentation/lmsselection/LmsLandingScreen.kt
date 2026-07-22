package org.openedx.auth.presentation.lmsselection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.R as coreR

// Weight of the gap between the welcome block and the action buttons; biases the
// logo/title group toward the upper third of the screen (mirrors the iOS landing).
private const val CONTENT_TO_ACTIONS_WEIGHT = 1.6f

/**
 * The LMS Directory entry point: welcome the learner and offer two ways in —
 * browse/search the catalog ("Find my LMS") or scan a platform's QR code.
 */
@Composable
internal fun LmsLandingScreen(
    onFindClick: () -> Unit,
    onQrClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = coreR.drawable.core_ic_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.appColors.primary),
            modifier = Modifier.height(48.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.auth_lms_welcome_title),
            style = MaterialTheme.appTypography.displaySmall,
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.auth_lms_welcome_subtitle),
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(CONTENT_TO_ACTIONS_WEIGHT))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpenEdXButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.auth_lms_find_button),
                onClick = onFindClick,
            )
            TextButton(onClick = onQrClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(id = R.string.auth_lms_qr_button),
                        style = MaterialTheme.appTypography.labelLarge,
                        color = MaterialTheme.appColors.primary,
                    )
                }
            }
        }
    }
}
