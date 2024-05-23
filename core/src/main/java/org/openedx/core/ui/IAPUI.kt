package org.openedx.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
fun IAPDialog(
    courseTitle: String,
    formattedPrice: String? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
    onDismiss: () -> Unit,
    onUpgradeNow: () -> Unit = {},
    onGetHelp: () -> Unit = {}
) {
    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        onDismissRequest = onDismiss
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            backgroundColor = MaterialTheme.appColors.background,
            topBar = {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        modifier = Modifier.clickable { onDismiss() },
                        imageVector = Icons.Filled.Close,
                        contentDescription = null
                    )
                }
            },
            bottomBar = {
                Box(modifier = Modifier.padding(all = 16.dp)) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        isError -> {
                            OpenEdXButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.iap_get_help),
                                backgroundColor = MaterialTheme.appColors.material.error,
                                onClick = onGetHelp
                            )
                        }

                        else -> {
                            OpenEdXButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(id = R.string.iap_upgrade_price, formattedPrice.orEmpty()),
                                onClick = onUpgradeNow
                            )
                        }
                    }
                }
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(all = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 32.dp),
                    text = stringResource(id = R.string.iap_upgrade_course, courseTitle),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                CheckmarkView(stringResource(id = R.string.iap_earn_certificate))
                CheckmarkView(stringResource(id = R.string.iap_unlock_access))
                CheckmarkView(stringResource(id = R.string.iap_full_access_course))
            }
        }
    }
}

@Composable
private fun CheckmarkView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.appColors.certificateForeground
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.labelLarge
        )
    }
}

@Preview
@Composable
private fun PreviewIAPDialog() {
    IAPDialog(
        courseTitle = "Course Title with a long name",
        formattedPrice = "218$",
        isLoading = true,
        isError = false,
        onDismiss = {},
    )
}
