package org.openedx.core.presentation.global

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun InDevelopmentScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.secondary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.testTag("txt_in_development"),
            text = "Will be available soon",
            style = MaterialTheme.appTypography.headlineMedium
        )
    }
}