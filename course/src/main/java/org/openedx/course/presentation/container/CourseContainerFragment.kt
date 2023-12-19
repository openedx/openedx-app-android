package org.openedx.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
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
import org.openedx.course.presentation.videos.CourseVideosFragment
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)
    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()


    private var adapter: CourseContainerAdapter? = null

    private var courseTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(requireArguments()) {
            courseTitle = getString(ARG_TITLE, "")
        }
        viewModel.preloadCourseStructure()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe()

        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.outline -> {
                    viewModel.courseTabClickedEvent()
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.videos -> {
                    viewModel.videoTabClickedEvent()
                    binding.viewPager.setCurrentItem(1, false)
                }

                R.id.discussions -> {
                    viewModel.discussionTabClickedEvent()
                    binding.viewPager.setCurrentItem(2, false)
                }

                R.id.dates -> {
                    viewModel.datesTabClickedEvent()
                    binding.viewPager.setCurrentItem(3, false)
                }

                R.id.resources -> {
                    viewModel.handoutsTabClickedEvent()
                    binding.viewPager.setCurrentItem(4, false)
                }
            }
            true
        }
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady == true) {
                binding.viewPager.isVisible = true
                binding.bottomNavView.isVisible = true
                initViewPager()
            } else {
                router.navigateToNoAccess(
                    requireActivity().supportFragmentManager,
                    courseTitle,
                    null
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

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        adapter = CourseContainerAdapter(this).apply {
            addFragment(CourseOutlineFragment.newInstance(viewModel.courseId, courseTitle))
            addFragment(CourseVideosFragment.newInstance(viewModel.courseId, courseTitle))
            addFragment(DiscussionTopicsFragment.newInstance(viewModel.courseId, courseTitle))
            addFragment(CourseDatesFragment.newInstance(viewModel.courseId, courseTitle))
            addFragment(HandoutsFragment.newInstance(viewModel.courseId))
        }
        binding.viewPager.offscreenPageLimit = adapter?.itemCount ?: 1
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    fun updateCourseStructure(withSwipeRefresh: Boolean) {
        viewModel.updateData(withSwipeRefresh)
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