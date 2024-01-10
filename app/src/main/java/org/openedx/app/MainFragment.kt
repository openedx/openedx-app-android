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
import org.openedx.app.adapter.MainNavigationFragmentAdapter
import org.openedx.app.databinding.FragmentMainBinding
import org.openedx.core.presentation.global.app_upgrade.UpgradeRequiredFragment
import org.openedx.core.presentation.global.viewBinding
import org.openedx.dashboard.presentation.dashboard.DashboardFragment
import org.openedx.dashboard.presentation.program.ProgramFragment
import org.openedx.discovery.presentation.DiscoveryNavigator
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.profile.presentation.profile.ProfileFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val analytics by inject<AppAnalytics>()
    private val viewModel by viewModel<MainViewModel>()
    private val router by inject<DiscoveryRouter>()

    private lateinit var adapter: MainNavigationFragmentAdapter

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
                R.id.fragmentHome -> {
                    analytics.discoveryTabClickedEvent()
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.fragmentDashboard -> {
                    analytics.dashboardTabClickedEvent()
                    binding.viewPager.setCurrentItem(1, false)
                }

                R.id.fragmentPrograms -> {
                    analytics.programsTabClickedEvent()
                    binding.viewPager.setCurrentItem(2, false)
                }

                R.id.fragmentProfile -> {
                    analytics.profileTabClickedEvent()
                    binding.viewPager.setCurrentItem(3, false)
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
                    binding.bottomNavView.selectedItemId = R.id.fragmentHome
                    binding.viewPager.setCurrentItem(0, true)
                }
            }
        }

        requireArguments().apply {
            this.getString(ARG_COURSE_ID, null)?.apply {
                router.navigateToCourseDetail(parentFragmentManager, this)
            }
            this.putString(ARG_COURSE_ID, null)
        }
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 4

        val discoveryFragment = DiscoveryNavigator(viewModel.isDiscoveryTypeWebView)
            .getDiscoveryFragment()
        val programFragment = if (viewModel.isProgramTypeWebView) {
            ProgramFragment()
        } else {
            InDevelopmentFragment()
        }

        adapter = MainNavigationFragmentAdapter(this).apply {
            addFragment(discoveryFragment)
            addFragment(DashboardFragment())
            addFragment(programFragment)
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
        fun newInstance(courseId: String? = null): MainFragment {
            val fragment = MainFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }
}
