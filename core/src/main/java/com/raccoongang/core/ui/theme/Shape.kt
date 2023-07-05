package com.raccoongang.core.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class AppShapes(
    val material: Shapes,
    val buttonShape: CornerBasedShape,
    val navigationButtonShape: CornerBasedShape,
    val textFieldShape: CornerBasedShape,
    val screenBackgroundShape: CornerBasedShape,
    val cardShape: CornerBasedShape,
    val screenBackgroundShapeFull: CornerBasedShape,
    val courseImageShape: CornerBasedShape,
    val dialogShape: CornerBasedShape
)

internal val LocalShapes = staticCompositionLocalOf {
    AppShapes(
        material = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        ),
        buttonShape = RoundedCornerShape(8.dp),
        navigationButtonShape = RoundedCornerShape(8.dp),
        textFieldShape = RoundedCornerShape(CornerSize(8.dp)),
        screenBackgroundShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        cardShape = RoundedCornerShape(12.dp),
        screenBackgroundShapeFull = RoundedCornerShape(24.dp),
        courseImageShape = RoundedCornerShape(8.dp),
        dialogShape = RoundedCornerShape(24.dp)
    )
}

val MaterialTheme.appShapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current
