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
        buttonShape = RoundedCornerShape(0.dp),
        navigationButtonShape = RoundedCornerShape(0.dp),
        textFieldShape = RoundedCornerShape(CornerSize(0.dp)),
        screenBackgroundShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        cardShape = RoundedCornerShape(0.dp),
        screenBackgroundShapeFull = RoundedCornerShape(24.dp),
        courseImageShape = RoundedCornerShape(0.dp),
        dialogShape = RoundedCornerShape(24.dp)
    )
}
