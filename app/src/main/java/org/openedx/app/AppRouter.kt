package org.openedx.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.openedx.app.deeplink.HomeTab
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.logistration.LogistrationFragment
import org.openedx.auth.presentation.restore.RestorePasswordFragment
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.auth.presentation.signup.SignUpFragment
import org.openedx.core.CalendarRouter
import org.openedx.core.FragmentViewType
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRouter
import org.openedx.core.presentation.global.appupgrade.UpgradeRequiredFragment
import org.openedx.core.presentation.global.webview.WebContentFragment
import org.openedx.core.presentation.settings.video.VideoQualityFragment
import org.openedx.core.presentation.settings.video.VideoQualityType
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.container.NoAccessCourseContainerFragment
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.handouts.HandoutsWebViewFragment
import org.openedx.course.presentation.section.CourseSectionFragment
import org.openedx.course.presentation.unit.container.CourseUnitContainerFragment
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.course.presentation.unit.video.VideoFullScreenFragment
import org.openedx.course.presentation.unit.video.YoutubeVideoFullScreenFragment
import org.openedx.course.settings.download.DownloadQueueFragment
import org.openedx.courses.presentation.AllEnrolledCoursesFragment
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.NativeDiscoveryFragment
import org.openedx.discovery.presentation.WebViewDiscoveryFragment
import org.openedx.discovery.presentation.detail.CourseDetailsFragment
import org.openedx.discovery.presentation.info.CourseInfoFragment
import org.openedx.discovery.presentation.program.ProgramFragment
import org.openedx.discovery.presentation.search.CourseSearchFragment
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.comments.DiscussionCommentsFragment
import org.openedx.discussion.presentation.responses.DiscussionResponsesFragment
import org.openedx.discussion.presentation.search.DiscussionSearchThreadFragment
import org.openedx.discussion.presentation.threads.DiscussionAddThreadFragment
import org.openedx.discussion.presentation.threads.DiscussionThreadsFragment
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.presentation.anothersaccount.AnothersProfileFragment
import org.openedx.profile.presentation.calendar.CalendarFragment
import org.openedx.profile.presentation.calendar.CoursesToSyncFragment
import org.openedx.profile.presentation.delete.DeleteProfileFragment
import org.openedx.profile.presentation.edit.EditProfileFragment
import org.openedx.profile.presentation.manageaccount.ManageAccountFragment
import org.openedx.profile.presentation.profile.ProfileFragment
import org.openedx.profile.presentation.settings.SettingsFragment
import org.openedx.profile.presentation.video.VideoSettingsFragment
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewFragment

