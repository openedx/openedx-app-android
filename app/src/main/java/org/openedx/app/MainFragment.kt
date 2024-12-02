package org.openedx.app

import android.os.Bundle
import android.view.View
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
import org.openedx.core.adapter.NavigationFragmentAdapter
import org.openedx.core.presentation.global.appupgrade.UpgradeRequiredFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.learn.presentation.LearnFragment
import org.openedx.learn.presentation.LearnTab
import org.openedx.profile.presentation.profile.ProfileFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel by viewModel<MainViewModel>()
    private val router by inject<DiscoveryRouter>()

    private lateinit var adapter: NavigationFragmentAdapter

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

        initViewPager()

        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.fragmentLearn -> {
                    viewModel.logLearnTabClickedEvent()
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.fragmentDiscover -> {
                    viewModel.logDiscoveryTabClickedEvent()
                    binding.viewPager.setCurrentItem(1, false)
                }

                R.id.fragmentProfile -> {
                    viewModel.logProfileTabClickedEvent()
                    binding.viewPager.setCurrentItem(2, false)
                }
            }
            true
        }

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

        requireArguments().apply {
            getString(ARG_COURSE_ID).takeIf { it.isNullOrBlank().not() }?.let { courseId ->
                val infoType = getString(ARG_INFO_TYPE)

                if (viewModel.isDiscoveryTypeWebView && infoType != null) {
                    router.navigateToCourseInfo(parentFragmentManager, courseId, infoType)
                } else {
                    router.navigateToCourseDetail(parentFragmentManager, courseId)
                }

                // Clear arguments after navigation
                putString(ARG_COURSE_ID, "")
                putString(ARG_INFO_TYPE, "")
            }

            when (requireArguments().getString(ARG_OPEN_TAB, "")) {
                HomeTab.LEARN.name,
                HomeTab.PROGRAMS.name -> {
                    binding.bottomNavView.selectedItemId = R.id.fragmentLearn
                }

                HomeTab.DISCOVER.name -> {
                    binding.bottomNavView.selectedItemId = R.id.fragmentDiscover
                }

                HomeTab.PROFILE.name -> {
                    binding.bottomNavView.selectedItemId = R.id.fragmentProfile
                }
            }
            requireArguments().remove(ARG_OPEN_TAB)
        }
    }

    @Suppress("MagicNumber")
    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 4

        val openTab = requireArguments().getString(ARG_OPEN_TAB, HomeTab.LEARN.name)
        val learnTab = if (openTab == HomeTab.PROGRAMS.name) {
            LearnTab.PROGRAMS
        } else {
            LearnTab.COURSES
        }
        adapter = NavigationFragmentAdapter(this).apply {
            addFragment(LearnFragment.newInstance(openTab = learnTab.name))
            addFragment(viewModel.getDiscoveryFragment)
            addFragment(ProfileFragment())
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    private fun enableBottomBar(enable: Boolean) {
        binding.bottomNavView.menu.forEach {
            it.isEnabled = enable
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
