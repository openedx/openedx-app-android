package org.openedx.course.presentation.container

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.appColors
import kotlin.math.roundToInt

@Composable
internal fun CollapsingLayout(
    courseImage: Bitmap,
    expandedTop: @Composable BoxScope.() -> Unit,
    collapsedTop: @Composable BoxScope.() -> Unit,
    navigation: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable BoxScope.() -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current
    var expandedTopHeight by remember {
        mutableFloatStateOf(0f)
    }
    var collapsedTopHeight by remember {
        mutableFloatStateOf(0f)
    }
    var navigationHeight by remember {
        mutableFloatStateOf(0f)
    }
    val offset = remember { Animatable(0f) }
    var backgroundImageHeight by remember {
        mutableFloatStateOf(0f)
    }
    val windowSize = rememberWindowSize()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val imageHeight = 200
    val rawFactor = (-imageHeight - offset.value) / -imageHeight
    val factor = if (rawFactor.isNaN() || rawFactor < 0) 0f else rawFactor
    val blurImagePadding = 40.dp
    val blurImagePaddingPx = with(localDensity) { blurImagePadding.toPx() }
    val toolbarOffset = (offset.value + backgroundImageHeight - blurImagePaddingPx).roundToInt()
    val imageStartY = (backgroundImageHeight - blurImagePaddingPx) * 0.5f
    val imageOffsetY = -(offset.value + imageStartY)
    val toolbarBackgroundOffset = if (toolbarOffset >= 0) {
        toolbarOffset
    } else {
        0
    }
    val blurImageAlignment = if (toolbarOffset >= 0f) {
        imageOffsetY
    } else {
        imageStartY
    }
    val backBtnStartPadding = if (!windowSize.isTablet) {
        0.dp
    } else {
        60.dp
    }

    fun calculateOffset(delta: Float): Offset {
        val oldOffset = offset.value
        val maxValue = 0f
        val minValue = (-expandedTopHeight - backgroundImageHeight + collapsedTopHeight).let {
            if (it >= maxValue) {
                0f
            } else {
                it
            }
        }
        val newOffset = (oldOffset + delta).coerceIn(minValue, maxValue)
        coroutineScope.launch {
            offset.snapTo(newOffset)
        }
        return Offset(0f, newOffset - oldOffset)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                when {
                    available.y >= 0 -> Offset.Zero
                    offset.value == -expandedTopHeight -> Offset.Zero
                    else -> calculateOffset(available.y)
                }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                when {
                    available.y <= 0 -> Offset.Zero
                    offset.value == 0f -> Offset.Zero
                    else -> calculateOffset(available.y)
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                var yStart = 0f
                kotlinx.coroutines.coroutineScope {
                    routePointerChangesTo(
                        onDown = { change ->
                            yStart = change.position.y
                        },
                        onUp = { change ->
                            val yEnd = change.position.y
                            val yDelta = yEnd - yStart
                            val scrollDown = yDelta > 0
                            val collapsedOffset =
                                -expandedTopHeight - backgroundImageHeight + collapsedTopHeight
                            val expandedOffset = 0f

                            launch {
                                // Handle Fling, offset.animateTo does not work if the value changes faster than 10ms
                                if (change.uptimeMillis - change.previousUptimeMillis <= 50) {
                                    delay(50)
                                }

                                if (scrollDown) {
                                    if (offset.value > -backgroundImageHeight * 0.85) {
                                        offset.animateTo(expandedOffset)
                                    } else {
                                        offset.animateTo(collapsedOffset)
                                    }
                                } else {
                                    if (offset.value < -backgroundImageHeight * 0.15) {
                                        offset.animateTo(collapsedOffset)
                                    } else {
                                        offset.animateTo(expandedOffset)
                                    }
                                }
                            }
                        }
                    )
                }
            },
    ) {
        if (windowSize.isTablet) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .onSizeChanged { size ->
                            backgroundImageHeight = size.height.toFloat()
                        },
                    bitmap = courseImage.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (backgroundImageHeight - blurImagePaddingPx).roundToInt())
                        }
                        .background(Color.White)
                        .blur(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.surface)
                            .fillMaxWidth()
                            .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } + blurImagePadding)
                            .align(Alignment.Center)
                    )
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(blurImagePadding)
                            .align(Alignment.TopCenter),
                        bitmap = courseImage.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.courseHomeHeaderShade)
                            .fillMaxWidth()
                            .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } * 0.1f)
                            .align(Alignment.BottomCenter)
                    )
                }
            } else {
                val backgroundColor = MaterialTheme.appColors.background
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .onSizeChanged { size ->
                            backgroundImageHeight = size.height.toFloat()
                        },
                    bitmap = courseImage.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() })
                        .offset { IntOffset(x = 0, y = backgroundImageHeight.roundToInt()) }
                        .background(backgroundColor)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(backgroundColor, Color.Transparent),
                                startY = 500f,
                                endY = 400f
                            )
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } + blurImagePadding)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (backgroundImageHeight - blurImagePaddingPx).roundToInt() )
                        }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(backgroundColor, Color.Transparent),
                                startY = 400f,
                                endY = 0f
                            )
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        expandedTopHeight = size.height.toFloat()
                    }
                    .offset { IntOffset(x = 0, y = backgroundImageHeight.roundToInt()) },
                content = expandedTop,
            )

            Icon(
                modifier = Modifier
                    .statusBarsInset()
                    .padding(top = 12.dp, start = backBtnStartPadding + 12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.courseHomeBackBtnBackground)
                    .clickable {
                        onBackClick()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = MaterialTheme.appColors.textPrimary,
                contentDescription = null
            )


            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = (backgroundImageHeight + expandedTopHeight).roundToInt()) }
                    .onSizeChanged { size ->
                        navigationHeight = size.height.toFloat()
                    },
                content = navigation,
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (expandedTopHeight + backgroundImageHeight + navigationHeight).roundToInt()
                        )
                    }
                    .padding(bottom = with(localDensity) { (expandedTopHeight + navigationHeight + backgroundImageHeight).toDp() }),
                content = bodyContent,
            )
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .background(Color.White)
                        .blur(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.surface)
                            .fillMaxWidth()
                            .height(with(localDensity) { (collapsedTopHeight + navigationHeight).toDp() } + blurImagePadding)
                            .align(Alignment.Center)
                    )
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(blurImagePadding)
                            .align(Alignment.TopCenter),
                        bitmap = courseImage.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.courseHomeHeaderShade)
                            .fillMaxWidth()
                            .height(with(localDensity) { (collapsedTopHeight + navigationHeight).toDp() } * 0.1f)
                            .align(Alignment.BottomCenter)
                    )
                }
            } else {
                val backgroundColor = MaterialTheme.appColors.background
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .onSizeChanged { size ->
                            backgroundImageHeight = size.height.toFloat()
                        },
                    bitmap = courseImage.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (collapsedTopHeight + navigationHeight).toDp() })
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(backgroundColor, Color.Transparent),
                                startY = 400f,
                                endY = 0f
                            )
                        ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .displayCutoutForLandscape()
                    .padding(horizontal = 12.dp)
                    .onSizeChanged { size ->
                        collapsedTopHeight = size.height.toFloat()
                    },
                verticalAlignment = Alignment.Bottom
            ) {
                Icon(
                    modifier = Modifier
                        .statusBarsInset()
                        .padding(top = 12.dp, start = backBtnStartPadding)
                        .clip(CircleShape)
                        .clickable {
                            onBackClick()
                        },
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = MaterialTheme.appColors.textPrimary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    content = collapsedTop,
                )
            }


            Box(
                modifier = Modifier
                    .displayCutoutForLandscape()
                    .offset { IntOffset(x = 0, y = (collapsedTopHeight).roundToInt()) }
                    .onSizeChanged { size ->
                        navigationHeight = size.height.toFloat()
                    },
                content = navigation,
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (collapsedTopHeight + navigationHeight).roundToInt()
                        )
                    }
                    .padding(bottom = with(localDensity) { (collapsedTopHeight + navigationHeight).toDp() }),
                content = bodyContent,
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .onSizeChanged { size ->
                            backgroundImageHeight = size.height.toFloat()
                        },
                    bitmap = courseImage.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = toolbarBackgroundOffset) }
                        .background(Color.White)
                        .blur(100.dp)
                ) {
                    val adaptiveBlurImagePadding = blurImagePadding.value * (3 - rawFactor)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.surface)
                            .fillMaxWidth()
                            .height(with(localDensity) { (expandedTopHeight + navigationHeight + adaptiveBlurImagePadding).toDp() })
                            .align(Alignment.Center)
                    )
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(blurImagePadding)
                            .align(Alignment.TopCenter),
                        bitmap = courseImage.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = PixelAlignment(0f, blurImageAlignment),
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.appColors.courseHomeHeaderShade)
                            .fillMaxWidth()
                            .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } * 0.1f)
                            .align(Alignment.BottomCenter)
                    )
                }
            } else {
                val backgroundColor = MaterialTheme.appColors.background
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .onSizeChanged { size ->
                            backgroundImageHeight = size.height.toFloat()
                        },
                    bitmap = courseImage.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() })
                        .offset { IntOffset(x = 0, y = backgroundImageHeight.roundToInt()) }
                        .background(backgroundColor)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(backgroundColor, Color.Transparent),
                                startY = 500f,
                                endY = 400f
                            )
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } + blurImagePadding)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (offset.value + backgroundImageHeight - blurImagePaddingPx).roundToInt() )
                        }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(backgroundColor, Color.Transparent),
                                startY = 400f,
                                endY = 0f
                            )
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        expandedTopHeight = size.height.toFloat()
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (offset.value + backgroundImageHeight - blurImagePaddingPx).roundToInt()
                        )
                    }
                    .alpha(factor),
                content = expandedTop,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .onSizeChanged { size ->
                        collapsedTopHeight = size.height.toFloat()
                    },
                verticalAlignment = Alignment.Bottom
            ) {
                Icon(
                    modifier = Modifier
                        .statusBarsInset()
                        .padding(top = 12.dp, start = backBtnStartPadding)
                        .clip(CircleShape)
                        .background(MaterialTheme.appColors.courseHomeBackBtnBackground.copy(factor / 2))
                        .clickable {
                            onBackClick()
                        },
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = MaterialTheme.appColors.textPrimary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .alpha(1 - factor),
                    content = collapsedTop,
                )
            }

            val adaptiveImagePadding = blurImagePaddingPx * factor
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (offset.value + backgroundImageHeight + expandedTopHeight - adaptiveImagePadding).roundToInt()
                        )
                    }
                    .onSizeChanged { size ->
                        navigationHeight = size.height.toFloat()
                    },
                content = navigation,
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (expandedTopHeight + offset.value + backgroundImageHeight + navigationHeight - blurImagePaddingPx * factor).roundToInt()
                        )
                    }
                    .padding(bottom = with(localDensity) { (collapsedTopHeight + navigationHeight).toDp() }),
                content = bodyContent,
            )
        }
    }
}

suspend fun PointerInputScope.routePointerChangesTo(
    onDown: (PointerInputChange) -> Unit = {},
    onUp: (PointerInputChange) -> Unit = {}
) {
    awaitEachGesture {
        do {
            val event = awaitPointerEvent()
            event.changes.forEach {
                when (event.type) {
                    PointerEventType.Press -> onDown(it)
                    PointerEventType.Release -> onUp(it)
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Immutable
data class PixelAlignment(
    val offsetX: Float,
    val offsetY: Float
) : Alignment {

    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val centerX = (space.width - size.width).toFloat() / 2f
        val centerY = (space.height - size.height).toFloat() / 2f

        val x = centerX + offsetX
        val y = centerY + offsetY

        return IntOffset(x.roundToInt(), y.roundToInt())
    }
}
