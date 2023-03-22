package com.raccoongang.core

import androidx.compose.material.SnackbarDuration

sealed class UIMessage {
    class SnackBarMessage(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Long,
    ) : UIMessage()

    class ToastMessage(val message: String) : UIMessage()
}