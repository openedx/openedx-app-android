package org.openedx.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.foundation.extension.tagId

@Composable
private fun OpenEdXButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color,
    backgroundColor: Color,
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        modifier = Modifier
            .testTag("btn_${text.tagId()}")
            .then(modifier)
            .height(42.dp),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor
        ),
        enabled = enabled,
        elevation = elevation,
        onClick = onClick
    ) {
        if (content == null) {
            Text(
                modifier = Modifier.testTag("txt_${text.tagId()}"),
                text = text,
                color = textColor,
                style = MaterialTheme.appTypography.labelLarge
            )
        } else {
            content()
        }
    }
}

@Composable
private fun OpenEdXOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color,
    borderColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(
        modifier = Modifier
            .testTag("btn_${text.tagId()}")
            .then(modifier)
            .height(42.dp),
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.appShapes.buttonShape,
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor)
    ) {
        if (content == null) {
            Text(
                modifier = Modifier.testTag("txt_${text.tagId()}"),
                text = text,
                style = MaterialTheme.appTypography.labelLarge,
                color = textColor
            )
        } else {
            content()
        }
    }
}

@Composable
fun OpenEdXPrimaryButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.primaryButtonText,
    backgroundColor: Color = MaterialTheme.appColors.primaryButtonBackground,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OpenEdXButton(
        modifier = modifier,
        text = text,
        enabled = enabled,
        textColor = textColor,
        backgroundColor = backgroundColor,
        onClick = onClick,
        content = content
    )
}

@Composable
fun OpenEdXPrimaryOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.primaryButtonBorderedText,
    borderColor: Color = MaterialTheme.appColors.primaryButtonBorder,
    backgroundColor: Color = MaterialTheme.appColors.primaryButtonBorderedBackground,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OpenEdXOutlinedButton(
        modifier = modifier,
        text = text,
        enabled = enabled,
        textColor = textColor,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        onClick = onClick,
        content = content
    )
}

@Composable
fun OpenEdXSecondaryButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.secondaryButtonText,
    backgroundColor: Color = MaterialTheme.appColors.secondaryButtonBackground,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OpenEdXButton(
        modifier = modifier,
        text = text,
        enabled = enabled,
        textColor = textColor,
        backgroundColor = backgroundColor,
        onClick = onClick,
        content = content
    )
}

@Composable
fun OpenEdXSecondaryOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.secondaryButtonBorderedText,
    borderColor: Color = MaterialTheme.appColors.secondaryButtonBorder,
    backgroundColor: Color = MaterialTheme.appColors.secondaryButtonBorderedBackground,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OpenEdXOutlinedButton(
        modifier = modifier,
        text = text,
        enabled = enabled,
        textColor = textColor,
        borderColor = borderColor,
        backgroundColor = backgroundColor,
        onClick = onClick,
        content = content
    )
}

@Composable
fun OpenEdXErrorOutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.appColors.error,
    borderColor: Color = MaterialTheme.appColors.error,
    onClick: () -> Unit,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    OpenEdXOutlinedButton(
        modifier = modifier,
        text = text,
        enabled = enabled,
        textColor = textColor,
        borderColor = borderColor,
        backgroundColor = Color.Transparent,
        onClick = onClick,
        content = content
    )
}

@Composable
fun DefaultTextButton(
    isEnabled: Boolean = true,
    text: String,
    onClick: () -> Unit
) {
    val textColor: Color
    val backgroundColor: Color
    if (isEnabled) {
        textColor = MaterialTheme.appColors.primaryButtonText
        backgroundColor = MaterialTheme.appColors.primaryButtonBackground
    } else {
        textColor = MaterialTheme.appColors.inactiveButtonText
        backgroundColor = MaterialTheme.appColors.inactiveButtonBackground
    }

    Button(
        modifier = Modifier
            .testTag("btn_primary")
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        enabled = isEnabled,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun TransparentTextButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .testTag("btn_secondary")
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.testTag("txt_secondary"),
            color = MaterialTheme.appColors.textAccent,
            style = MaterialTheme.appTypography.labelLarge,
            text = text
        )
    }
}
