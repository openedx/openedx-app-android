package org.openedx.core.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal val LocalShapes = staticCompositionLocalOf {
    AppShapes(
        material = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        ),
        buttonShape = RoundedCornerShape(CornerSize(8.dp)),
        navigationButtonShape = RoundedCornerShape(8.dp),
        textFieldShape = RoundedCornerShape(CornerSize(8.dp)),
        screenBackgroundShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        cardShape = RoundedCornerShape(12.dp),
        screenBackgroundShapeFull = RoundedCornerShape(24.dp),
        courseImageShape = RoundedCornerShape(8.dp),
        dialogShape = RoundedCornerShape(24.dp)
    )
}
