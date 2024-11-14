package org.openedx.course.presentation.container

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.ui.RoundTabsBar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.foundation.presentation.rememberWindowSize
import kotlin.math.roundToInt

private const val FLING_DELAY = 50L
private const val SCROLL_UP_THRESHOLD = 0.15f
private const val SCROLL_DOWN_THRESHOLD = 0.85f
private const val SHADE_HEIGHT_MULTIPLIER = 0.1f
private const val BLUR_PADDING_FACTOR = 3

@Composable
internal fun CollapsingLayout(
    modifier: Modifier = Modifier,
    courseImage: Bitmap,
    imageHeight: Int,
    isEnabled: Boolean,
    expandedTop: @Composable BoxScope.() -> Unit,
    collapsedTop: @Composable BoxScope.() -> Unit,
    navigation: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable BoxScope.() -> Unit,
    onBackClick: () -> Unit,
) {
    val localDensity = LocalDensity.current
    val expandedTopHeight = remember {
        mutableFloatStateOf(0f)
    }
    val collapsedTopHeight = remember {
        mutableFloatStateOf(0f)
    }
    val navigationHeight = remember {
        mutableFloatStateOf(0f)
    }
    val offset = remember { Animatable(0f) }
    val backgroundImageHeight = remember {
        mutableFloatStateOf(0f)
    }
    val windowSize = rememberWindowSize()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val rawFactor = (-imageHeight - offset.value) / -imageHeight
    val factor = if (rawFactor.isNaN() || rawFactor < 0) 0f else rawFactor
    val blurImagePadding = 40.dp
    val blurImagePaddingPx = with(localDensity) { blurImagePadding.toPx() }
    val toolbarOffset =
        (offset.value + backgroundImageHeight.floatValue - blurImagePaddingPx).roundToInt()
    val imageStartY = (backgroundImageHeight.floatValue - blurImagePaddingPx) / 2f
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
        val minValue =
            (-expandedTopHeight.floatValue - backgroundImageHeight.floatValue + collapsedTopHeight.floatValue).let {
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
                    offset.value == -expandedTopHeight.floatValue -> Offset.Zero
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

    val collapsingModifier = if (isEnabled) {
        modifier
            .nestedScroll(nestedScrollConnection)
    } else {
        modifier
    }
    Box(
        modifier = collapsingModifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var yStart = 0f
                coroutineScope {
                    routePointerChangesTo(
                        onDown = { change ->
                            yStart = change.position.y
                        },
                        onUp = { change ->
                            val yEnd = change.position.y
                            val yDelta = yEnd - yStart
                            val scrollDown = yDelta > 0
                            val collapsedOffset = -expandedTopHeight.floatValue - backgroundImageHeight.floatValue +
                                    collapsedTopHeight.floatValue
                            val expandedOffset = 0f

                            launch {
                                // Handle Fling, offset.animateTo does not work if the value changes faster than 10ms
                                if (change.uptimeMillis - change.previousUptimeMillis <= FLING_DELAY) {
                                    delay(FLING_DELAY)
                                }

                                if (scrollDown) {
                                    if (offset.value > -backgroundImageHeight.floatValue * SCROLL_DOWN_THRESHOLD) {
                                        offset.animateTo(expandedOffset)
                                    } else {
                                        offset.animateTo(collapsedOffset)
                                    }
                                } else {
                                    if (offset.value < -backgroundImageHeight.floatValue * SCROLL_UP_THRESHOLD) {
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
            CollapsingLayoutTablet(
                localDensity = localDensity,
                navigationHeight = navigationHeight,
                backgroundImageHeight = backgroundImageHeight,
                expandedTopHeight = expandedTopHeight,
                blurImagePaddingPx = blurImagePaddingPx,
                blurImagePadding = blurImagePadding,
                backBtnStartPadding = backBtnStartPadding,
                courseImage = courseImage,
                imageHeight = imageHeight,
                isEnabled = isEnabled,
                onBackClick = onBackClick,
                expandedTop = expandedTop,
                navigation = navigation,
                bodyContent = bodyContent
            )
        } else {
            CollapsingLayoutMobile(
                configuration = configuration,
                localDensity = localDensity,
                collapsedTopHeight = collapsedTopHeight,
                navigationHeight = navigationHeight,
                backgroundImageHeight = backgroundImageHeight,
                expandedTopHeight = expandedTopHeight,
                rawFactor = rawFactor,
                factor = factor,
                offset = offset,
                blurImagePaddingPx = blurImagePaddingPx,
                blurImageAlignment = blurImageAlignment,
                blurImagePadding = blurImagePadding,
                backBtnStartPadding = backBtnStartPadding,
                courseImage = courseImage,
                imageHeight = imageHeight,
                toolbarBackgroundOffset = toolbarBackgroundOffset,
                isEnabled = isEnabled,
                onBackClick = onBackClick,
                expandedTop = expandedTop,
                collapsedTop = collapsedTop,
                navigation = navigation,
                bodyContent = bodyContent
            )
        }
    }
}

@Composable
private fun CollapsingLayoutTablet(
    localDensity: Density,
    navigationHeight: MutableState<Float>,
    backgroundImageHeight: MutableState<Float>,
    expandedTopHeight: MutableState<Float>,
    blurImagePaddingPx: Float,
    blurImagePadding: Dp,
    backBtnStartPadding: Dp,
    courseImage: Bitmap,
    imageHeight: Int,
    isEnabled: Boolean,
    onBackClick: () -> Unit,
    expandedTop: @Composable BoxScope.() -> Unit,
    navigation: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable BoxScope.() -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight.dp)
                .onSizeChanged { size ->
                    backgroundImageHeight.value = size.height.toFloat()
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
                        y = (backgroundImageHeight.value - blurImagePaddingPx).roundToInt()
                    )
                }
                .background(Color.White)
                .blur(100.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.appColors.surface)
                    .fillMaxWidth()
                    .height(
                        with(localDensity) {
                            (expandedTopHeight.value + navigationHeight.value).toDp() + blurImagePadding
                        }
                    )
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
                    .height(
                        with(localDensity) {
                            (expandedTopHeight.value + navigationHeight.value).toDp() * SHADE_HEIGHT_MULTIPLIER
                        }
                    )
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
                    backgroundImageHeight.value = size.height.toFloat()
                },
            bitmap = courseImage.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(localDensity) { (expandedTopHeight.value + navigationHeight.value).toDp() })
                .offset { IntOffset(x = 0, y = backgroundImageHeight.value.roundToInt()) }
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
                .height(
                    with(localDensity) {
                        (expandedTopHeight.value + navigationHeight.value).toDp() + blurImagePadding
                    }
                )
                .offset {
                    IntOffset(
                        x = 0,
                        y = (backgroundImageHeight.value - blurImagePaddingPx).roundToInt()
                    )
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
                expandedTopHeight.value = size.height.toFloat()
            }
            .offset { IntOffset(x = 0, y = backgroundImageHeight.value.roundToInt()) },
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
            .offset {
                IntOffset(
                    x = 0,
                    y = (backgroundImageHeight.value + expandedTopHeight.value).roundToInt()
                )
            }
            .onSizeChanged { size ->
                navigationHeight.value = size.height.toFloat()
            },
        content = navigation,
    )

    val bodyPadding = expandedTopHeight.value + backgroundImageHeight.value + navigationHeight.value
    val bodyModifier = if (isEnabled) {
        Modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = bodyPadding.roundToInt()
                )
            }
            .padding(bottom = with(localDensity) { bodyPadding.toDp() })
    } else {
        Modifier
            .padding(top = with(localDensity) { if (bodyPadding < 0) 0.toDp() else bodyPadding.toDp() })
    }
    Box(
        modifier = bodyModifier,
        content = bodyContent,
    )
}

