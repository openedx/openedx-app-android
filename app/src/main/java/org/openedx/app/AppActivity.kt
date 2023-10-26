package org.openedx.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.window.layout.WindowMetricsCalculator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.app.databinding.ActivityAppBinding
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.extension.requestApplyInsetsWhenAttached
import org.openedx.core.presentation.dialog.SelectBottomDialogFragment
import org.openedx.core.presentation.dialog.app_review.RateDialogFragment
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.AppDataHolder
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.presentation.global.WindowSizeHolder
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.profile.presentation.ProfileRouter

class AppActivity : AppCompatActivity(), InsetHolder, WindowSizeHolder, AppDataHolder {

    override val topInset: Int
        get() = _insetTop
    override val bottomInset: Int
        get() = _insetBottom
    override val cutoutInset: Int
        get() = _insetCutout

    override val windowSize: WindowSize
        get() = _windowSize

    override val appData: AppData
        get() = AppData(BuildConfig.VERSION_NAME)

    private lateinit var binding: ActivityAppBinding
    private val preferencesManager by inject<CorePreferences>()
    private val viewModel by viewModel<AppViewModel>()
    private val profileRouter by inject<ProfileRouter>()

    private var _insetTop = 0
    private var _insetBottom = 0
    private var _insetCutout = 0

    private var _windowSize = WindowSize(WindowType.Compact, WindowType.Compact)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(TOP_INSET, topInset)
        outState.putInt(BOTTOM_INSET, bottomInset)
        outState.putInt(CUTOUT_INSET, cutoutInset)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityAppBinding.inflate(layoutInflater)
        lifecycle.addObserver(viewModel)
        setContentView(binding.root)
        val container = binding.rootLayout

        //TODO remove
        val dialog = RateDialogFragment.newInstance()
        dialog.show(
            supportFragmentManager,
            SelectBottomDialogFragment::class.simpleName
        )

        container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()

        if (savedInstanceState != null) {
            _insetTop = savedInstanceState.getInt(TOP_INSET, 0)
            _insetBottom = savedInstanceState.getInt(BOTTOM_INSET, 0)
            _insetCutout = savedInstanceState.getInt(CUTOUT_INSET, 0)
        }

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            WindowCompat.setDecorFitsSystemWindows(this, false)

            val insetsController = WindowInsetsControllerCompat(this, binding.root)
            insetsController.isAppearanceLightStatusBars = !isUsingNightModeResources()
            statusBarColor = Color.TRANSPARENT
        }

        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())

            _insetTop = insetsCompat.top
            _insetBottom = insetsCompat.bottom

            val displayCutout = WindowInsetsCompat.toWindowInsetsCompat(insets).displayCutout
            if (displayCutout != null) {
                val top = displayCutout.safeInsetTop
                val left = displayCutout.safeInsetLeft
                val right = displayCutout.safeInsetRight
                _insetCutout = maxOf(top, left, right)
            }

            insets
        }
        binding.root.requestApplyInsetsWhenAttached()

        if (savedInstanceState == null) {
            if (preferencesManager.user != null) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.container, MainFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .add(R.id.container, SignInFragment())
                    .commit()
            }
        }

        viewModel.logoutUser.observe(this) {
            profileRouter.restartApp(supportFragmentManager)
        }
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() /
                resources.displayMetrics.density
        val widthWindowSize = when {
            widthDp < 600f -> WindowType.Compact
            widthDp < 840f -> WindowType.Medium
            else -> WindowType.Expanded
        }

        val heightDp = metrics.bounds.height() /
                resources.displayMetrics.density
        val heightWindowSize = when {
            heightDp < 480f -> WindowType.Compact
            heightDp < 900f -> WindowType.Medium
            else -> WindowType.Expanded
        }
        _windowSize = WindowSize(widthWindowSize, heightWindowSize)
    }

    private fun isUsingNightModeResources(): Boolean {
        return when (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    companion object {
        const val TOP_INSET = "topInset"
        const val BOTTOM_INSET = "bottomInset"
        const val CUTOUT_INSET = "cutoutInset"
    }
}
