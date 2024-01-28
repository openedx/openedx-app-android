package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.global.viewBinding
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseContainerBinding
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.dates.CourseDatesFragment
import org.openedx.course.presentation.handouts.HandoutsFragment
import org.openedx.course.presentation.outline.CourseOutlineFragment
import org.openedx.course.presentation.ui.CourseToolbar
import org.openedx.course.presentation.videos.CourseVideosFragment
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment
import org.openedx.core.R as coreR

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)
    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(
            requireArguments().getString(ARG_COURSE_ID, ""),
            requireArguments().getString(ARG_TITLE, "")
        )
    }
    private val router by inject<CourseRouter>()

    private var adapter: CourseContainerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.preloadCourseStructure()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(viewModel.courseName)
        observe()
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady == true) {
                setupToolbar(viewModel.courseName)
                initViewPager()
            } else {
                router.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    viewModel.courseName
                )
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_INDEFINITE)
                .setAction(org.openedx.core.R.string.core_error_try_again) {
                    viewModel.preloadCourseStructure()
                }
            snackBar?.show()

        }
        viewModel.showProgress.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
    }

    private fun setupToolbar(courseName: String) {
        binding.toolbar.setContent {
            CourseToolbar(
                title = courseName,
                onBackClick = {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            )
        }
    }

    private fun initViewPager() {
        binding.viewPager.isVisible = true
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        adapter = CourseContainerAdapter(this).apply {
            addFragment(
                CourseContainerTab.OUTLINE,
                CourseOutlineFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                CourseContainerTab.VIDEOS,
                CourseVideosFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                CourseContainerTab.DISCUSSION,
                DiscussionTopicsFragment.newInstance(viewModel.courseId, viewModel.courseName)
            )
            addFragment(
                CourseContainerTab.DATES,
                CourseDatesFragment.newInstance(viewModel.courseId, viewModel.isSelfPaced)
            )
            addFragment(
                CourseContainerTab.HANDOUTS,
                HandoutsFragment.newInstance(viewModel.courseId)
            )
        }
        binding.viewPager.offscreenPageLimit = adapter?.itemCount ?: 1
        binding.viewPager.adapter = adapter

        if (viewModel.isCourseTopTabBarEnabled) {
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = getString(
                    CourseContainerTab.values().find { it.position == position }?.titleResId
                        ?: R.string.course_navigation_course
                )
            }.attach()
            binding.tabLayout.isVisible = true

        } else {
            binding.viewPager.isUserInputEnabled = false
            binding.bottomNavView.setOnItemSelectedListener { menuItem ->
                CourseContainerTab.values().find { menuItem.itemId == it.itemId }?.let { tab ->
                    viewModel.courseTabClickedEvent(tab)
                    binding.viewPager.setCurrentItem(tab.position, false)
                }
                true
            }
            binding.bottomNavView.isVisible = true
        }
    }

    fun updateCourseStructure(withSwipeRefresh: Boolean) {
        viewModel.updateData(withSwipeRefresh)
    }

    fun showDatesUpdateSnackbar(isSuccess: Boolean) {
        val message = if (isSuccess) {
            getString(coreR.string.core_dates_shift_dates_successfully_msg)
        } else {
            getString(coreR.string.core_dates_shift_dates_unsuccessful_msg)
        }
        val duration = if (isSuccess) {
            Snackbar.LENGTH_INDEFINITE
        } else {
            Snackbar.LENGTH_LONG
        }
        snackBar = Snackbar.make(binding.root, message, duration).also {
            if (isSuccess) {
                it.setAction(coreR.string.core_dates_view_all_dates) {
                    adapter?.getFragment(CourseContainerTab.DATES)?.let { fragment ->
                        binding.viewPager.setCurrentItem(CourseContainerTab.DATES.position, true)
                        (fragment as CourseDatesFragment).updateData()
                    }
                }
            }
        }
        snackBar?.show()
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        fun newInstance(
            courseId: String,
            courseTitle: String
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to courseTitle
            )
            return fragment
        }
    }
}
