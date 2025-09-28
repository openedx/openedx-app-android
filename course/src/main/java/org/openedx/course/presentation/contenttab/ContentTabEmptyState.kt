package org.openedx.course.presentation.contenttab

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R

@Composable
fun ContentTabEmptyState(
    message: String,
    onReturnToCourseClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Icon(
                modifier = Modifier
                    .size(120.dp),
                painter = painterResource(R.drawable.course_ic_warning),
                contentDescription = null,
                tint = MaterialTheme.appColors.textFieldHint
            )
            Spacer(Modifier.height(24.dp))
        }
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = message,
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OpenEdXButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textColor = MaterialTheme.appColors.secondaryButtonText,
            backgroundColor = MaterialTheme.appColors.secondaryButtonBackground,
            onClick = onReturnToCourseClick
        ) {
            IconText(
                text = stringResource(id = R.string.course_return_to_course_home),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                color = MaterialTheme.appColors.secondaryButtonText,
                textStyle = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun CourseContentAllEmptyState(
    onReturnToCourseClick: () -> Unit
) {
    ContentTabEmptyState(
        message = stringResource(id = org.openedx.core.R.string.core_no_course_content),
        onReturnToCourseClick = onReturnToCourseClick
    )
}

@Composable
fun CourseContentVideoEmptyState(
    onReturnToCourseClick: () -> Unit
) {
    ContentTabEmptyState(
        message = stringResource(id = org.openedx.core.R.string.core_no_videos),
        onReturnToCourseClick = onReturnToCourseClick
    )
}

@Composable
fun CourseContentAssignmentEmptyState(
    onReturnToCourseClick: () -> Unit
) {
    ContentTabEmptyState(
        message = stringResource(id = org.openedx.core.R.string.core_no_assignments),
        onReturnToCourseClick = onReturnToCourseClick
    )
}

@Preview
@Composable
private fun CourseContentAllEmptyStatePreview() {
    OpenEdXTheme {
        CourseContentAllEmptyState({})
    }
}
