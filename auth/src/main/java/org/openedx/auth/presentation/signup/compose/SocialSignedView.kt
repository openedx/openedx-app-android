package org.openedx.auth.presentation.signup.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openedx.auth.R
import org.openedx.auth.data.model.AuthType
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.R as coreR

@Composable
internal fun SocialSignedView(authType: AuthType) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.appColors.secondary,
                shape = MaterialTheme.appShapes.buttonShape
            )
            .padding(20.dp)
    ) {
        Text(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            text = stringResource(
                id = R.string.auth_social_signed_title,
                authType.methodName
            )
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(
                id = R.string.auth_social_signed_desc,
                stringResource(id = coreR.string.app_name)
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSocialSignedView() {
    SocialSignedView(AuthType.GOOGLE)
}
