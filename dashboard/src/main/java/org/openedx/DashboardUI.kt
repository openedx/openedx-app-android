package org.openedx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors

@Composable
fun Lock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Icon(
            modifier = Modifier
                .size(32.dp)
                .padding(top = 8.dp, end = 8.dp)
                .background(
                    color = MaterialTheme.appColors.onPrimary.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.dp)
                .align(Alignment.TopEnd),
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.appColors.onSurface
        )
    }
}

@Preview
@Composable
private fun LockPreview() {
    OpenEdXTheme {
        Lock()
    }
}