class AppRouter :
    AuthRouter,
    DiscoveryRouter,
    DashboardRouter,
    CourseRouter,
    DiscussionRouter,
    ProfileRouter,
    AppUpgradeRouter,
    WhatsNewRouter,
    CalendarRouter,
    DownloadsRouter {

    // region AuthRouter
    override fun navigateToMain(
        fm: FragmentManager,
        courseId: String?,
        infoType: String?,
        openTab: String
    ) {
        try {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(courseId, infoType, openTab))
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun navigateToSignIn(fm: FragmentManager, courseId: String?, infoType: String?) {
        replaceFragmentWithBackStack(fm, SignInFragment.newInstance(courseId, infoType))
    }

    override fun navigateToSignUp(fm: FragmentManager, courseId: String?, infoType: String?) {
        replaceFragmentWithBackStack(fm, SignUpFragment.newInstance(courseId, infoType))
    }

    override fun navigateToLogistration(fm: FragmentManager, courseId: String?) {
        replaceFragmentWithBackStack(fm, LogistrationFragment.newInstance(courseId))
    }

    override fun navigateToDownloadQueue(fm: FragmentManager, descendants: List<String>) {
        replaceFragmentWithBackStack(fm, DownloadQueueFragment.newInstance(descendants))
    }

    override fun navigateToRestorePassword(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, RestorePasswordFragment())
    }

    override fun navigateToNativeDiscoverCourses(fm: FragmentManager, querySearch: String) {
        replaceFragmentWithBackStack(fm, NativeDiscoveryFragment.newInstance(querySearch))
    }

    override fun navigateToWebDiscoverCourses(fm: FragmentManager, querySearch: String) {
        replaceFragmentWithBackStack(fm, WebViewDiscoveryFragment.newInstance(querySearch))
    }

    override fun navigateToWhatsNew(fm: FragmentManager, courseId: String?, infoType: String?) {
        try {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.container, WhatsNewFragment.newInstance(courseId, infoType))
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun clearBackStack(fm: FragmentManager) {
        fm.apply {
            try {
                for (fragment in fragments) {
                    beginTransaction().remove(fragment).commit()
                }
                popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // endregion

    // region DiscoveryRouter
    override fun navigateToCourseDetail(fm: FragmentManager, courseId: String) {
        replaceFragmentWithBackStack(fm, CourseDetailsFragment.newInstance(courseId))
    }

    override fun navigateToCourseSearch(fm: FragmentManager, querySearch: String) {
        replaceFragmentWithBackStack(fm, CourseSearchFragment.newInstance(querySearch))
    }

    override fun navigateToUpgradeRequired(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, UpgradeRequiredFragment())
    }

    override fun navigateToAllEnrolledCourses(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, AllEnrolledCoursesFragment())
    }

    override fun getProgramFragment(): Fragment {
        return ProgramFragment.newInstance(isNestedFragment = true)
    }

    override fun navigateToCourseInfo(
        fm: FragmentManager,
        courseId: String,
        infoType: String,
    ) {
        replaceFragmentWithBackStack(fm, CourseInfoFragment.newInstance(courseId, infoType))
    }

    override fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseContainerFragment.newInstance(courseId, courseTitle)
        )
    }
    // endregion

    // region DashboardRouter

    override fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        openTab: String,
        resumeBlockId: String,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseContainerFragment.newInstance(
                courseId,
                courseTitle,
                openTab,
                resumeBlockId
            )
        )
    }

    override fun navigateToEnrolledProgramInfo(fm: FragmentManager, pathId: String) {
        replaceFragmentWithBackStack(
            fm,
            ProgramFragment.newInstance(pathId = pathId, isNestedFragment = false)
        )
    }

    override fun navigateToNoAccess(
        fm: FragmentManager,
        title: String,
    ) {
        replaceFragment(fm, NoAccessCourseContainerFragment.newInstance(title))
    }
    // endregion

    // region CourseRouter

    override fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        subSectionId: String,
        unitId: String,
        componentId: String,
        mode: CourseViewMode,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseSectionFragment.newInstance(
                courseId = courseId,
                subSectionId = subSectionId,
                unitId = unitId,
                componentId = componentId,
                mode = mode
            )
        )
    }

    override fun navigateToCourseContainer(
        fm: FragmentManager,
        courseId: String,
        unitId: String,
        componentId: String,
        mode: CourseViewMode,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseUnitContainerFragment.newInstance(
                courseId = courseId,
                unitId = unitId,
                componentId = componentId,
                mode = mode
            )
        )
    }

    override fun replaceCourseContainer(
        fm: FragmentManager,
        courseId: String,
        unitId: String,
        componentId: String,
        mode: CourseViewMode,
    ) {
        replaceFragment(
            fm,
            CourseUnitContainerFragment.newInstance(
                courseId = courseId,
                unitId = unitId,
                componentId = componentId,
                mode = mode
            ),
            FragmentTransaction.TRANSIT_FRAGMENT_FADE
        )
    }

    override fun navigateToFullScreenVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean,
    ) {
        replaceFragmentWithBackStack(
            fm,
            VideoFullScreenFragment.newInstance(videoUrl, videoTime, blockId, courseId, isPlaying)
        )
    }

    override fun navigateToFullScreenYoutubeVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean,
    ) {
        replaceFragmentWithBackStack(
            fm,
            YoutubeVideoFullScreenFragment.newInstance(
                videoUrl,
                videoTime,
                blockId,
                courseId,
                isPlaying
            )
        )
    }

    override fun navigateToHandoutsWebView(
        fm: FragmentManager,
        courseId: String,
        type: HandoutsType,
    ) {
        replaceFragmentWithBackStack(
            fm,
            HandoutsWebViewFragment.newInstance(type.name, courseId)
        )
    }
    // endregion

    // region DiscussionRouter
    override fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        title: String,
        viewType: FragmentViewType,
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

    override fun navigateToDiscussionResponses(
        fm: FragmentManager,
        comment: DiscussionComment,
        isClosed: Boolean,
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionResponsesFragment.newInstance(comment, isClosed)
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

    override fun navigateToAnothersProfile(
        fm: FragmentManager,
        username: String,
    ) {
        replaceFragmentWithBackStack(
            fm,
            AnothersProfileFragment.newInstance(username)
        )
    }
    // endregion

    // region ProfileRouter
    override fun navigateToEditProfile(fm: FragmentManager, account: Account) {
        replaceFragmentWithBackStack(fm, EditProfileFragment.newInstance(account))
    }

    override fun navigateToDeleteAccount(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, DeleteProfileFragment())
    }

    override fun navigateToSettings(fm: FragmentManager) {
        replaceFragmentWithBackStack(
            fm,
            SettingsFragment()
        )
    }

    override fun restartApp(fm: FragmentManager, isLogistrationEnabled: Boolean) {
        fm.apply {
            clearBackStack(this)
            if (isLogistrationEnabled) {
                replaceFragment(fm, LogistrationFragment())
            } else {
                replaceFragment(fm, SignInFragment())
            }
        }
    }

    override fun navigateToVideoSettings(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, VideoSettingsFragment())
    }

    override fun navigateToVideoQuality(fm: FragmentManager, videoQualityType: VideoQualityType) {
        replaceFragmentWithBackStack(fm, VideoQualityFragment.newInstance(videoQualityType.name))
    }

    override fun navigateToDiscover(fm: FragmentManager) {
        fm.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance("", "", HomeTab.DISCOVER.name))
            .commit()
    }

    override fun navigateToWebContent(fm: FragmentManager, title: String, url: String) {
        replaceFragmentWithBackStack(
            fm,
            WebContentFragment.newInstance(title = title, url = url)
        )
    }

    override fun navigateToManageAccount(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, ManageAccountFragment())
    }

    override fun navigateToCalendarSettings(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, CalendarFragment())
    }

    override fun navigateToCoursesToSync(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, CoursesToSyncFragment())
    }
    // endregion

    fun getVisibleFragment(fm: FragmentManager): Fragment? {
        return fm.fragments.firstOrNull { it.isVisible }
    }

    private fun replaceFragmentWithBackStack(fm: FragmentManager, fragment: Fragment) {
        try {
            fm.beginTransaction()
                .replace(R.id.container, fragment, fragment.javaClass.simpleName)
                .addToBackStack(fragment.javaClass.simpleName)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun replaceFragment(
        fm: FragmentManager,
        fragment: Fragment,
        transaction: Int = FragmentTransaction.TRANSIT_NONE,
    ) {
        try {
            fm.beginTransaction()
                .setTransition(transaction)
                .replace(R.id.container, fragment, fragment.javaClass.simpleName)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // App upgrade
    override fun navigateToUserProfile(fm: FragmentManager) {
        try {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.container, ProfileFragment())
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // endregion
}
