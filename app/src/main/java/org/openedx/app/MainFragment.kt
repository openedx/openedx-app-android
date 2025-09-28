package org.openedx.app

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.app.databinding.FragmentMainBinding
import org.openedx.app.deeplink.HomeTab
import org.openedx.core.AppUpdateState
import org.openedx.core.AppUpdateState.wasUpgradeDialogClosed
import org.openedx.core.adapter.NavigationFragmentAdapter
import org.openedx.core.presentation.dialog.appupgrade.AppUpgradeDialogFragment
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRecommendedBox
import org.openedx.core.presentation.global.appupgrade.UpgradeRequiredFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.downloads.presentation.download.DownloadsFragment
import org.openedx.learn.presentation.LearnFragment
import org.openedx.learn.presentation.LearnTab
import org.openedx.profile.presentation.profile.ProfileFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel by viewModel<MainViewModel>()
    private val router by inject<DiscoveryRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        setFragmentResultListener(UpgradeRequiredFragment.REQUEST_KEY) { _, _ ->
            binding.bottomNavView.selectedItemId = R.id.fragmentProfile
            viewModel.enableBottomBar(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleArguments()
        setupBottomNavigation()
        setupViewPager()
        setupBottomPopup()
        observeViewModel()
    }

    private fun handleArguments() {
        requireArguments().apply {
            getString(ARG_COURSE_ID).takeIf { it.isNullOrBlank().not() }?.let { courseId ->
                val infoType = getString(ARG_INFO_TYPE)
                if (viewModel.isDiscoveryTypeWebView && infoType != null) {
                    router.navigateToCourseInfo(parentFragmentManager, courseId, infoType)
                } else {
                    router.navigateToCourseDetail(parentFragmentManager, courseId)
                }
                putString(ARG_COURSE_ID, "")
                putString(ARG_INFO_TYPE, "")
            }
        }
    }

    private fun setupBottomNavigation() {
        val openTabArg = requireArguments().getString(ARG_OPEN_TAB, HomeTab.LEARN.name)
        val initialMenuId = getInitialMenuId(openTabArg)
        binding.bottomNavView.selectedItemId = initialMenuId

        val menu = binding.bottomNavView.menu
        menu.clear()

        val tabList = createTabList(openTabArg)
        addMenuItems(menu, tabList)
        setupBottomNavListener(tabList)

        requireArguments().remove(ARG_OPEN_TAB)
    }

    private fun createTabList(openTabArg: String): List<Pair<Int, () -> Fragment>> {
        val learnFragmentFactory = {
            LearnFragment.newInstance(
                openTab = if (openTabArg == HomeTab.PROGRAMS.name) {
                    LearnTab.PROGRAMS.name
                } else {
                    LearnTab.COURSES.name
                }
            )
        }

        return mutableListOf<Pair<Int, () -> Fragment>>().apply {
            add(R.id.fragmentLearn to learnFragmentFactory)
            add(R.id.fragmentDiscover to { viewModel.getDiscoveryFragment })
            if (viewModel.isDownloadsFragmentEnabled) {
                add(R.id.fragmentDownloads to { DownloadsFragment() })
            }
            add(R.id.fragmentProfile to { ProfileFragment() })
        }
    }

    private fun addMenuItems(menu: Menu, tabList: List<Pair<Int, () -> Fragment>>) {
        val tabTitles = mapOf(
            R.id.fragmentLearn to resources.getString(R.string.app_navigation_learn),
            R.id.fragmentDiscover to resources.getString(R.string.app_navigation_discovery),
            R.id.fragmentDownloads to resources.getString(R.string.app_navigation_downloads),
            R.id.fragmentProfile to resources.getString(R.string.app_navigation_profile),
        )
        val tabIconSelectors = mapOf(
            R.id.fragmentLearn to R.drawable.app_ic_learn_selector,
            R.id.fragmentDiscover to R.drawable.app_ic_discover_selector,
            R.id.fragmentDownloads to R.drawable.app_ic_downloads_selector,
            R.id.fragmentProfile to R.drawable.app_ic_profile_selector
        )

        for ((id, _) in tabList) {
            val menuItem = menu.add(Menu.NONE, id, Menu.NONE, tabTitles[id] ?: "")
            tabIconSelectors[id]?.let { menuItem.setIcon(it) }
        }
    }

    private fun setupBottomNavListener(tabList: List<Pair<Int, () -> Fragment>>) {
        val menuIdToIndex = tabList.mapIndexed { index, pair -> pair.first to index }.toMap()

        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.fragmentLearn -> viewModel.logLearnTabClickedEvent()
                R.id.fragmentDiscover -> viewModel.logDiscoveryTabClickedEvent()
                R.id.fragmentDownloads -> viewModel.logDownloadsTabClickedEvent()
                R.id.fragmentProfile -> viewModel.logProfileTabClickedEvent()
            }
            menuIdToIndex[menuItem.itemId]?.let { index ->
                binding.viewPager.setCurrentItem(index, false)
            }
            true
        }
    }

    private fun setupViewPager() {
        val tabList = createTabList(requireArguments().getString(ARG_OPEN_TAB, HomeTab.LEARN.name))
        initViewPager(tabList)
    }

    private fun observeViewModel() {
        viewModel.isBottomBarEnabled.observe(viewLifecycleOwner) { isBottomBarEnabled ->
            enableBottomBar(isBottomBarEnabled)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigateToDiscovery.collect { shouldNavigateToDiscovery ->
                if (shouldNavigateToDiscovery) {
                    binding.bottomNavView.selectedItemId = R.id.fragmentDiscover
                }
            }
        }
    }

    private fun getInitialMenuId(openTabArg: String): Int {
        return when (openTabArg) {
            HomeTab.LEARN.name, HomeTab.PROGRAMS.name -> R.id.fragmentLearn
            HomeTab.DISCOVER.name -> R.id.fragmentDiscover
            HomeTab.DOWNLOADS.name -> if (viewModel.isDownloadsFragmentEnabled) {
                R.id.fragmentDownloads
            } else {
                R.id.fragmentLearn
            }

            HomeTab.PROFILE.name -> R.id.fragmentProfile
            else -> R.id.fragmentLearn
        }
    }

    private fun initViewPager(tabList: List<Pair<Int, () -> Fragment>>) {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = tabList.size
        binding.viewPager.adapter = NavigationFragmentAdapter(this).apply {
            tabList.forEach { (_, fragmentFactory) ->
                // Use fragment factory to prevent memory leaks
                addFragment { fragmentFactory() }
            }
        }
        binding.viewPager.isUserInputEnabled = false
    }

    private fun enableBottomBar(enable: Boolean) {
        binding.bottomNavView.menu.forEach {
            it.isEnabled = enable
        }
    }

    private fun setupBottomPopup() {
        binding.composeBottomPopup.setContent {
            val appUpgradeEvent by viewModel.appUpgradeEvent.observeAsState()
            val wasUpgradeDialogClosed by remember { wasUpgradeDialogClosed }
            val appUpgradeParameters = AppUpdateState.AppUpgradeParameters(
                appUpgradeEvent = appUpgradeEvent,
                wasUpgradeDialogClosed = wasUpgradeDialogClosed,
                appUpgradeRecommendedDialog = {
                    val dialog = AppUpgradeDialogFragment.newInstance()
                    dialog.show(
                        requireActivity().supportFragmentManager,
                        AppUpgradeDialogFragment::class.simpleName
                    )
                },
                onAppUpgradeRecommendedBoxClick = {
                    AppUpdateState.openPlayMarket(requireContext())
                },
                onAppUpgradeRequired = {
                    router.navigateToUpgradeRequired(
                        requireActivity().supportFragmentManager
                    )
                }
            )
            when (appUpgradeParameters.appUpgradeEvent) {
                is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                    if (appUpgradeParameters.wasUpgradeDialogClosed) {
                        AppUpgradeRecommendedBox(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = appUpgradeParameters.onAppUpgradeRecommendedBoxClick
                        )
                    } else {
                        if (!AppUpdateState.wasUpdateDialogDisplayed) {
                            AppUpdateState.wasUpdateDialogDisplayed = true
                            appUpgradeParameters.appUpgradeRecommendedDialog()
                        }
                    }
                }

                is AppUpgradeEvent.UpgradeRequiredEvent -> {
                    if (!AppUpdateState.wasUpdateDialogDisplayed) {
                        AppUpdateState.wasUpdateDialogDisplayed = true
                        appUpgradeParameters.onAppUpgradeRequired()
                    }
                }

                else -> {}
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_INFO_TYPE = "info_type"
        private const val ARG_OPEN_TAB = "open_tab"
        fun newInstance(
            courseId: String? = null,
            infoType: String? = null,
            openTab: String = HomeTab.LEARN.name
        ): MainFragment {
            val fragment = MainFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_INFO_TYPE to infoType,
                ARG_OPEN_TAB to openTab
            )
            return fragment
        }
    }
}
