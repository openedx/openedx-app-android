package org.openedx.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun UnlockingAccessView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.appColors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            colorFilter = ColorFilter.tint(MaterialTheme.appColors.progressBarBackgroundColor),
            painter = painterResource(id = R.drawable.core_ic_rocket_launch),
            contentDescription = null,
        )
        
        val annotatedString = buildAnnotatedString {
            append(stringResource(id = R.string.iap_unloacking_text))
            append("\n")
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.appColors.primary
                )
            ) {
                append(stringResource(id = R.string.iap_full_access_text))
            }
            append("\n")
            append(stringResource(id = R.string.iap_your_course_text))
        }

        Text(
            modifier = Modifier.padding(vertical = 24.dp),
            text = annotatedString,
            textAlign = TextAlign.Center,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleLarge,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
        }
    }
}

@Preview
@Composable
private fun PreviewUnlockingAccessView() {
    UnlockingAccessView()
}
