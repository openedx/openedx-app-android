package org.openedx.core.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.R
import org.openedx.core.lmsdirectory.LmsThemeController

/**
 * Header image for the auth screens (sign-in / register / reset password). When the LMS
 * Directory feature has a selected platform with a custom login background, shows that
 * image; otherwise the stock gradient header. Mirrors iOS's `LmsHeaderBackground`, so a
 * branded platform looks the same across sign-in, register, reset and the settings screens.
 */
@Composable
fun LmsHeaderImage(modifier: Modifier = Modifier) {
    val backgroundUrl = LmsThemeController.loginBackgroundUrl
    if (!backgroundUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(backgroundUrl)
                .placeholder(R.drawable.core_top_header)
                .error(R.drawable.core_top_header)
                .crossfade(true)
                .build(),
            modifier = modifier,
            contentScale = ContentScale.FillBounds,
            contentDescription = null,
        )
    } else {
        Image(
            modifier = modifier,
            painter = painterResource(id = R.drawable.core_top_header),
            contentScale = ContentScale.FillBounds,
            contentDescription = null,
        )
    }
}
