package org.openedx.core.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import org.openedx.core.ui.theme.InstanceThemeManager

/**
 * An image an instance may brand: a remote URL, the name of a bundled drawable, or nothing.
 * Anything that can't be resolved, including a failed download, shows [fallback], so a broken
 * logo URL never leaves an empty space.
 */
@Composable
fun InstanceThemedImage(
    source: String?,
    fallback: Painter,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    val bundledId = rememberBundledDrawable(source)
    when {
        source != null && (source.startsWith("https://") || source.startsWith("http://")) -> AsyncImage(
            model = source,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
        )

        bundledId != 0 -> Image(
            painter = painterResource(bundledId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )

        else -> Image(
            painter = fallback,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/** The selected instance's header background, or the app's own header. */
@Composable
fun InstanceHeaderImage(
    fallback: Painter,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    val theme by InstanceThemeManager.theme.collectAsState()
    InstanceThemedImage(
        source = theme?.headerBackgroundUrl,
        fallback = fallback,
        modifier = modifier,
        contentScale = contentScale,
    )
}

// Instances name bundled logos in configuration, so the resource is looked up by name.
@SuppressLint("DiscouragedApi")
@Composable
private fun rememberBundledDrawable(source: String?): Int {
    val context = LocalContext.current
    return remember(source) {
        if (source.isNullOrBlank() || source.contains("/")) {
            0
        } else {
            context.resources.getIdentifier(source, "drawable", context.packageName)
        }
    }
}
