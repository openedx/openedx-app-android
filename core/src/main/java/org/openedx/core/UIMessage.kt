package org.openedx.core

import androidx.compose.material.SnackbarDuration

open class UIMessage {
    class SnackBarMessage(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Long,
    ) : UIMessage()

    class ToastMessage(val message: String) : UIMessage()
}
