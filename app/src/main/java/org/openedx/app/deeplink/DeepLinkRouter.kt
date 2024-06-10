package org.openedx.app.deeplink

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openedx.app.AppRouter
import org.openedx.app.MainFragment
import org.openedx.app.R
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.core.FragmentViewType
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import kotlin.coroutines.CoroutineContext

class DeepLinkRouter(
    private val config: Config,
    private val appRouter: AppRouter,
    private val corePreferences: CorePreferences,
    private val courseInteractor: CourseInteractor,
    private val discussionInteractor: DiscussionInteractor
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val isUserLoggedIn
        get() = corePreferences.user != null

    fun makeRoute(fm: FragmentManager, deepLink: DeepLink) {
        val screenName = deepLink.screenName ?: return
        when (screenName) {
            // Discovery
            Screen.DISCOVERY.screenName -> {
                navigateToDiscoveryScreen(fm = fm)
                return
            }

            Screen.DISCOVERY_COURSE_DETAIL.screenName -> {
                navigateToCourseDetail(
                    fm = fm,
                    deepLink = deepLink
                )
                return
            }

            Screen.DISCOVERY_PROGRAM_DETAIL.screenName -> {
                navigateToProgramDetail(
                    fm = fm,
                    deepLink = deepLink
                )
                return
            }
        }

        if (!isUserLoggedIn) {
            navigateToSignIn(fm = fm)
            return
        }

        when (screenName) {
            // Course
            Screen.COURSE_DASHBOARD.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDashboard(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_VIDEOS.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseVideos(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_DATES.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDates(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_DISCUSSION.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDiscussion(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_HANDOUT.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseMore(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToCourseHandout(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_ANNOUNCEMENT.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseMore(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToCourseAnnouncement(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.COURSE_COMPONENT.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDashboard(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToCourseComponent(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            // Program
            Screen.PROGRAM.screenName -> {
                navigateToProgram(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            // Discussions
            Screen.DISCUSSION_TOPIC.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDiscussion(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToDiscussionTopic(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.DISCUSSION_POST.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDiscussion(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToDiscussionPost(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            Screen.DISCUSSION_COMMENT.screenName -> {
                navigateToDashboard(fm = fm)
                navigateToCourseDiscussion(
                    fm = fm,
                    deepLink = deepLink
                )
                navigateToDiscussionComment(
                    fm = fm,
                    deepLink = deepLink
                )
            }

            // Profile
            Screen.PROFILE.screenName,
            Screen.USER_PROFILE.screenName -> {
                navigateToProfile(fm = fm)
            }
        }
    }

    // Returns true if there was a successful redirect to the discovery screen
    private fun navigateToDiscoveryScreen(fm: FragmentManager): Boolean {
        return if (isUserLoggedIn) {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(openTab = "DISCOVER"))
                .commitNow()
            true
        } else if (!config.isPreLoginExperienceEnabled()) {
            navigateToSignIn(fm = fm)
            false
        } else if (config.getDiscoveryConfig().isViewTypeWebView()) {
            appRouter.navigateToWebDiscoverCourses(
                fm = fm,
                querySearch = ""
            )
            true
        } else {
            appRouter.navigateToNativeDiscoverCourses(
                fm = fm,
                querySearch = ""
            )
            true
        }
    }

    private fun navigateToCourseDetail(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            if (navigateToDiscoveryScreen(fm = fm)) {
                appRouter.navigateToCourseInfo(
                    fm = fm,
                    courseId = courseId,
                    infoType = WebViewLink.Authority.COURSE_INFO.name
                )
            }
        }
    }

    private fun navigateToProgramDetail(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.pathId?.let { pathId ->
            if (navigateToDiscoveryScreen(fm = fm)) {
                appRouter.navigateToCourseInfo(
                    fm = fm,
                    courseId = pathId,
                    infoType = WebViewLink.Authority.PROGRAM_INFO.name
                )
            }
        }
    }

    private fun navigateToSignIn(fm: FragmentManager) {
        if (appRouter.getVisibleFragment(fm = fm) !is SignInFragment) {
            appRouter.navigateToSignIn(
                fm = fm,
                courseId = null,
                infoType = null
            )
        }
    }

    private fun navigateToCourseDashboard(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = ""
            )
        }
    }

    private fun navigateToCourseVideos(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = "",
                openTab = "VIDEOS"
            )
        }
    }

    private fun navigateToCourseDates(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = "",
                openTab = "DATES"
            )
        }
    }

    private fun navigateToCourseDiscussion(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = "",
                openTab = "DISCUSSIONS"
            )
        }
    }

    private fun navigateToCourseMore(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = "",
                openTab = "MORE"
            )
        }
    }

    private fun navigateToCourseHandout(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToHandoutsWebView(
                fm = fm,
                courseId = courseId,
                type = HandoutsType.Handouts
            )
        }
    }

    private fun navigateToCourseAnnouncement(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToHandoutsWebView(
                fm = fm,
                courseId = courseId,
                type = HandoutsType.Announcements
            )
        }
    }

    private fun navigateToCourseComponent(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            deepLink.componentId?.let { componentId ->
                launch {
                    try {
                        val courseStructure = courseInteractor.getCourseStructure(courseId)
                        courseStructure.blockData
                            .find { it.descendants.contains(componentId) }?.let { block ->
                                appRouter.navigateToCourseContainer(
                                    fm = fm,
                                    courseId = courseId,
                                    unitId = block.id,
                                    componentId = componentId,
                                    mode = CourseViewMode.FULL
                                )
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun navigateToProgram(fm: FragmentManager, deepLink: DeepLink) {
        val pathId = deepLink.pathId
        if (pathId == null) {
            navigateToPrograms(fm = fm)
        } else {
            appRouter.navigateToEnrolledProgramInfo(
                fm = fm,
                pathId = pathId
            )
        }
    }

    private fun navigateToDiscussionTopic(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            deepLink.topicId?.let { topicId ->
                launch {
                    try {
                        discussionInteractor.getCourseTopics(courseId)
                            .find { it.id == topicId }?.let { topic ->
                                launch(Dispatchers.Main) {
                                    appRouter.navigateToDiscussionThread(
                                        fm = fm,
                                        action = DiscussionTopicsViewModel.TOPIC,
                                        courseId = courseId,
                                        topicId = topicId,
                                        title = topic.name,
                                        viewType = FragmentViewType.FULL_CONTENT
                                    )
                                }
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun navigateToDiscussionPost(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            deepLink.topicId?.let { topicId ->
                deepLink.threadId?.let { threadId ->
                    launch {
                        try {
                            discussionInteractor.getCourseTopics(courseId)
                                .find { it.id == topicId }?.let { topic ->
                                    launch(Dispatchers.Main) {
                                        appRouter.navigateToDiscussionThread(
                                            fm = fm,
                                            action = DiscussionTopicsViewModel.TOPIC,
                                            courseId = courseId,
                                            topicId = topicId,
                                            title = topic.name,
                                            viewType = FragmentViewType.FULL_CONTENT
                                        )
                                    }
                                }
                            val thread = discussionInteractor.getThread(
                                threadId,
                                courseId,
                                topicId
                            )
                            launch(Dispatchers.Main) {
                                appRouter.navigateToDiscussionComments(
                                    fm = fm,
                                    thread = thread
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDiscussionComment(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            deepLink.topicId?.let { topicId ->
                deepLink.threadId?.let { threadId ->
                    deepLink.commentId?.let { commentId ->
                        launch {
                            try {
                                discussionInteractor.getCourseTopics(courseId)
                                    .find { it.id == topicId }?.let { topic ->
                                        launch(Dispatchers.Main) {
                                            appRouter.navigateToDiscussionThread(
                                                fm = fm,
                                                action = DiscussionTopicsViewModel.TOPIC,
                                                courseId = courseId,
                                                topicId = topicId,
                                                title = topic.name,
                                                viewType = FragmentViewType.FULL_CONTENT
                                            )
                                        }
                                    }
                                val thread = discussionInteractor.getThread(
                                    threadId,
                                    courseId,
                                    topicId
                                )
                                launch(Dispatchers.Main) {
                                    appRouter.navigateToDiscussionComments(
                                        fm = fm,
                                        thread = thread
                                    )
                                }
                                val commentsData = discussionInteractor.getThreadComment(commentId)
                                commentsData.results.firstOrNull()?.let { comment ->
                                    launch(Dispatchers.Main) {
                                        appRouter.navigateToDiscussionResponses(
                                            fm = fm,
                                            comment = comment,
                                            isClosed = false
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToDashboard(fm: FragmentManager) {
        appRouter.navigateToMain(
            fm = fm,
            courseId = null,
            infoType = null,
            openTab = "LEARN"
        )
    }

    private fun navigateToPrograms(fm: FragmentManager) {
        appRouter.navigateToMain(
            fm = fm,
            courseId = null,
            infoType = null,
            openTab = "PROGRAMS"
        )
    }

    private fun navigateToProfile(fm: FragmentManager) {
        appRouter.navigateToMain(
            fm = fm,
            courseId = null,
            infoType = null,
            openTab = "PROFILE"
        )
    }
}
