package org.openedx.app

import android.content.Intent
import android.content.res.Configuration
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
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.braze.support.toStringMap
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchUniversalReferralInitListener
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.app.databinding.ActivityAppBinding
import org.openedx.app.deeplink.DeepLink
import org.openedx.auth.presentation.logistration.LogistrationFragment
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.core.ApiConstants
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.presentation.global.WindowSizeHolder
import org.openedx.core.utils.Logger
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.foundation.extension.requestApplyInsetsWhenAttached
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
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
    private val downloadDialogManager by inject<DownloadDialogManager>()
    private val calendarSyncScheduler by inject<CalendarSyncScheduler>()

    private val branchLogger = Logger(BRANCH_TAG)

    private var _insetTop = 0
    private var _insetBottom = 0
    private var _insetCutout = 0

    private var _windowSize = WindowSize(WindowType.Compact, WindowType.Compact)
    private val authCode: String?
        get() {
            val data = intent?.data
            if (
                data is Uri &&
                data.scheme == BuildConfig.APPLICATION_ID &&
                data.host == ApiConstants.BrowserLogin.REDIRECT_HOST
            ) {
                return data.getQueryParameter(ApiConstants.BrowserLogin.CODE_QUERY_PARAM)
            }
            return null
        }

    private val branchCallback =
        BranchUniversalReferralInitListener { branchUniversalObject, _, error ->
            if (branchUniversalObject?.contentMetadata?.customMetadata != null) {
                branchLogger.i { "Branch init complete." }
                branchLogger.i { branchUniversalObject.contentMetadata.customMetadata.toString() }
                viewModel.makeExternalRoute(
                    fm = supportFragmentManager,
                    deepLink = DeepLink(branchUniversalObject.contentMetadata.customMetadata)
                )
            } else if (error != null) {
                branchLogger.e { "Branch init failed. Caused by -" + error.message }
            }
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

        setupWindowInsets(savedInstanceState)
        setupWindowSettings()
        setupInitialFragment(savedInstanceState)
        observeLogoutEvent()
        observeDownloadFailedDialog()

        calendarSyncScheduler.scheduleDailySync()
    }

    private fun setupWindowInsets(savedInstanceState: Bundle?) {
        val container = binding.rootLayout
        container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()

        savedInstanceState?.let {
            _insetTop = it.getInt(TOP_INSET, 0)
            _insetBottom = it.getInt(BOTTOM_INSET, 0)
            _insetCutout = it.getInt(CUTOUT_INSET, 0)
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
    }

    private fun setupWindowSettings() {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            WindowCompat.setDecorFitsSystemWindows(this, false)
            val insetsController = WindowInsetsControllerCompat(this, binding.root)
            insetsController.isAppearanceLightStatusBars = !isUsingNightModeResources()
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupInitialFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            when {
                corePreferencesManager.user == null -> {
                    val fragment = if (viewModel.isLogistrationEnabled && authCode == null) {
                        LogistrationFragment()
                    } else {
                        SignInFragment.newInstance(null, null, authCode = authCode)
                    }
                    addFragment(fragment)
                }

                whatsNewManager.shouldShowWhatsNew() -> addFragment(WhatsNewFragment.newInstance())
                else -> addFragment(MainFragment.newInstance())
            }

            intent.extras?.takeIf { it.containsKey(DeepLink.Keys.NOTIFICATION_TYPE.value) }?.let {
                handlePushNotification(it)
            }
        }
    }

    private fun observeLogoutEvent() {
        viewModel.logoutUser.observe(this) {
            profileRouter.restartApp(supportFragmentManager, viewModel.isLogistrationEnabled)
        }
    }

    private fun observeDownloadFailedDialog() {
        lifecycleScope.launch {
            viewModel.downloadFailedDialog.collect {
                downloadDialogManager.showDownloadFailedPopup(
                    downloadModel = it.downloadModel,
                    fragmentManager = supportFragmentManager,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (viewModel.isBranchEnabled) {
            Branch.sessionBuilder(this)
                .withCallback(branchCallback)
                .withData(this.intent.data)
                .init()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent

        if (authCode != null) {
            addFragment(SignInFragment.newInstance(null, null, authCode = authCode))
        }

        val extras = intent.extras
        if (extras?.containsKey(DeepLink.Keys.NOTIFICATION_TYPE.value) == true) {
            handlePushNotification(extras)
        }

        if (viewModel.isBranchEnabled) {
            if (intent.getBooleanExtra(BRANCH_FORCE_NEW_SESSION, false)) {
                Branch.sessionBuilder(this)
                    .withCallback(branchCallback)
                    .reInit()
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
            widthDp < COMPACT_MAX_WIDTH -> WindowType.Compact
            widthDp < MEDIUM_MAX_WIDTH -> WindowType.Medium
            else -> WindowType.Expanded
        }

        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        val heightWindowSize = when {
            heightDp < COMPACT_MAX_HEIGHT -> WindowType.Compact
            heightDp < MEDIUM_MAX_HEIGHT -> WindowType.Medium
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

    private fun handlePushNotification(data: Bundle) {
        val deepLink = DeepLink(data.toStringMap())
        viewModel.makeExternalRoute(supportFragmentManager, deepLink)
    }

    companion object {
        const val TOP_INSET = "topInset"
        const val BOTTOM_INSET = "bottomInset"
        const val CUTOUT_INSET = "cutoutInset"
        const val BRANCH_TAG = "Branch"
        const val BRANCH_FORCE_NEW_SESSION = "branch_force_new_session"

        internal const val COMPACT_MAX_WIDTH = 600
        internal const val MEDIUM_MAX_WIDTH = 840
        internal const val COMPACT_MAX_HEIGHT = 480
        internal const val MEDIUM_MAX_HEIGHT = 900
    }
}
