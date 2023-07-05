package org.openedx.app.di

import org.openedx.auth.data.api.AuthApi
import org.openedx.core.data.api.CookiesApi
import org.openedx.core.data.api.CourseApi
import org.openedx.discussion.data.api.DiscussionApi
import org.openedx.app.data.networking.HandleErrorInterceptor
import org.openedx.app.data.networking.HeadersInterceptor
import org.openedx.app.data.networking.OauthRefreshTokenAuthenticator
import org.openedx.profile.data.api.ProfileApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import org.openedx.core.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkingModule = module {

    single { OauthRefreshTokenAuthenticator(get(), get()) }

    single {
        OkHttpClient.Builder().apply {
            writeTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            addInterceptor(HeadersInterceptor(get()))
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
            addInterceptor(HandleErrorInterceptor(get()))
            authenticator(get<OauthRefreshTokenAuthenticator>())
        }.build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(org.openedx.core.BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    single { provideApi<AuthApi>(get()) }
    single { provideApi<CookiesApi>(get()) }
    single { provideApi<CourseApi>(get()) }
    single { provideApi<ProfileApi>(get()) }
    single { provideApi<DiscussionApi>(get()) }
}


inline fun <reified T> provideApi(retrofit: Retrofit): T {
    return retrofit.create(T::class.java)
}