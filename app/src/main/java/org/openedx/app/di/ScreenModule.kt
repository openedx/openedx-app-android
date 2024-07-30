package org.openedx.app.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.app.AppViewModel
import org.openedx.app.MainViewModel
import org.openedx.auth.data.repository.AuthRepository
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.logistration.LogistrationViewModel
import org.openedx.auth.presentation.restore.RestorePasswordViewModel
import org.openedx.auth.presentation.signin.SignInViewModel
import org.openedx.auth.presentation.signup.SignUpViewModel
import org.openedx.core.Validator
import org.openedx.core.presentation.dialog.selectorbottomsheet.SelectDialogViewModel
import org.openedx.core.presentation.settings.VideoQualityViewModel
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.container.CourseContainerViewModel
import org.openedx.course.presentation.dates.CourseDatesViewModel
import org.openedx.course.presentation.handouts.HandoutsViewModel
import org.openedx.course.presentation.outline.CourseOutlineViewModel
import org.openedx.course.presentation.section.CourseSectionViewModel
import org.openedx.course.presentation.unit.container.CourseUnitContainerViewModel
import org.openedx.course.presentation.unit.html.HtmlUnitViewModel
import org.openedx.course.presentation.unit.video.BaseVideoViewModel
import org.openedx.course.presentation.unit.video.EncodedVideoUnitViewModel
import org.openedx.course.presentation.unit.video.VideoUnitViewModel
import org.openedx.course.presentation.unit.video.VideoViewModel
import org.openedx.course.presentation.videos.CourseVideoViewModel
import org.openedx.course.settings.download.DownloadQueueViewModel
import org.openedx.dashboard.data.repository.DashboardRepository
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardViewModel
import org.openedx.discovery.data.repository.DiscoveryRepository
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.NativeDiscoveryViewModel
import org.openedx.discovery.presentation.WebViewDiscoveryViewModel
import org.openedx.discovery.presentation.detail.CourseDetailsViewModel
import org.openedx.discovery.presentation.info.CourseInfoViewModel
import org.openedx.discovery.presentation.program.ProgramViewModel
import org.openedx.discovery.presentation.search.CourseSearchViewModel
import org.openedx.discussion.data.repository.DiscussionRepository
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.comments.DiscussionCommentsViewModel
import org.openedx.discussion.presentation.responses.DiscussionResponsesViewModel
import org.openedx.discussion.presentation.search.DiscussionSearchThreadViewModel
import org.openedx.discussion.presentation.threads.DiscussionAddThreadViewModel
import org.openedx.discussion.presentation.threads.DiscussionThreadsViewModel
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import org.openedx.profile.data.repository.ProfileRepository
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.anothersaccount.AnothersProfileViewModel
import org.openedx.profile.presentation.delete.DeleteProfileViewModel
import org.openedx.profile.presentation.edit.EditProfileViewModel
import org.openedx.profile.presentation.manageaccount.ManageAccountViewModel
import org.openedx.profile.presentation.profile.ProfileViewModel
import org.openedx.profile.presentation.settings.SettingsViewModel
import org.openedx.profile.presentation.video.VideoSettingsViewModel
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewViewModel

