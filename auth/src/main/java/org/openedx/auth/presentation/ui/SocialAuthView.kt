package org.openedx.auth.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.auth.data.model.AuthType
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors

@Composable
internal fun SocialAuthView(
    modifier: Modifier = Modifier,
    isGoogleAuthEnabled: Boolean = true,
    isFacebookAuthEnabled: Boolean = true,
    isMicrosoftAuthEnabled: Boolean = true,
    isSignIn: Boolean = false,
    onEvent: (AuthType) -> Unit,
) {
    Column(modifier = modifier) {
        if (isGoogleAuthEnabled) {
            val stringRes = if (isSignIn) {
                R.string.auth_google
            } else {
                R.string.auth_continue_google
            }
            OpenEdXOutlinedButton(
                modifier = Modifier
                    .testTag("btn_google_auth")
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                backgroundColor = MaterialTheme.appColors.authGoogleButtonBackground,
                borderColor = MaterialTheme.appColors.primary,
                textColor = Color.Unspecified,
                onClick = {
                    onEvent(AuthType.GOOGLE)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.auth_ic_google),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                    Text(
                        modifier = Modifier
                            .testTag("txt_google_auth")
                            .padding(start = 10.dp),
                        text = stringResource(id = stringRes),
                        color = MaterialTheme.appColors.primaryButtonBorderedText,
                    )
                }
            }
        }
        if (isFacebookAuthEnabled) {
            val stringRes = if (isSignIn) {
                R.string.auth_facebook
            } else {
                R.string.auth_continue_facebook
            }
            OpenEdXButton(
                modifier = Modifier
                    .testTag("btn_facebook_auth")
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                backgroundColor = MaterialTheme.appColors.authFacebookButtonBackground,
                onClick = {
                    onEvent(AuthType.FACEBOOK)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.auth_ic_facebook),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primaryButtonText,
                    )
                    Text(
                        modifier = Modifier
                            .testTag("txt_facebook_auth")
                            .padding(start = 10.dp),
                        color = MaterialTheme.appColors.primaryButtonText,
                        text = stringResource(id = stringRes)
                    )
                }
            }
        }
        if (isMicrosoftAuthEnabled) {
            val stringRes = if (isSignIn) {
                R.string.auth_microsoft
            } else {
                R.string.auth_continue_microsoft
            }
            OpenEdXButton(
                modifier = Modifier
                    .testTag("btn_microsoft_auth")
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                backgroundColor = MaterialTheme.appColors.authMicrosoftButtonBackground,
                onClick = {
                    onEvent(AuthType.MICROSOFT)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.auth_ic_microsoft),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                    Text(
                        modifier = Modifier
                            .testTag("txt_microsoft_auth")
                            .padding(start = 10.dp),
                        color = MaterialTheme.appColors.primaryButtonText,
                        text = stringResource(id = stringRes)
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SocialAuthViewPreview() {
    OpenEdXTheme {
        SocialAuthView {}
    }
}
