package org.openedx.core.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

data class AppShapes(
    val material: Shapes,
    val buttonShape: CornerBasedShape,
    val navigationButtonShape: CornerBasedShape,
    val textFieldShape: CornerBasedShape,
    val screenBackgroundShape: CornerBasedShape,
    val cardShape: CornerBasedShape,
    val sectionCardShape: CornerBasedShape,
    val screenBackgroundShapeFull: CornerBasedShape,
    val courseImageShape: CornerBasedShape,
    val dialogShape: CornerBasedShape,
    val videoPreviewShape: CornerBasedShape,
)

val MaterialTheme.appShapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current