@Composable
private fun CollapsingLayoutMobile(
    configuration: Configuration,
    localDensity: Density,
    collapsedTopHeight: MutableState<Float>,
    navigationHeight: MutableState<Float>,
    backgroundImageHeight: MutableState<Float>,
    expandedTopHeight: MutableState<Float>,
    rawFactor: Float,
    factor: Float,
    offset: Animatable<Float, AnimationVector1D>,
    blurImagePaddingPx: Float,
    blurImageAlignment: Float,
    blurImagePadding: Dp,
    backBtnStartPadding: Dp,
    courseImage: Bitmap,
    imageHeight: Int,
    toolbarBackgroundOffset: Int,
    isEnabled: Boolean,
    onBackClick: () -> Unit,
    expandedTop: @Composable BoxScope.() -> Unit,
    collapsedTop: @Composable BoxScope.() -> Unit,
    navigation: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable BoxScope.() -> Unit,
) {
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
                        .height(
                            with(localDensity) {
                                (collapsedTopHeight.value + navigationHeight.value).toDp() + blurImagePadding
                            }
                        )
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
                        .height(
                            with(localDensity) {
                                (collapsedTopHeight.value + navigationHeight.value).toDp() * SHADE_HEIGHT_MULTIPLIER
                            }
                        )
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
                        backgroundImageHeight.value = size.height.toFloat()
                    },
                bitmap = courseImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(localDensity) { (collapsedTopHeight.value + navigationHeight.value).toDp() })
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
                    collapsedTopHeight.value = size.height.toFloat()
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
                contentDescription = stringResource(id = R.string.core_accessibility_btn_back)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                content = collapsedTop,
            )
        }

        Box(
            modifier = Modifier
                .displayCutoutForLandscape()
                .offset { IntOffset(x = 0, y = (collapsedTopHeight.value).roundToInt()) }
                .onSizeChanged { size ->
                    navigationHeight.value = size.height.toFloat()
                },
            content = navigation,
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = (collapsedTopHeight.value + navigationHeight.value).roundToInt()
                    )
                }
                .padding(bottom = with(localDensity) { (collapsedTopHeight.value + navigationHeight.value).toDp() }),
            content = bodyContent,
        )
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .onSizeChanged { size ->
                        backgroundImageHeight.value = size.height.toFloat()
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
                val adaptiveBlurImagePadding = blurImagePadding.value * (BLUR_PADDING_FACTOR - rawFactor)
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.appColors.surface)
                        .fillMaxWidth()
                        .height(
                            with(localDensity) {
                                (expandedTopHeight.value + navigationHeight.value + adaptiveBlurImagePadding).toDp()
                            }
                        )
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
                        .height(
                            with(localDensity) {
                                (expandedTopHeight.value + navigationHeight.value).toDp() * SHADE_HEIGHT_MULTIPLIER
                            }
                        )
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
                        backgroundImageHeight.value = size.height.toFloat()
                    },
                bitmap = courseImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(localDensity) { (expandedTopHeight.value + navigationHeight.value).toDp() })
                    .offset { IntOffset(x = 0, y = backgroundImageHeight.value.roundToInt()) }
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
                    .height(
                        with(localDensity) {
                            (expandedTopHeight.value + navigationHeight.value).toDp() + blurImagePadding
                        }
                    )
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (offset.value + backgroundImageHeight.value - blurImagePaddingPx).roundToInt()
                        )
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
                    expandedTopHeight.value = size.height.toFloat()
                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = (offset.value + backgroundImageHeight.value - blurImagePaddingPx).roundToInt()
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
                    collapsedTopHeight.value = size.height.toFloat()
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
                contentDescription = stringResource(id = R.string.core_accessibility_btn_back)
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
                        y = (
                                offset.value + backgroundImageHeight.value +
                                        expandedTopHeight.value - adaptiveImagePadding
                                ).roundToInt()
                    )
                }
                .onSizeChanged { size ->
                    navigationHeight.value = size.height.toFloat()
                },
            content = navigation,
        )

        val bodyPadding = expandedTopHeight.value + offset.value + backgroundImageHeight.value +
                navigationHeight.value - blurImagePaddingPx * factor
        val bodyModifier = if (isEnabled) {
            Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = bodyPadding.roundToInt()
                    )
                }
                .padding(bottom = with(localDensity) { (collapsedTopHeight.value + navigationHeight.value).toDp() })
        } else {
            Modifier
                .padding(top = with(localDensity) { if (bodyPadding < 0) 0.toDp() else bodyPadding.toDp() })
        }
        Box(
            modifier = bodyModifier,
            content = bodyContent,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollapsingLayoutPreview() {
    OpenEdXTheme {
        CollapsingLayout(
            modifier = Modifier,
            courseImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            imageHeight = 200,
            expandedTop = {
                ExpandedHeaderContent(
                    courseTitle = "courseName",
                    org = "organization"
                )
            },
            collapsedTop = {
                CollapsedHeaderContent(
                    courseTitle = "courseName"
                )
            },
            navigation = {
                RoundTabsBar(
                    items = CourseContainerTab.entries,
                    rowState = rememberLazyListState(),
                    pagerState = rememberPagerState(pageCount = { CourseContainerTab.entries.size })
                )
            },
            isEnabled = true,
            onBackClick = {},
            bodyContent = {}
        )
    }
}

suspend fun PointerInputScope.routePointerChangesTo(
    onDown: (PointerInputChange) -> Unit = {},
    onUp: (PointerInputChange) -> Unit = {},
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
    val offsetY: Float,
) : Alignment {

    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val centerX = (space.width - size.width).toFloat() / 2f
        val centerY = (space.height - size.height).toFloat() / 2f

        val x = centerX + offsetX
        val y = centerY + offsetY

        return IntOffset(x.roundToInt(), y.roundToInt())
    }
}
