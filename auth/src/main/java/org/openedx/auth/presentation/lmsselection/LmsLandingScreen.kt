package org.openedx.auth.presentation.lmsselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

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
        Text(
            text = stringResource(id = R.string.auth_lms_welcome_title),
            style = MaterialTheme.appTypography.displaySmall,
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(top = 12.dp))
        Text(
            text = stringResource(id = R.string.auth_lms_welcome_subtitle),
            style = MaterialTheme.appTypography.bodyLarge,
            color = MaterialTheme.appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
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
                Text(
                    text = stringResource(id = R.string.auth_lms_qr_button),
                    style = MaterialTheme.appTypography.labelLarge,
                    color = MaterialTheme.appColors.primary,
                )
            }
        }
    }
}
