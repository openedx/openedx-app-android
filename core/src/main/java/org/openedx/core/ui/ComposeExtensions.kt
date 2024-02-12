package org.openedx.core.ui

import android.content.res.Configuration
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.openedx.core.presentation.global.InsetHolder

inline val isPreview: Boolean
    @ReadOnlyComposable
    @Composable
    get() {
        return LocalInspectionMode.current
    }

@Stable
inline val Int.px: Float
    @Composable
    get() {
        return this.toFloat().px
    }

@Stable
inline val Float.px: Float
    @Composable
    get() {
        val density = LocalDensity.current.density
        return this * density
    }

fun LazyListState.shouldLoadMore(rememberedIndex: MutableState<Int>, threshold: Int): Boolean {
    val firstVisibleIndex = this.firstVisibleItemIndex
    if (rememberedIndex.value != firstVisibleIndex) {
        rememberedIndex.value = firstVisibleIndex
        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        return lastVisibleIndex >= layoutInfo.totalItemsCount - 1 - threshold
    }
    return false
}

fun Modifier.statusBarsInset(): Modifier = composed {
    val topInset = (LocalContext.current as? InsetHolder)?.topInset ?: 0
    return@composed this
        .padding(top = with(LocalDensity.current) { topInset.toDp() })
}

fun Modifier.displayCutoutForLandscape(): Modifier = composed {
    val cutoutInset = (LocalContext.current as? InsetHolder)?.cutoutInset ?: 0
    val cutoutInsetDp = with(LocalDensity.current) { cutoutInset.toDp() }
    return@composed if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        this.padding(horizontal = cutoutInsetDp)
    } else {
        this
    }
}

inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun Modifier.roundBorderWithoutBottom(borderWidth: Dp, cornerRadius: Dp): Modifier = composed(
    factory = {
        var path: Path
        this.then(
            Modifier.drawWithCache {
                val height = this.size.height
                val width = this.size.width
                onDrawWithContent {
                    drawContent()
                    path = Path().apply {
                        moveTo(width.times(0f), height.times(1f))
                        lineTo(width.times(0f), height.times(0f))
                        lineTo(width.times(1f), height.times(0f))
                        lineTo(width.times(1f), height.times(1f))
                    }
                    drawPath(
                        path = path,
                        color = Color.LightGray,
                        style = Stroke(
                            width = borderWidth.toPx(),
                            pathEffect = PathEffect.cornerPathEffect(cornerRadius.toPx())
                        )
                    )
                }
            }
        )
    }
)

@Composable
fun <T : Any> rememberSaveableMap(init: () -> MutableMap<String, T?>): MutableMap<String, T?> {
    return rememberSaveable(
        saver = mapSaver(
            save = {
                it.toMap()
            },
            restore = {
                it.toMutableMap() as MutableMap<String, T?>
            }
        )
    ) {
        init()
    }
}

@Composable
fun isImeVisibleState(): State<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardState.value = keypadHeight > screenHeight * 0.15
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}

fun LazyListState.disableScrolling(scope: CoroutineScope) {
    scope.launch {
        scroll(scrollPriority = MutatePriority.PreventUserInput) {
            awaitCancellation()
        }
    }
}

fun LazyListState.reEnableScrolling(scope: CoroutineScope) {
    scope.launch {
        scroll(scrollPriority = MutatePriority.PreventUserInput) {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}