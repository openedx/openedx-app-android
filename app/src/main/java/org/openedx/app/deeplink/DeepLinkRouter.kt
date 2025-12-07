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
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import kotlin.coroutines.CoroutineContext

class DeepLinkRouter(
    private val config: Config,
    private val appRouter: AppRouter,
    private val corePreferences: CorePreferences,
    private val discoveryInteractor: DiscoveryInteractor,
    private val courseInteractor: CourseInteractor,
    private val discussionInteractor: DiscussionInteractor
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val isUserLoggedIn
        get() = corePreferences.user != null

    fun makeRoute(fm: FragmentManager, deepLink: DeepLink) {
        when (deepLink.type) {
            DeepLinkType.DISCOVERY -> navigateToDiscoveryScreen(fm)
            DeepLinkType.DISCOVERY_COURSE_DETAIL -> navigateToCourseDetail(fm, deepLink)
            DeepLinkType.DISCOVERY_PROGRAM_DETAIL -> navigateToProgramDetail(fm, deepLink)
            else -> handleLoggedOutOrUserNavigation(fm, deepLink)
        }
    }

    private fun handleLoggedOutOrUserNavigation(fm: FragmentManager, deepLink: DeepLink) {
        if (!isUserLoggedIn) {
            navigateToSignIn(fm)
        } else {
            handleProgramAndProfileNavigation(fm, deepLink)
        }
    }

    private fun handleProgramAndProfileNavigation(fm: FragmentManager, deepLink: DeepLink) {
        when (deepLink.type) {
            DeepLinkType.PROGRAM -> navigateToProgram(fm, deepLink)
            DeepLinkType.PROFILE, DeepLinkType.USER_PROFILE -> navigateToProfile(fm)
            else -> handleCourseRelatedNavigation(fm, deepLink)
        }
    }

    private fun handleCourseRelatedNavigation(fm: FragmentManager, deepLink: DeepLink) {
        launch(Dispatchers.Main) {
            val courseId = deepLink.courseId ?: return@launch navigateToDashboard(fm)
            val course = getCourseDetails(courseId) ?: return@launch navigateToDashboard(fm)
            if (!course.isEnrolled) return@launch navigateToDashboard(fm)

            handleSpecificCourseNavigation(fm, deepLink, course.name)
        }
    }

    private fun handleSpecificCourseNavigation(fm: FragmentManager, deepLink: DeepLink, courseTitle: String) {
        navigateToDashboard(fm)
        when (deepLink.type) {
            DeepLinkType.COURSE_DASHBOARD, DeepLinkType.ENROLL, DeepLinkType.ADD_BETA_TESTER -> {
                navigateToCourseDashboard(fm, deepLink, courseTitle)
            }

            DeepLinkType.UNENROLL, DeepLinkType.REMOVE_BETA_TESTER -> {} // Just navigate to dashboard
            DeepLinkType.COURSE_VIDEOS -> navigateToCourseVideos(fm, deepLink)
            DeepLinkType.COURSE_DATES -> navigateToCourseDates(fm, deepLink)
            DeepLinkType.COURSE_DISCUSSION -> navigateToCourseDiscussion(fm, deepLink)
            DeepLinkType.COURSE_HANDOUT -> navigateToCourseHandoutWithMore(fm, deepLink)
            DeepLinkType.COURSE_ANNOUNCEMENT -> navigateToCourseAnnouncementWithMore(fm, deepLink)
            DeepLinkType.COURSE_COMPONENT -> navigateToCourseComponentWithDashboard(fm, deepLink, courseTitle)
            DeepLinkType.DISCUSSION_TOPIC -> navigateToDiscussionTopicWithDiscussion(fm, deepLink)
            DeepLinkType.DISCUSSION_POST -> navigateToDiscussionPostWithDiscussion(fm, deepLink)
            DeepLinkType.DISCUSSION_COMMENT, DeepLinkType.FORUM_RESPONSE -> {
                navigateToDiscussionResponseWithDiscussion(fm, deepLink)
            }

            DeepLinkType.FORUM_COMMENT -> navigateToDiscussionCommentWithDiscussion(fm, deepLink)
            else -> {} // ignore
        }
    }

    // Additional helper methods to encapsulate grouped navigation
    private fun navigateToCourseHandoutWithMore(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseMore(fm, deepLink)
        navigateToCourseHandout(fm, deepLink)
    }

    private fun navigateToCourseAnnouncementWithMore(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseMore(fm, deepLink)
        navigateToCourseAnnouncement(fm, deepLink)
    }

    private fun navigateToCourseComponentWithDashboard(fm: FragmentManager, deepLink: DeepLink, courseTitle: String) {
        navigateToCourseDashboard(fm, deepLink, courseTitle)
        navigateToCourseComponent(fm, deepLink)
    }

    private fun navigateToDiscussionTopicWithDiscussion(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseDiscussion(fm, deepLink)
        navigateToDiscussionTopic(fm, deepLink)
    }

    private fun navigateToDiscussionPostWithDiscussion(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseDiscussion(fm, deepLink)
        navigateToDiscussionPost(fm, deepLink)
    }

    private fun navigateToDiscussionResponseWithDiscussion(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseDiscussion(fm, deepLink)
        navigateToDiscussionResponse(fm, deepLink)
    }

    private fun navigateToDiscussionCommentWithDiscussion(fm: FragmentManager, deepLink: DeepLink) {
        navigateToCourseDiscussion(fm, deepLink)
        navigateToDiscussionComment(fm, deepLink)
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

    private fun navigateToCourseDashboard(
        fm: FragmentManager,
        deepLink: DeepLink,
        courseTitle: String
    ) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = courseTitle,
            )
        }
    }

    private fun navigateToCourseVideos(fm: FragmentManager, deepLink: DeepLink) {
        deepLink.courseId?.let { courseId ->
            appRouter.navigateToCourseOutline(
                fm = fm,
                courseId = courseId,
                courseTitle = "",
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

    private fun navigateToDiscussionResponse(fm: FragmentManager, deepLink: DeepLink) {
        val courseId = deepLink.courseId
        val topicId = deepLink.topicId
        val threadId = deepLink.threadId
        val commentId = deepLink.commentId
        if (courseId == null || topicId == null || threadId == null || commentId == null) {
            return
        }
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
                val response = discussionInteractor.getResponse(commentId)
                launch(Dispatchers.Main) {
                    appRouter.navigateToDiscussionResponses(
                        fm = fm,
                        comment = response,
                        isClosed = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigateToDiscussionComment(fm: FragmentManager, deepLink: DeepLink) {
        val courseId = deepLink.courseId
        val topicId = deepLink.topicId
        val threadId = deepLink.threadId
        val commentId = deepLink.commentId
        val parentId = deepLink.parentId
        if (courseId == null || topicId == null || threadId == null || commentId == null || parentId == null) {
            return
        }
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
                val comment = discussionInteractor.getResponse(parentId)
                launch(Dispatchers.Main) {
                    appRouter.navigateToDiscussionResponses(
                        fm = fm,
                        comment = comment,
                        isClosed = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private suspend fun getCourseDetails(courseId: String): Course? {
        return try {
            discoveryInteractor.getCourseDetails(courseId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
