package org.openedx.core.ui

import android.text.TextUtils
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.openedx.core.ui.theme.appColors

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
    val alertTitle = "Upgrade $courseTitle"
    AlertDialog(
        title = { Text(text = alertTitle) },
        confirmButton = {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                }

                isError -> {
                    OpenEdXButton(text = "Get Help",
                        onClick = {
                            onGetHelp()
                        })
                }

                TextUtils.isEmpty(formattedPrice).not() -> {
                    OpenEdXButton(text = "Upgrade now for $formattedPrice",
                        onClick = {
                            onUpgradeNow()
                        })
                }
            }
        },
        dismissButton = {
            OpenEdXButton(text = "Cancel",
                onClick = {
                    onDismiss()
                })
        },
        onDismissRequest = {
            onDismiss()
        }
    )
}