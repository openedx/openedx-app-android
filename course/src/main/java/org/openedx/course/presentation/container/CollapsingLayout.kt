package org.openedx.course.presentation.container

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Picture
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.appColors
import kotlin.math.roundToInt

@Composable
internal fun CollapsingLayout(
    imageUrl: String,
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
    var offset by remember {
        mutableFloatStateOf(0f)
    }
    var backgroundImageHeight by remember {
        mutableFloatStateOf(0f)
    }
    val picture = remember { Picture() }
    val windowSize = rememberWindowSize()
    val configuration = LocalConfiguration.current
    val imageHeight = if (!windowSize.isTablet && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        100
    } else {
        200
    }
    val factor = (-imageHeight - offset) / -imageHeight
    val alpha = if (factor.isNaN() || factor < 0) 0f else factor
    val blurImagePadding = 40.dp
    val toolbarOffset = with(localDensity) { (offset + backgroundImageHeight - blurImagePadding.toPx()).roundToInt() }
    val imageStartY = with(localDensity) { backgroundImageHeight - blurImagePadding.toPx() } * 0.5f
    val imageOffsetY = -(offset + imageStartY)
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
    val imageModel = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .error(org.openedx.core.R.drawable.core_no_image_course)
        .placeholder(org.openedx.core.R.drawable.core_no_image_course)
        .allowHardware(false)
        .build()

    fun calculateOffset(delta: Float): Offset {
        val oldOffset = offset
        val newOffset =
            (oldOffset + delta).coerceIn(-expandedTopHeight - backgroundImageHeight + collapsedTopHeight, 0f)
        offset = newOffset
        return Offset(0f, newOffset - oldOffset)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                when {
                    available.y >= 0 -> Offset.Zero
                    offset == -expandedTopHeight -> Offset.Zero
                    else -> calculateOffset(available.y)
                }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                when {
                    available.y <= 0 -> Offset.Zero
                    offset == 0f -> Offset.Zero
                    else -> calculateOffset(available.y)
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight.dp)
                .onSizeChanged { size ->
                    backgroundImageHeight = size.height.toFloat()
                }
                .drawWithCache {
                    val width = this.size.width.toInt()
                    val height = this.size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas =
                            androidx.compose.ui.graphics.Canvas(
                                picture.beginRecording(
                                    width,
                                    height
                                )
                            )
                        draw(this, this.layoutDirection, pictureCanvas, this.size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()
                        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
                    }
                },
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = toolbarBackgroundOffset) }
                    .background(Color.White)
                    .blur(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.appColors.courseHomeHeaderShadePrimary)
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } + blurImagePadding)
                        .align(Alignment.Center)
                )
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(blurImagePadding)
                        .align(Alignment.TopCenter),
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = PixelAlignment(0f, blurImageAlignment),
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.appColors.courseHomeHeaderShadeSecondary)
                        .fillMaxWidth()
                        .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } * 0.1f)
                        .align(Alignment.BottomCenter)
                )
            }
        } else if (picture.width > 0 && picture.height > 0) {
            //TODO Change color
            val backgroundColor = MaterialTheme.appColors.cardViewBackground
            LegacyBlurImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp),
                bitmap = createBitmapFromPicture(picture),
                blurRadio = 25f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(localDensity) { (expandedTopHeight + navigationHeight).toDp() } )
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
                            y = with(localDensity) { (offset + backgroundImageHeight - blurImagePadding.toPx()).roundToInt() })
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
                .offset { IntOffset(x = 0, y = (offset + backgroundImageHeight).roundToInt()) }
                .alpha(alpha),
            content = expandedTop,
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .onSizeChanged { size ->
                    collapsedTopHeight = size.height.toFloat()
                }
        ) {
            Icon(
                modifier = Modifier
                    .statusBarsInset()
                    .padding(top = 12.dp, start = backBtnStartPadding)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.courseHomeBackBtnBackground.copy(alpha / 2))
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
                    .padding(top = 12.dp)
                    .alpha(1 - alpha),
                content = collapsedTop,
            )
        }


        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = (offset + backgroundImageHeight + expandedTopHeight).roundToInt()) }
                .onSizeChanged { size ->
                    navigationHeight = size.height.toFloat()
                },
            content = navigation,
        )

        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = 0,
                    y = (expandedTopHeight + offset + backgroundImageHeight + navigationHeight).roundToInt()
                )
            },
            content = bodyContent,
        )
    }
}

private fun createBitmapFromPicture(picture: Picture): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawPicture(picture)
    return bitmap
}

@Composable
fun BlurImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun LegacyBlurImage(
    bitmap: Bitmap,
    blurRadio: Float,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val renderScript = RenderScript.create(LocalContext.current)
    val bitmapAlloc = Allocation.createFromBitmap(renderScript, bitmap)
    ScriptIntrinsicBlur.create(renderScript, bitmapAlloc.element).apply {
        setRadius(blurRadio)
        setInput(bitmapAlloc)
        forEach(bitmapAlloc)
    }
    bitmapAlloc.copyTo(bitmap)
    renderScript.destroy()

    BlurImage(bitmap, modifier)
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
