package org.openedx.core.presentation.dialog.appreview

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.presentation.dialog.DefaultDialogBox
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import kotlin.math.round

@Composable
fun ThankYouDialog(
    modifier: Modifier = Modifier,
    description: String,
    showButtons: Boolean,
    onNotNowClick: () -> Unit,
    onRateUsClick: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val imageModifier = if (orientation == ORIENTATION_LANDSCAPE) {
        Modifier.size(40.dp)
    } else {
        Modifier
    }

    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onNotNowClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Image(
                modifier = imageModifier,
                painter = painterResource(id = R.drawable.core_ic_heart),
                contentScale = ContentScale.FillBounds,
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.core_thank_you),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = description,
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )

            if (showButtons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    TransparentTextButton(
                        text = stringResource(id = R.string.core_not_now),
                        onClick = onNotNowClick
                    )
                    DefaultTextButton(
                        text = stringResource(id = R.string.core_rate_us),
                        onClick = onRateUsClick
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    modifier: Modifier = Modifier,
    feedback: MutableState<String>,
    onNotNowClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val textFieldModifier = if (orientation == ORIENTATION_LANDSCAPE) {
        Modifier.height(80.dp)
    } else {
        Modifier.height(162.dp)
    }

    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onNotNowClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.core_feedback_dialog_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.core_feedback_dialog_description),
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(textFieldModifier),
                value = feedback.value,
                onValueChange = { str ->
                    feedback.value = str
                },
                textStyle = MaterialTheme.appTypography.labelLarge,
                shape = MaterialTheme.appShapes.buttonShape,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.core_feedback_dialog_textfield_hint),
                        color = MaterialTheme.appColors.textFieldHint,
                        style = MaterialTheme.appTypography.labelLarge,
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.appColors.cardViewBackground,
                    unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
                    textColor = MaterialTheme.appColors.textFieldText
                ),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TransparentTextButton(
                    text = stringResource(id = R.string.core_not_now),
                    onClick = onNotNowClick
                )
                DefaultTextButton(
                    isEnabled = feedback.value.isNotEmpty(),
                    text = stringResource(id = R.string.core_share_feedback),
                    onClick = onShareClick
                )
            }
        }
    }
}

@Composable
fun RateDialog(
    modifier: Modifier = Modifier,
    rating: MutableIntState,
    onNotNowClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    DefaultDialogBox(
        modifier = modifier,
        onDismissClick = onNotNowClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.core_rate_dialog_title, stringResource(R.string.app_name)),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.core_rate_dialog_description),
                color = MaterialTheme.appColors.textPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.bodyMedium
            )
            RatingBar(
                modifier = Modifier
                    .padding(vertical = 12.dp),
                rating = rating
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TransparentTextButton(
                    text = stringResource(id = R.string.core_not_now),
                    onClick = onNotNowClick
                )
                DefaultTextButton(
                    isEnabled = rating.intValue > 0,
                    text = stringResource(id = R.string.core_submit),
                    onClick = onSubmitClick
                )
            }
        }
    }
}

@Composable
fun TransparentTextButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onClick
    ) {
        Text(
            color = MaterialTheme.appColors.textAccent,
            style = MaterialTheme.appTypography.labelLarge,
            text = text
        )
    }
}

@Composable
fun DefaultTextButton(
    isEnabled: Boolean = true,
    text: String,
    onClick: () -> Unit
) {
    val textColor: Color
    val backgroundColor: Color
    if (isEnabled) {
        textColor = MaterialTheme.appColors.primaryButtonText
        backgroundColor = MaterialTheme.appColors.primaryButtonBackground
    } else {
        textColor = MaterialTheme.appColors.inactiveButtonText
        backgroundColor = MaterialTheme.appColors.inactiveButtonBackground
    }

    Button(
        modifier = Modifier
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        enabled = isEnabled,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: MutableIntState,
    stars: Int = 5,
    starsColor: Color = MaterialTheme.appColors.rateStars,
) {
    val density = LocalDensity.current
    var componentWight by remember { mutableStateOf(0.dp) }
    var maxXValue by remember { mutableFloatStateOf(0f) }
    val startSize = componentWight / stars
    val filledStars = rating.intValue
    val unfilledStars = stars - rating.intValue

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                maxXValue = coordinates.size.width + coordinates.positionInRoot().x
                componentWight = with(density) {
                    coordinates.size.width.toDp()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    rating.intValue = round(x = offset.x / maxXValue * stars + 0.8f).toInt()
                }
            },
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(filledStars) {
            Icon(
                modifier = Modifier.size(startSize),
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = starsColor
            )
        }
        repeat(unfilledStars) {
            Icon(
                modifier = Modifier.size(startSize),
                imageVector = Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun RatingBarPreview() {
    OpenEdXTheme {
        RatingBar(
            rating = remember { mutableIntStateOf(2) }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RateDialogPreview() {
    OpenEdXTheme {
        RateDialog(
            rating = remember { mutableIntStateOf(2) },
            onNotNowClick = {},
            onSubmitClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun FeedbackDialogPreview() {
    OpenEdXTheme {
        FeedbackDialog(
            feedback = remember { mutableStateOf("Feedback") },
            onNotNowClick = {},
            onShareClick = {}
        )
    }
}

@Preview
@Composable
private fun ThankYouDialogWithButtonsPreview() {
    OpenEdXTheme {
        ThankYouDialog(
            description = "Description",
            showButtons = true,
            onNotNowClick = {},
            onRateUsClick = {}
        )
    }
}

@Preview
@Composable
private fun ThankYouDialogWithoutButtonsPreview() {
    OpenEdXTheme {
        ThankYouDialog(
            description = "Description",
            showButtons = false,
            onNotNowClick = {},
            onRateUsClick = {}
        )
    }
}
