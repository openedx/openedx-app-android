package org.openedx.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
internal fun CheckboxField(
    text: String,
    defaultValue: Boolean,
    onValueChanged: (Boolean) -> Unit
) {
    var checkedState by remember { mutableStateOf(defaultValue) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checkedState,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.appColors.primary,
                uncheckedColor = MaterialTheme.appColors.primary
            ),
            onCheckedChange = {
                checkedState = it
                onValueChanged(it)
            }
        )
        Text(
            modifier = Modifier.noRippleClickable {
                checkedState = !checkedState
                onValueChanged(checkedState)
            },
            text = text,
            style = MaterialTheme.appTypography.bodySmall,
        )
    }
}

@Preview(widthDp = 375, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(widthDp = 375, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CheckboxFieldPreview() {
    OpenEdXTheme {
        CheckboxField(
            text = "Test",
            defaultValue = true,
        ) {}
    }
}
