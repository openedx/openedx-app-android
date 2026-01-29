package org.openedx.core.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal val LocalShapes = staticCompositionLocalOf {
    AppShapes(
        material3 = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        buttonShape = RoundedCornerShape(8.dp),
        navigationButtonShape = RoundedCornerShape(8.dp),
        textFieldShape = RoundedCornerShape(CornerSize(8.dp)),
        screenBackgroundShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        cardShape = RoundedCornerShape(12.dp),
        screenBackgroundShapeFull = RoundedCornerShape(24.dp),
        courseImageShape = RoundedCornerShape(8.dp),
        dialogShape = RoundedCornerShape(24.dp),
        sectionCardShape = RoundedCornerShape(6.dp),
        videoPreviewShape = RoundedCornerShape(8.dp),
    )
}
