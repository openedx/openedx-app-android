package org.openedx.core.presentation.dialog.app_review

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openedx.core.R
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import kotlin.math.round

@Composable
fun RateDialog(
    modifier: Modifier = Modifier,
    onNotNowClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val rating = rememberSaveable { mutableFloatStateOf(0f) }
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .noRippleClickable {
                    onNotNowClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.appShapes.cardShape)
                    .noRippleClickable {}
                    .background(
                        color = MaterialTheme.appColors.background,
                        shape = MaterialTheme.appShapes.cardShape
                    )
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
                            isEnabled = rating.floatValue > 0f,
                            text = stringResource(id = R.string.core_submit),
                            onClick = onSubmitClick
                        )
                    }
                }
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
        textColor = MaterialTheme.appColors.buttonText
        backgroundColor = MaterialTheme.appColors.buttonBackground
    } else {
        textColor = MaterialTheme.appColors.inactiveButtonText
        backgroundColor = MaterialTheme.appColors.inactiveButtonBackground
    }

    Button(
        modifier = Modifier
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor
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
                color = textColor,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: MutableState<Float> = mutableFloatStateOf(0f),
    stars: Int = 5,
    starsColor: Color = MaterialTheme.appColors.rateStars,
) {
    val density = LocalDensity.current
    var componentWight by remember { mutableStateOf(0.dp) }
    var maxXValue by remember { mutableFloatStateOf(0f) }
    val startSize = componentWight / stars
    val filledStars = round(rating.value).toInt()
    val unfilledStars = (stars - round(rating.value)).toInt()

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
                    rating.value = offset.x / maxXValue * stars + 0.8f
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
            rating = remember {
                mutableFloatStateOf(3.75f)
            }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RateDialog() {
    OpenEdXTheme {
        RateDialog(
            onNotNowClick = {},
            onSubmitClick = {}
        )
    }
}