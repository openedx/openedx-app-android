package org.openedx.profile.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.R
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.manage_account.compose.mockAccount

@Composable
fun ProfileTopic(account: Account) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val profileImage = if (account.profileImage.hasImage) {
            account.profileImage.imageUrlFull
        } else {
            R.drawable.core_ic_default_profile_picture
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profileImage)
                .error(R.drawable.core_ic_default_profile_picture)
                .placeholder(R.drawable.core_ic_default_profile_picture)
                .build(),
            contentDescription = stringResource(
                id = R.string.core_accessibility_user_profile_image,
                account.username
            ),
            modifier = Modifier
                .testTag("img_profile")
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (account.name.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .testTag("txt_profile_name")
                        .fillMaxWidth(),
                    text = account.name,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleLarge
                )
            }
            Text(
                modifier = Modifier
                    .testTag("txt_profile_username")
                    .fillMaxWidth(),
                text = "@${account.username}",
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium
            )
        }
    }
}

@Composable
fun ProfileInfoSection(account: Account) {
    if (account.bio.isNotEmpty()) {
        Column {
            Card(
                modifier = Modifier,
                shape = MaterialTheme.appShapes.cardShape,
                elevation = 0.dp,
                backgroundColor = MaterialTheme.appColors.cardViewBackground
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (account.bio.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(id = org.openedx.profile.R.string.profile_about_me),
                            style = MaterialTheme.appTypography.titleSmall,
                            color = MaterialTheme.appColors.textPrimary
                        )
                        Text(
                            modifier = Modifier.testTag("txt_profile_bio"),
                            text = account.bio,
                            style = MaterialTheme.appTypography.bodyMedium,
                            color = MaterialTheme.appColors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileTopicPreview() {
    OpenEdXTheme {
        ProfileTopic(
            account = mockAccount
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileInfoSectionPreview() {
    OpenEdXTheme {
        ProfileInfoSection(
            account = mockAccount
        )
    }
}
