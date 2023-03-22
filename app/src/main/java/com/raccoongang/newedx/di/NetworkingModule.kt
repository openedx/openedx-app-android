package com.raccoongang.newedx.di

import com.raccoongang.auth.data.api.AuthApi
import com.raccoongang.core.data.api.CookiesApi
import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.discussion.data.api.DiscussionApi
import com.raccoongang.newedx.BuildConfig
import com.raccoongang.newedx.data.networking.HandleErrorInterceptor
import com.raccoongang.newedx.data.networking.HeadersInterceptor
import com.raccoongang.newedx.data.networking.OauthRefreshTokenAuthenticator
import com.raccoongang.profile.data.api.ProfileApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
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
            .baseUrl(com.raccoongang.core.BuildConfig.BASE_URL)
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