package org.openedx.core.ui

import android.text.TextUtils
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.ui.theme.appColors

@Composable
fun IAPDialog(
    courseTitle: String,
    formattedPrice: String? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
    onDismiss: () -> Unit,
    iapCallback: (IAPAction) -> Unit = {}
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
                    Text(text = "Product is not available")
                }

                TextUtils.isEmpty(formattedPrice).not() -> {
                    OpenEdXButton(text = "Upgrade now for $formattedPrice",
                        onClick = {
                            iapCallback(IAPAction.START_PURCHASE_FLOW)
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