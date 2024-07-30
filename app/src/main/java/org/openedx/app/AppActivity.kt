package org.openedx.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.window.layout.WindowMetricsCalculator
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchUniversalReferralInitListener
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.app.databinding.ActivityAppBinding
import org.openedx.auth.presentation.logistration.LogistrationFragment
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.extension.requestApplyInsetsWhenAttached
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.presentation.global.WindowSizeHolder
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.utils.Logger
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewFragment

class AppActivity : AppCompatActivity(), InsetHolder, WindowSizeHolder {

    override val topInset: Int
        get() = _insetTop
    override val bottomInset: Int
        get() = _insetBottom
    override val cutoutInset: Int
        get() = _insetCutout

    override val windowSize: WindowSize
        get() = _windowSize

    private lateinit var binding: ActivityAppBinding
    private val viewModel by viewModel<AppViewModel>()
    private val whatsNewManager by inject<WhatsNewManager>()
    private val corePreferencesManager by inject<CorePreferences>()
    private val profileRouter by inject<ProfileRouter>()

    private val branchLogger = Logger(BRANCH_TAG)

    private var _insetTop = 0
    private var _insetBottom = 0
    private var _insetCutout = 0

    private var _windowSize = WindowSize(WindowType.Compact, WindowType.Compact)
    private val authCode: String?
        get() {
            val data = intent?.data
            if (data is Uri && data.scheme == BuildConfig.APPLICATION_ID && data.host == "oauth2Callback") {
                return data.getQueryParameter("code")
            }
            return null
        }

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
        viewModel.logAppLaunchEvent()
        setContentView(binding.root)
        val container = binding.rootLayout

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
            when {
                corePreferencesManager.user == null -> {
                    val authCode = authCode;
                    if (viewModel.isLogistrationEnabled && authCode == null) {
                        addFragment(LogistrationFragment())
                    } else {
                        val bundle = Bundle()
                        bundle.putString("auth_code", authCode)
                        val fragment = SignInFragment()
                        fragment.arguments = bundle
                        addFragment(fragment)
                    }
                }

                whatsNewManager.shouldShowWhatsNew() -> {
                    addFragment(WhatsNewFragment.newInstance())
                }

                corePreferencesManager.user != null -> {
                    addFragment(MainFragment.newInstance())
                }
            }
        }

        viewModel.logoutUser.observe(this) {
            profileRouter.restartApp(supportFragmentManager, viewModel.isLogistrationEnabled)
        }
    }

    override fun onStart() {
        super.onStart()

        if (viewModel.isBranchEnabled) {
            val callback = BranchUniversalReferralInitListener { _, linkProperties, error ->
                if (linkProperties != null) {
                    branchLogger.i { "Branch init complete." }
                    branchLogger.i { linkProperties.controlParams.toString() }
                } else if (error != null) {
                    branchLogger.e { "Branch init failed. Caused by -" + error.message }
                }
            }

            Branch.sessionBuilder(this)
                .withCallback(callback)
                .withData(this.intent.data)
                .init()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent

        if (viewModel.isBranchEnabled) {
            if (intent?.getBooleanExtra(BRANCH_FORCE_NEW_SESSION, false) == true) {
                Branch.sessionBuilder(this).withCallback { referringParams, error ->
                    if (error != null) {
                        branchLogger.e { error.message }
                    } else if (referringParams != null) {
                        branchLogger.i { referringParams.toString() }
                    }
                }.reInit()
            }
        }
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment)
            .commit()
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val widthWindowSize = when {
            widthDp < 600f -> WindowType.Compact
            widthDp < 840f -> WindowType.Medium
            else -> WindowType.Expanded
        }

        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        val heightWindowSize = when {
            heightDp < 480f -> WindowType.Compact
            heightDp < 900f -> WindowType.Medium
            else -> WindowType.Expanded
        }
        _windowSize = WindowSize(widthWindowSize, heightWindowSize)
    }

    private fun isUsingNightModeResources(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
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
        const val BRANCH_TAG = "Branch"
        const val BRANCH_FORCE_NEW_SESSION = "branch_force_new_session"
    }
}