val screenModule = module {

    viewModel { AppViewModel(get(), get(), get(), get(), get(named("IODispatcher")), get()) }
    viewModel { MainViewModel(get(), get(), get()) }

    factory { AuthRepository(get(), get(), get()) }
    factory { AuthInteractor(get()) }
    factory { Validator() }

    viewModel { (courseId: String) ->
        LogistrationViewModel(
            courseId,
            get(),
            get(),
            get(),
            get(),
        )
    }

    viewModel { (courseId: String?, infoType: String?) ->
        SignInViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            courseId,
            infoType,
        )
    }

    viewModel { (courseId: String?, infoType: String?) ->
        SignUpViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            courseId,
            infoType
        )
    }
    viewModel { RestorePasswordViewModel(get(), get(), get(), get()) }

    factory { DashboardRepository(get(), get(), get()) }
    factory { DashboardInteractor(get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get()) }

    factory { DiscoveryRepository(get(), get(), get()) }
    factory { DiscoveryInteractor(get()) }
    viewModel { NativeDiscoveryViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (querySearch: String) ->
        WebViewDiscoveryViewModel(
            querySearch,
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    factory { ProfileRepository(get(), get(), get(), get(), get()) }
    factory { ProfileInteractor(get()) }
    viewModel {
        ProfileViewModel(
            interactor = get(),
            resourceManager = get(),
            notifier = get(),
            analytics = get(),
            profileRouter = get(),
        )
    }
    viewModel { (account: Account) -> EditProfileViewModel(get(), get(), get(), get(), account) }
    viewModel { VideoSettingsViewModel(get(), get(), get(), get()) }
    viewModel { (qualityType: String) -> VideoQualityViewModel(qualityType, get(), get(), get()) }
    viewModel { DeleteProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { (username: String) -> AnothersProfileViewModel(get(), get(), username) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ManageAccountViewModel(get(), get(), get(), get(), get()) }

    single { CourseRepository(get(), get(), get(), get()) }
    factory { CourseInteractor(get()) }
    viewModel { (pathId: String, infoType: String) ->
        CourseInfoViewModel(
            pathId,
            infoType,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String) ->
        CourseDetailsViewModel(
            courseId,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { (courseId: String, courseTitle: String, enrollmentMode: String) ->
        CourseContainerViewModel(
            courseId,
            courseTitle,
            enrollmentMode,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { (courseId: String, courseTitle: String) ->
        CourseOutlineViewModel(
            courseId,
            courseTitle,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String) ->
        CourseSectionViewModel(
            courseId,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String, unitId: String) ->
        CourseUnitContainerViewModel(
            courseId,
            unitId,
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String, courseTitle: String) ->
        CourseVideoViewModel(
            courseId,
            courseTitle,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String) -> BaseVideoViewModel(courseId, get()) }
    viewModel { (courseId: String) -> VideoViewModel(courseId, get(), get(), get(), get()) }
    viewModel { (courseId: String) ->
        VideoUnitViewModel(
            courseId,
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { (courseId: String, blockId: String) ->
        EncodedVideoUnitViewModel(
            courseId,
            blockId,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (enrollmentMode: String) ->
        CourseDatesViewModel(
            enrollmentMode,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { (courseId: String, handoutsType: String) ->
        HandoutsViewModel(
            courseId,
            handoutsType,
            get(),
            get(),
            get(),
        )
    }
    viewModel { CourseSearchViewModel(get(), get(), get(), get(), get()) }
    viewModel { SelectDialogViewModel(get()) }

    single { DiscussionRepository(get(), get(), get()) }
    factory { DiscussionInteractor(get()) }
    viewModel {
        DiscussionTopicsViewModel(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { (courseId: String, topicId: String, threadType: String) ->
        DiscussionThreadsViewModel(
            get(),
            get(),
            get(),
            courseId,
            topicId,
            threadType
        )
    }
    viewModel { (thread: org.openedx.discussion.domain.model.Thread) ->
        DiscussionCommentsViewModel(
            get(),
            get(),
            get(),
            thread
        )
    }
    viewModel { (comment: DiscussionComment) ->
        DiscussionResponsesViewModel(
            get(),
            get(),
            get(),
            comment
        )
    }
    viewModel { (courseId: String) -> DiscussionAddThreadViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String) ->
        DiscussionSearchThreadViewModel(
            get(),
            get(),
            get(),
            courseId
        )
    }

    viewModel { (courseId: String?, infoType: String?) ->
        WhatsNewViewModel(
            courseId,
            infoType,
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    viewModel { (descendants: List<String>) ->
        DownloadQueueViewModel(
            descendants,
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModel { HtmlUnitViewModel(get(), get(), get(), get()) }

    viewModel { ProgramViewModel(get(), get(), get(), get(), get(), get(), get()) }

}
