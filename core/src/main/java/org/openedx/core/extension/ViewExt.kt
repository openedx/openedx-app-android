package org.openedx.core.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openedx.core.system.AppCookieManager

fun Context.dpToPixel(dp: Int): Float {
    return dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.dpToPixel(dp: Float): Float {
    return dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun Context.toastMessage(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun WebView.loadUrl(url: String, scope: CoroutineScope, cookieManager: AppCookieManager) {
    if (cookieManager.isSessionCookieMissingOrExpired()) {
        scope.launch {
            cookieManager.tryToRefreshSessionCookie()
            loadUrl(url)
        }
    } else {
        loadUrl(url)
    }
}

fun WebView.applyDarkModeIfEnabled(isDarkTheme: Boolean) {
    if (isDarkTheme && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.setAlgorithmicDarkeningAllowed(true)
        } else {
            // Switch WebView to dark mode; uses default dark theme
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(
                    settings,
                    WebSettingsCompat.FORCE_DARK_ON
                )
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(
                    settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
        }
    }
}
