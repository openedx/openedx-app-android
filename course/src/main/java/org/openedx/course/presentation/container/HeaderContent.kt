package org.openedx.course.presentation.container

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography

@Composable
internal fun ExpandedHeaderContent(
    modifier: Modifier = Modifier,
    org: String,
    courseTitle: String
) {
    val windowSize = rememberWindowSize()
    val horizontalPadding = if (!windowSize.isTablet){
        24.dp
    } else {
        98.dp
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.appColors.textDark,
            text = org,
            style = MaterialTheme.appTypography.labelLarge
        )
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.appColors.textDark,
            text = courseTitle,
            style = MaterialTheme.appTypography.titleLarge
        )
    }
}

@Composable
internal fun CollapsedHeaderContent(
    modifier: Modifier = Modifier,
    courseTitle: String
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsInset()
            .padding(top = 4.dp),
        text = courseTitle,
        color = MaterialTheme.appColors.textDark,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.appTypography.titleSmall,
        maxLines = 1
    )
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun ExpandedHeaderContentPreview() {
    ExpandedHeaderContent(
        modifier = Modifier.fillMaxWidth(),
        org = "organization",
        courseTitle = "Course title"
    )
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun CollapsedHeaderContentPreview() {
    CollapsedHeaderContent(
        modifier = Modifier.fillMaxWidth(),
        courseTitle = "Course title"
    )
}