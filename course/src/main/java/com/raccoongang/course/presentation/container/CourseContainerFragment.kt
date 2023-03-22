package com.raccoongang.course.presentation.container

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.raccoongang.core.domain.model.Certificate
import com.raccoongang.core.extension.parcelable
import com.raccoongang.core.presentation.global.viewBinding
import com.raccoongang.course.R
import com.raccoongang.course.databinding.FragmentCourseContainerBinding
import com.raccoongang.course.presentation.handouts.HandoutsFragment
import com.raccoongang.course.presentation.outline.CourseOutlineFragment
import com.raccoongang.course.presentation.videos.CourseVideosFragment
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CourseContainerFragment : Fragment(R.layout.fragment_course_container) {

    private val binding by viewBinding(FragmentCourseContainerBinding::bind)
    private val viewModel by viewModel<CourseContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private var adapter: CourseNavigationFragmentAdapter? = null

    private var courseTitle = ""
    private var courseImage = ""
    private var courseCertificate = Certificate("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(requireArguments()) {
            courseImage = getString(ARG_IMAGE, "")
            courseTitle = getString(ARG_TITLE, "")
            courseCertificate = parcelable(ARG_CERTIFICATE)!!
        }
        viewModel.preloadCourseStructure()
    }

    private var snackBar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe()

        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.outline -> binding.viewPager.setCurrentItem(0, false)
                R.id.videos -> binding.viewPager.setCurrentItem(1, false)
                R.id.discussions -> binding.viewPager.setCurrentItem(2, false)
                R.id.resources -> binding.viewPager.setCurrentItem(3, false)
            }
            true
        }
    }

    override fun onDestroyView() {
        snackBar?.dismiss()
        super.onDestroyView()
    }

    private fun observe() {
        viewModel.dataReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                binding.viewPager.isVisible = true
                binding.bottomNavView.isVisible = true
                initViewPager()
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            snackBar = Snackbar.make(binding.root, it, Snackbar.LENGTH_INDEFINITE)
                .setAction(com.raccoongang.core.R.string.core_error_try_again) {
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
        binding.viewPager.offscreenPageLimit = 4
        adapter = CourseNavigationFragmentAdapter(this).apply {
            addFragment(CourseOutlineFragment.newInstance(viewModel.courseId,courseTitle, courseImage, courseCertificate))
            addFragment(CourseVideosFragment.newInstance(viewModel.courseId,courseTitle, courseImage, courseCertificate))
            addFragment(DiscussionTopicsFragment.newInstance(viewModel.courseId))
            addFragment(HandoutsFragment.newInstance(viewModel.courseId))
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    fun updateCourseStructure(withSwipeRefresh: Boolean){
        viewModel.updateData(withSwipeRefresh)
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE = "image"
        private const val ARG_CERTIFICATE = "certificate"
        fun newInstance(
            courseId: String,
            title: String,
            image: String,
            certificate: Certificate
        ): CourseContainerFragment {
            val fragment = CourseContainerFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to title,
                ARG_IMAGE to image,
                ARG_CERTIFICATE to certificate
            )
            return fragment
        }
    }
}