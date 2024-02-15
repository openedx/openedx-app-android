package org.openedx.profile.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.profile.compose.mockAccount

@Composable
fun ProfileTopic(account: Account) {
    Column(
        Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
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
                .border(
                    2.dp,
                    MaterialTheme.appColors.onSurface,
                    CircleShape
                )
                .padding(2.dp)
                .size(100.dp)
                .clip(CircleShape)
        )
        if (account.name.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                modifier = Modifier.testTag("txt_profile_name"),
                text = account.name,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.headlineSmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.testTag("txt_profile_username"),
            text = "@${account.username}",
            color = MaterialTheme.appColors.textPrimaryVariant,
            style = MaterialTheme.appTypography.labelLarge
        )
    }
}

@Composable
fun ProfileInfoSection(account: Account) {

    if (account.yearOfBirth != null || account.bio.isNotEmpty()) {
        Column {
            Text(
                modifier = Modifier.testTag("txt_profile_info_label"),
                text = stringResource(id = org.openedx.profile.R.string.profile_prof_info),
                style = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (account.yearOfBirth != null) {
                        Text(
                            modifier = Modifier.testTag("txt_profile_year_of_birth"),
                            text = buildAnnotatedString {
                                val value = if (account.yearOfBirth != null) {
                                    account.yearOfBirth.toString()
                                } else ""
                                val text = stringResource(
                                    id = org.openedx.profile.R.string.profile_year_of_birth,
                                    value
                                )
                                append(text)
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.appColors.textPrimaryVariant
                                    ),
                                    start = 0,
                                    end = text.length - value.length
                                )
                            },
                            style = MaterialTheme.appTypography.titleMedium,
                            color = MaterialTheme.appColors.textPrimary
                        )
                    }
                    if (account.bio.isNotEmpty()) {
                        Text(
                            modifier = Modifier.testTag("txt_profile_bio"),
                            text = buildAnnotatedString {
                                val text = stringResource(
                                    id = org.openedx.profile.R.string.profile_bio,
                                    account.bio
                                )
                                append(text)
                                addStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.appColors.textPrimaryVariant
                                    ),
                                    start = 0,
                                    end = text.length - account.bio.length
                                )
                            },
                            style = MaterialTheme.appTypography.titleMedium,
                            color = MaterialTheme.appColors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileTopicPreview() {
    ProfileTopic(
        account = mockAccount
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileInfoSectionPreview() {
    ProfileInfoSection(
        account = mockAccount
    )
}
