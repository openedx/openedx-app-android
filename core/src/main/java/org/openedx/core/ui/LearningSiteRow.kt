package org.openedx.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openedx.core.instance.Instance
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import java.net.URI
import java.util.Locale

/**
 * One learning site: its logo, host and name, with an optional trailing control. Used by the
 * Learning Sites screen, the sign-in screen and Settings.
 */
@Composable
fun LearningSiteRow(
    instance: Instance,
    modifier: Modifier = Modifier,
    logoSize: Int = 50,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LearningSiteLogo(instance = instance, size = logoSize)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = instance.host,
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = instance.name,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

/**
 * The site's own logo; until it loads, or when the site has none, a monogram of its name.
 * The app logo is drawn for a dark header and would disappear on a light row.
 */
@Composable
fun LearningSiteLogo(instance: Instance, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.appColors.background)
            .border(1.dp, MaterialTheme.appColors.cardViewBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = instance.name.firstOrNull { it.isLetterOrDigit() }
                ?.uppercase(Locale.getDefault())
                .orEmpty(),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.primary,
        )
        if (instance.logoUrl != null) {
            InstanceThemedImage(
                source = instance.logoUrl,
                fallback = ColorPainter(Color.Transparent),
                modifier = Modifier.size(size.dp),
            )
        }
    }
}

/** `lms.example.com`, without scheme or port; the configured URL if unparsable. */
val Instance.host: String
    get() = runCatching { URI(apiHostUrl).host }.getOrNull() ?: apiHostUrl
