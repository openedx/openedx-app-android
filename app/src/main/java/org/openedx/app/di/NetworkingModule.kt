package org.openedx.app.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.app.data.api.NotificationsApi
import org.openedx.app.data.networking.AppUpgradeInterceptor
import org.openedx.app.data.networking.HandleErrorInterceptor
import org.openedx.app.data.networking.HeadersInterceptor
import org.openedx.app.data.networking.InstanceHostInterceptor
import org.openedx.app.data.networking.OauthRefreshTokenAuthenticator
import org.openedx.auth.data.api.AuthApi
import org.openedx.core.BuildConfig
import org.openedx.core.config.Config
import org.openedx.core.data.api.CookiesApi
import org.openedx.core.data.api.CourseApi
import org.openedx.core.instance.InstanceStore
import org.openedx.discovery.data.api.DiscoveryApi
import org.openedx.discussion.data.api.DiscussionApi
import org.openedx.profile.data.api.ProfileApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkingModule = module {

    single { OauthRefreshTokenAuthenticator(get(), get(), get(), get()) }
    single {
        val store = get<InstanceStore>()
        InstanceHostInterceptor(get<Config>(named(APP_LEVEL_CONFIG)).getAppLevelApiHostURL(), store) {
            store.instance(it)
        }
    }

    single {
        OkHttpClient.Builder().apply {
            writeTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            addInterceptor(get<InstanceHostInterceptor>())
            addInterceptor(HeadersInterceptor(get(), get(), get(), get()) { get<InstanceStore>().instance(it) })
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
            addInterceptor(HandleErrorInterceptor(get()))
            addInterceptor(AppUpgradeInterceptor(get()))
            addInterceptor(get<OauthRefreshTokenAuthenticator>())
            authenticator(get<OauthRefreshTokenAuthenticator>())
        }.build()
    }

    single<Retrofit> {
        // Built once, against the app-level host; InstanceHostInterceptor re-targets each
        // request at the selected instance.
        val config = this.get<Config>(named(APP_LEVEL_CONFIG))
        Retrofit.Builder()
            .baseUrl(config.getAppLevelApiHostURL())
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    single { provideApi<AuthApi>(get()) }
    single { provideApi<CookiesApi>(get()) }
    single { provideApi<CourseApi>(get()) }
    single { provideApi<ProfileApi>(get()) }
    single { provideApi<DiscussionApi>(get()) }
    single { provideApi<DiscoveryApi>(get()) }
    single { provideApi<NotificationsApi>(get()) }
}

inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}
