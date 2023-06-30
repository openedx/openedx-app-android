package com.raccoongang.newedx.di

import com.raccoongang.auth.data.repository.AuthRepository
import com.raccoongang.auth.domain.interactor.AuthInteractor
import com.raccoongang.auth.presentation.restore.RestorePasswordViewModel
import com.raccoongang.auth.presentation.signin.SignInViewModel
import com.raccoongang.auth.presentation.signup.SignUpViewModel
import com.raccoongang.core.Validator
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.presentation.dialog.SelectDialogViewModel
import com.raccoongang.course.data.repository.CourseRepository
import com.raccoongang.course.domain.interactor.CourseInteractor
import com.raccoongang.course.presentation.container.CourseContainerViewModel
import com.raccoongang.course.presentation.detail.CourseDetailsViewModel
import com.raccoongang.course.presentation.handouts.HandoutsViewModel
import com.raccoongang.course.presentation.outline.CourseOutlineViewModel
import com.raccoongang.discovery.presentation.search.CourseSearchViewModel
import com.raccoongang.course.presentation.section.CourseSectionViewModel
import com.raccoongang.course.presentation.unit.container.CourseUnitContainerViewModel
import com.raccoongang.course.presentation.unit.video.VideoUnitViewModel
import com.raccoongang.course.presentation.unit.video.VideoViewModel
import com.raccoongang.course.presentation.videos.CourseVideoViewModel
import com.raccoongang.dashboard.data.repository.DashboardRepository
import com.raccoongang.dashboard.domain.interactor.DashboardInteractor
import com.raccoongang.dashboard.presentation.DashboardViewModel
import com.raccoongang.discovery.data.repository.DiscoveryRepository
import com.raccoongang.discovery.domain.interactor.DiscoveryInteractor
import com.raccoongang.discovery.presentation.DiscoveryViewModel
import com.raccoongang.discussion.data.repository.DiscussionRepository
import com.raccoongang.discussion.domain.interactor.DiscussionInteractor
import com.raccoongang.discussion.domain.model.DiscussionComment
import com.raccoongang.discussion.presentation.comments.DiscussionCommentsViewModel
import com.raccoongang.discussion.presentation.responses.DiscussionResponsesViewModel
import com.raccoongang.discussion.presentation.search.DiscussionSearchThreadViewModel
import com.raccoongang.discussion.presentation.threads.DiscussionAddThreadViewModel
import com.raccoongang.discussion.presentation.threads.DiscussionThreadsViewModel
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsViewModel
import com.raccoongang.newedx.AppViewModel
import com.raccoongang.profile.data.repository.ProfileRepository
import com.raccoongang.profile.domain.interactor.ProfileInteractor
import com.raccoongang.profile.presentation.delete.DeleteProfileViewModel
import com.raccoongang.profile.presentation.edit.EditProfileViewModel
import com.raccoongang.profile.presentation.profile.ProfileViewModel
import com.raccoongang.profile.presentation.settings.video.VideoQualityViewModel
import com.raccoongang.profile.presentation.settings.video.VideoSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val screenModule = module {

    viewModel { AppViewModel(get(), get(), get(), get(named("IODispatcher")), get()) }

    factory { AuthRepository(get(), get()) }
    factory { AuthInteractor(get()) }
    factory { Validator() }
    viewModel { SignInViewModel(get(), get(), get(), get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get(), get()) }
    viewModel { RestorePasswordViewModel(get(), get(), get()) }

    factory { DashboardRepository(get(), get(),get()) }
    factory { DashboardInteractor(get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get()) }

    factory { DiscoveryRepository(get(), get()) }
    factory { DiscoveryInteractor(get()) }
    viewModel { DiscoveryViewModel(get(), get(), get(), get()) }

    factory { ProfileRepository(get(), get(), get()) }
    factory { ProfileInteractor(get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get(named("IODispatcher")), get(), get(), get()) }
    viewModel { (account: Account) -> EditProfileViewModel(get(), get(), get(), get(), account) }
    viewModel { VideoSettingsViewModel(get(), get()) }
    viewModel { VideoQualityViewModel(get(), get()) }
    viewModel { DeleteProfileViewModel(get(), get(), get(), get()) }

    single { CourseRepository(get(), get(), get(),get()) }
    factory { CourseInteractor(get()) }
    viewModel { (courseId: String) -> CourseDetailsViewModel(courseId, get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseContainerViewModel(courseId, get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseOutlineViewModel(courseId, get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseSectionViewModel(get(), get(), get(), get(), get(), get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> CourseUnitContainerViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> CourseVideoViewModel(courseId, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> VideoViewModel(courseId, get(), get(), get()) }
    viewModel { (courseId: String) -> VideoUnitViewModel(courseId, get(), get(), get(), get()) }
    viewModel { (courseId:String, handoutsType: String) -> HandoutsViewModel(courseId, handoutsType, get()) }
    viewModel { CourseSearchViewModel(get(), get(), get()) }
    viewModel { SelectDialogViewModel(get()) }

    single { DiscussionRepository(get(), get()) }
    factory { DiscussionInteractor(get()) }
    viewModel { (courseId: String) -> DiscussionTopicsViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String, topicId: String, threadType: String) ->  DiscussionThreadsViewModel(get(), get(), get(), courseId, topicId, threadType) }
    viewModel { (thread: com.raccoongang.discussion.domain.model.Thread) -> DiscussionCommentsViewModel(get(), get(), get(), get(), thread) }
    viewModel { (comment: DiscussionComment) -> DiscussionResponsesViewModel(get(), get(), get(), get(), comment) }
    viewModel { (courseId: String) -> DiscussionAddThreadViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> DiscussionSearchThreadViewModel(get(), get(), get(), courseId) }
}