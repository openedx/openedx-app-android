package org.openedx.core.presentation.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes

@Composable
fun DefaultDialogBox(
    modifier: Modifier = Modifier,
    onDismissClick: () -> Unit,
    content: @Composable (BoxScope.() -> Unit)
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .noRippleClickable {
                    onDismissClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.appShapes.cardShape)
                    .noRippleClickable {}
                    .background(
                        color = MaterialTheme.appColors.background,
                        shape = MaterialTheme.appShapes.cardShape
                    )
            ) {
                content.invoke(this)
            }
        }
    }
}
