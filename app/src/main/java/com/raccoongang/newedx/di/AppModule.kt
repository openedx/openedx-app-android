package com.raccoongang.newedx.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.raccoongang.auth.presentation.AuthRouter
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.download.FileDownloader
import com.raccoongang.core.system.AppCookieManager
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.course.presentation.CourseRouter
import com.raccoongang.dashboard.presentation.DashboardRouter
import com.raccoongang.discovery.presentation.DiscoveryRouter
import com.raccoongang.discussion.presentation.DiscussionRouter
import com.raccoongang.discussion.system.notifier.DiscussionNotifier
import com.raccoongang.newedx.AppRouter
import com.raccoongang.newedx.room.AppDatabase
import com.raccoongang.newedx.room.DATABASE_NAME
import com.raccoongang.newedx.system.notifier.AppNotifier
import com.raccoongang.profile.presentation.ProfileRouter
import com.raccoongang.profile.system.notifier.ProfileNotifier
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {

    single { PreferencesManager(get()) }

    single { ResourceManager(get()) }

    single { AppCookieManager(get()) }

    single<Gson> { GsonBuilder().create() }

    single { AppNotifier() }
    single { CourseNotifier() }
    single { DiscussionNotifier() }
    single { ProfileNotifier() }

    single { AppRouter() }
    single<AuthRouter> { get<AppRouter>() }
    single<DiscoveryRouter> { get<AppRouter>() }
    single<DashboardRouter> { get<AppRouter>() }
    single<CourseRouter> { get<AppRouter>() }
    single<DiscussionRouter> { get<AppRouter>() }
    single<ProfileRouter> { get<AppRouter>() }


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
        FileDownloader()
    }

    single {
        DownloadWorkerController(get(), get(), get())
    }
}