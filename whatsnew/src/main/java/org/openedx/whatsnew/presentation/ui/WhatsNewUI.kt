package org.openedx.whatsnew.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.whatsnew.R

@Composable
fun PageIndicator(
    numberOfPages: Int,
    modifier: Modifier = Modifier,
    selectedPage: Int = 0,
    selectedColor: Color = MaterialTheme.appColors.info,
    previousUnselectedColor: Color = MaterialTheme.appColors.cardViewBorder,
    nextUnselectedColor: Color = MaterialTheme.appColors.textFieldBorder,
    defaultRadius: Dp = 20.dp,
    selectedLength: Dp = 60.dp,
    space: Dp = 30.dp,
    animationDurationInMillis: Int = 300,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space),
        modifier = modifier,
    ) {
        for (i in 0 until numberOfPages) {
            val isSelected = i == selectedPage
            val unselectedColor =
                if (i < selectedPage) previousUnselectedColor else nextUnselectedColor
            PageIndicatorView(
                isSelected = isSelected,
                selectedColor = selectedColor,
                defaultColor = unselectedColor,
                defaultRadius = defaultRadius,
                selectedLength = selectedLength,
                animationDurationInMillis = animationDurationInMillis,
            )
        }
    }
}

@Composable
fun PageIndicatorView(
    isSelected: Boolean,
    selectedColor: Color,
    defaultColor: Color,
    defaultRadius: Dp,
    selectedLength: Dp,
    animationDurationInMillis: Int,
    modifier: Modifier = Modifier,
) {
    val color: Color by animateColorAsState(
        targetValue = if (isSelected) {
            selectedColor
        } else {
            defaultColor
        },
        animationSpec = tween(
            durationMillis = animationDurationInMillis,
        ),
        label = ""
    )
    val width: Dp by animateDpAsState(
        targetValue = if (isSelected) {
            selectedLength
        } else {
            defaultRadius
        },
        animationSpec = tween(
            durationMillis = animationDurationInMillis,
        ),
        label = ""
    )

    Canvas(
        modifier = modifier
            .size(
                width = width,
                height = defaultRadius,
            ),
    ) {
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(
                width = width.toPx(),
                height = defaultRadius.toPx(),
            ),
            cornerRadius = CornerRadius(
                x = defaultRadius.toPx(),
                y = defaultRadius.toPx(),
            ),
        )
    }
}

@Composable
fun NavigationUnitsButtons(
    hasPrevPage: Boolean,
    hasNextPage: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PrevButton(
            hasPrevPage = hasPrevPage,
            onPrevClick = onPrevClick
        )
        NextFinishButton(
            hasNextPage = hasNextPage,
            onNextClick = onNextClick
        )
    }
}

@Composable
fun PrevButton(
    hasPrevPage: Boolean,
    onPrevClick: () -> Unit
) {
    val prevButtonAnimationFactor by animateFloatAsState(
        targetValue = if (hasPrevPage) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    OutlinedButton(
        modifier = Modifier
            .testTag("btn_previous")
            .height(42.dp)
            .alpha(prevButtonAnimationFactor),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MaterialTheme.appColors.background
        ),
        border = BorderStroke(1.dp, MaterialTheme.appColors.primary),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onPrevClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = org.openedx.core.R.drawable.core_ic_back),
                contentDescription = null,
                tint = MaterialTheme.appColors.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.whats_new_navigation_previous),
                color = MaterialTheme.appColors.primary,
                style = MaterialTheme.appTypography.labelLarge
            )
        }
    }
}

@Composable
fun NextFinishButton(
    onNextClick: () -> Unit,
    hasNextPage: Boolean
) {
    Button(
        modifier = Modifier
            .testTag("btn_next")
            .height(42.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.appColors.primaryButtonBackground
        ),
        elevation = null,
        shape = MaterialTheme.appShapes.navigationButtonShape,
        onClick = onNextClick
    ) {
        AnimatedContent(
            targetState = hasNextPage,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = ""
        ) { hasNextPage ->
            if (hasNextPage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier.testTag("txt_next"),
                        text = stringResource(id = R.string.whats_new_navigation_next),
                        color = MaterialTheme.appColors.primaryButtonText,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = org.openedx.core.R.drawable.core_ic_forward),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primaryButtonText
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier.testTag("txt_done"),
                        text = stringResource(id = R.string.whats_new_navigation_done),
                        color = MaterialTheme.appColors.primaryButtonText,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = org.openedx.core.R.drawable.core_ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primaryButtonText
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NavigationUnitsButtonsPrevInTheMiddle() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevPage = true,
            hasNextPage = true,
            onPrevClick = {},
            onNextClick = {}
        )
    }
}

@Preview
@Composable
private fun NavigationUnitsButtonsPrevInTheStart() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevPage = false,
            hasNextPage = true,
            onPrevClick = {},
            onNextClick = {}
        )
    }
}

@Preview
@Composable
private fun NavigationUnitsButtonsPrevInTheEnd() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevPage = true,
            hasNextPage = false,
            onPrevClick = {},
            onNextClick = {}
        )
    }
}

@Preview
@Composable
private fun PageIndicatorViewPreview() {
    OpenEdXTheme {
        PageIndicator(
            numberOfPages = 4,
            selectedPage = 2
        )
    }
}
