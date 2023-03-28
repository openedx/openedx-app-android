package com.raccoongang.newedx

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.raccoongang.auth.presentation.AuthRouter
import com.raccoongang.auth.presentation.restore.RestorePasswordFragment
import com.raccoongang.auth.presentation.signin.SignInFragment
import com.raccoongang.auth.presentation.signup.SignUpFragment
import com.raccoongang.core.FragmentViewType
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.domain.model.CoursewareAccess
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.course.presentation.container.CourseContainerFragment
import com.raccoongang.course.presentation.container.NoAccessCourseContainerFragment
import com.raccoongang.course.presentation.detail.CourseDetailsFragment
import com.raccoongang.course.presentation.handouts.HandoutsType
import com.raccoongang.course.presentation.handouts.WebViewFragment
import com.raccoongang.discovery.presentation.search.CourseSearchFragment
import com.raccoongang.course.presentation.section.CourseSectionFragment
import com.raccoongang.course.presentation.unit.container.CourseUnitContainerFragment
import com.raccoongang.course.presentation.unit.video.VideoFullScreenFragment
import com.raccoongang.course.presentation.unit.video.YoutubeVideoFullScreenFragment
import com.raccoongang.course.presentation.units.CourseUnitsFragment
import com.raccoongang.dashboard.presentation.DashboardRouter
import com.raccoongang.discovery.presentation.DiscoveryRouter
import com.raccoongang.discussion.domain.model.DiscussionComment
import com.raccoongang.discussion.domain.model.Thread
import com.raccoongang.discussion.presentation.DiscussionRouter
import com.raccoongang.discussion.presentation.comments.DiscussionCommentsFragment
import com.raccoongang.discussion.presentation.responses.DiscussionResponsesFragment
import com.raccoongang.discussion.presentation.search.DiscussionSearchThreadFragment
import com.raccoongang.discussion.presentation.threads.DiscussionAddThreadFragment
import com.raccoongang.discussion.presentation.threads.DiscussionThreadsFragment
import com.raccoongang.profile.presentation.ProfileRouter
import com.raccoongang.profile.presentation.delete.DeleteProfileFragment
import com.raccoongang.profile.presentation.edit.EditProfileFragment
import com.raccoongang.profile.presentation.settings.video.VideoQualityFragment
import com.raccoongang.profile.presentation.settings.video.VideoSettingsFragment
import java.util.*

class AppRouter : AuthRouter, DiscoveryRouter, DashboardRouter, CourseRouter, DiscussionRouter,
    ProfileRouter {

    //region AuthRouter
    override fun navigateToMain(fm: FragmentManager) {
        fm.popBackStack()
        fm.beginTransaction()
            .replace(R.id.container, MainFragment())
            .commit()
    }

    override fun navigateToSignUp(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, SignUpFragment())
    }

    override fun navigateToRestorePassword(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, RestorePasswordFragment())
    }
    //endregion

    //region DiscoveryRouter
    override fun navigateToCourseDetail(fm: FragmentManager, courseId: String) {
        replaceFragmentWithBackStack(fm, CourseDetailsFragment.newInstance(courseId))
    }

    override fun navigateToCourseSearch(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, CourseSearchFragment())
    }
    //endregion

    //region DashboardRouter

//    override fun navigateToCourseOutline(
//        fm: FragmentManager,
//        courseId: String,
//        title: String,
//        image: String,
//        certificate: Certificate,
//        coursewareAccess: CoursewareAccess,
//        auditAccessExpires: Date?
//    ) {
//        val destinationFragment = if (coursewareAccess.hasAccess) {
//            CourseContainerFragment.newInstance(courseId, title, image, certificate)
//        } else {
//            NoAccessCourseContainerFragment.newInstance(title, coursewareAccess, auditAccessExpires)
//        }
//
//        replaceFragmentWithBackStack(
//            fm,
//            destinationFragment
//        )
//    }

    override fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseContainerFragment.newInstance(courseId, courseTitle)
        )
    }

    override fun navigateToNoAccess(
        fm: FragmentManager,
        title: String,
        coursewareAccess: CoursewareAccess,
        auditAccessExpires: Date?
    ) {
        replaceFragment(fm, NoAccessCourseContainerFragment.newInstance(title,coursewareAccess, auditAccessExpires))
    }
    //endregion

    //region CourseRouter
    override fun navigateToCourseUnits(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        courseName: String,
        mode: CourseViewMode,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseUnitsFragment.newInstance(courseId, blockId, courseName, mode)
        )
    }

    override fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        title: String,
        mode: CourseViewMode,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseSectionFragment.newInstance(courseId, blockId, title, mode)
        )
    }

    override fun navigateToCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        courseName: String,
        mode: CourseViewMode
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseUnitContainerFragment.newInstance(blockId, courseId, courseName, mode)
        )
    }

    override fun navigateToFullScreenVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String
    ) {
        replaceFragmentWithBackStack(
            fm,
            VideoFullScreenFragment.newInstance(videoUrl, videoTime, blockId, courseId)
        )
    }

    override fun navigateToFullScreenYoutubeVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String
    ) {
        replaceFragmentWithBackStack(
            fm,
            YoutubeVideoFullScreenFragment.newInstance(videoUrl, videoTime, blockId, courseId)
        )
    }

    override fun navigateToHandoutsWebView(
        fm: FragmentManager,
        courseId: String,
        title: String,
        type: HandoutsType
    ) {
        replaceFragmentWithBackStack(
            fm,
            WebViewFragment.newInstance(title, type.name, courseId)
        )
    }
    //endregion

    //region DiscussionRouter
    override fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        title: String,
        viewType: FragmentViewType
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionThreadsFragment.newInstance(action, courseId, topicId, title, viewType.name)
        )
    }

    override fun navigateToDiscussionComments(fm: FragmentManager, thread: Thread) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionCommentsFragment.newInstance(thread)
        )
    }

    override fun navigateToDiscussionResponses(fm: FragmentManager, comment: DiscussionComment) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionResponsesFragment.newInstance(comment)
        )
    }

    override fun navigateToAddThread(
        fm: FragmentManager,
        topicId: String,
        courseId: String,
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionAddThreadFragment.newInstance(topicId, courseId)
        )
    }

    override fun navigateToSearchThread(fm: FragmentManager, courseId: String) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionSearchThreadFragment.newInstance(courseId)
        )
    }
    //endregion

    //region ProfileRouter
    override fun navigateToEditProfile(fm: FragmentManager, account: Account) {
        replaceFragmentWithBackStack(fm, EditProfileFragment.newInstance(account))
    }

    override fun navigateToVideoSettings(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, VideoSettingsFragment())
    }

    override fun navigateToVideoQuality(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, VideoQualityFragment())
    }

    override fun navigateToDeleteAccount(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, DeleteProfileFragment())
    }

    override fun restartApp(fm: FragmentManager) {
        fm.apply {
            for (fragment in fragments) {
                beginTransaction().remove(fragment).commit()
            }
            popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            beginTransaction().replace(R.id.container, SignInFragment())
                .commit()
        }
    }
    //endregion

    private fun replaceFragmentWithBackStack(fm: FragmentManager, fragment: Fragment) {
        fm.beginTransaction()
            .replace(R.id.container, fragment, fragment.javaClass.simpleName)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    private fun replaceFragment(fm: FragmentManager, fragment: Fragment) {
        fm.beginTransaction()
            .replace(R.id.container, fragment, fragment.javaClass.simpleName)
            .commit()
    }
}