package org.openedx.app.di

import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.app.AnalyticsManager
import org.openedx.app.AppAnalytics
import org.openedx.app.AppRouter
import org.openedx.app.BuildConfig
import org.openedx.app.PluginManager
import org.openedx.app.data.storage.PreferencesManager
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.room.AppDatabase
import org.openedx.app.room.DATABASE_NAME
import org.openedx.app.room.DatabaseManager
import org.openedx.auth.presentation.AgreementProvider
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.sso.BrowserAuthHelper
import org.openedx.auth.presentation.sso.FacebookAuthHelper
import org.openedx.auth.presentation.sso.GoogleAuthHelper
import org.openedx.auth.presentation.sso.MicrosoftAuthHelper
import org.openedx.auth.presentation.sso.OAuthHelper
import org.openedx.core.CalendarRouter
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.model.CourseEnrollments
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.domain.helper.VideoPreviewHelper
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.TranscriptManager
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.module.download.FileDownloader
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.DownloadsAnalytics
import org.openedx.core.presentation.dialog.appreview.AppReviewAnalytics
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.core.presentation.global.appupgrade.AppUpgradeRouter
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.utils.ImageProcessor
import org.openedx.course.worker.OfflineProgressSyncScheduler
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.downloads.presentation.DownloadsRouter
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import org.openedx.profile.data.storage.ProfilePreferences
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.profile.ProfileNotifier
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.data.storage.WhatsNewPreferences
import org.openedx.whatsnew.presentation.WhatsNewAnalytics
import org.openedx.core.DatabaseManager as IDatabaseManager

val appModule = module {

    single { Config(get()) }
    single { PreferencesManager(get()) }
    single<CorePreferences> { get<PreferencesManager>() }
    single<ProfilePreferences> { get<PreferencesManager>() }
    single<WhatsNewPreferences> { get<PreferencesManager>() }
    single<InAppReviewPreferences> { get<PreferencesManager>() }
    single<CoursePreferences> { get<PreferencesManager>() }
    single<CalendarPreferences> { get<PreferencesManager>() }

    single { ResourceManager(get()) }
    single { AppCookieManager(get(), get()) }
    single { ReviewManagerFactory.create(get()) }
    single { CalendarManager(get(), get()) }
    single { DownloadDialogManager(get(), get(), get(), get()) }
    single { DatabaseManager(get(), get(), get(), get()) }
    single<IDatabaseManager> { get<DatabaseManager>() }

    single { ImageProcessor(get()) }

    single<Gson> {
        GsonBuilder()
            .registerTypeAdapter(CourseEnrollments::class.java, CourseEnrollments.Deserializer())
            .create()
    }

    single { AppNotifier() }
    single { CourseNotifier() }
    single { DiscussionNotifier() }
    single { ProfileNotifier() }
    single { DownloadNotifier() }
    single { VideoNotifier() }
    single { DiscoveryNotifier() }
    single { CalendarNotifier() }

    single { AppRouter() }
    single<AuthRouter> { get<AppRouter>() }
    single<DiscoveryRouter> { get<AppRouter>() }
    single<DashboardRouter> { get<AppRouter>() }
    single<CourseRouter> { get<AppRouter>() }
    single<DiscussionRouter> { get<AppRouter>() }
    single<ProfileRouter> { get<AppRouter>() }
    single<WhatsNewRouter> { get<AppRouter>() }
    single<AppUpgradeRouter> { get<AppRouter>() }
    single { DeepLinkRouter(get(), get(), get(), get(), get(), get()) }
    single<CalendarRouter> { get<AppRouter>() }
    single<DownloadsRouter> { get<AppRouter>() }

    single { NetworkConnection(get()) }

    single(named("IODispatcher")) {
        Dispatchers.IO
    }

    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    single<RoomDatabase> {
        get<AppDatabase>()
    }

    single {
        val room = get<AppDatabase>()
        room.discoveryDao()
    }

    single {
        val room = get<AppDatabase>()
        room.courseDao()
    }

    single {
        val room = get<AppDatabase>()
        room.dashboardDao()
    }

    single {
        val room = get<AppDatabase>()
        room.downloadDao()
    }

    single {
        val room = get<AppDatabase>()
        room.calendarDao()
    }

    single {
        FileDownloader()
    }

    single {
        DownloadWorkerController(get(), get(), get())
    }

    single {
        val resourceManager = get<ResourceManager>()
        AppData(
            appName = resourceManager.getString(R.string.app_name),
            versionName = BuildConfig.VERSION_NAME,
            applicationId = BuildConfig.APPLICATION_ID,
        )
    }
    factory { (activity: AppCompatActivity) -> AppReviewManager(activity, get(), get()) }

    single { TranscriptManager(get(), get()) }
    single { WhatsNewManager(get(), get(), get(), get()) }
    single<WhatsNewGlobalManager> { get<WhatsNewManager>() }

    single<AppAnalytics> { get<AnalyticsManager>() }
    single<AuthAnalytics> { get<AnalyticsManager>() }
    single<AppReviewAnalytics> { get<AnalyticsManager>() }
    single<CoreAnalytics> { get<AnalyticsManager>() }
    single<CourseAnalytics> { get<AnalyticsManager>() }
    single<DashboardAnalytics> { get<AnalyticsManager>() }
    single<DiscoveryAnalytics> { get<AnalyticsManager>() }
    single<DiscussionAnalytics> { get<AnalyticsManager>() }
    single<ProfileAnalytics> { get<AnalyticsManager>() }
    single<WhatsNewAnalytics> { get<AnalyticsManager>() }
    single<DownloadsAnalytics> { get<AnalyticsManager>() }

    factory { AgreementProvider(get(), get()) }
    factory { FacebookAuthHelper() }
    factory { GoogleAuthHelper(get()) }
    factory { MicrosoftAuthHelper() }
    factory { BrowserAuthHelper(get()) }
    factory { OAuthHelper(get(), get(), get()) }
    factory { VideoPreviewHelper(get(), get()) }

    factory { FileUtil(get(), get<ResourceManager>().getString(R.string.app_name)) }
    single { DownloadHelper(get(), get()) }

    factory { OfflineProgressSyncScheduler(get()) }

    single { CalendarSyncScheduler(get()) }

    single { AnalyticsManager() }
    single {
        PluginManager(
            analyticsManager = get()
        )
    }
}
