package org.openedx.app

import android.app.Application
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import org.openedx.app.di.appModule
import org.openedx.app.di.networkingModule
import org.openedx.app.di.screenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenEdXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OpenEdXApp)
            modules(
                appModule,
                networkingModule,
                screenModule
            )
        }

        if (org.openedx.core.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(org.openedx.core.BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(getString(org.openedx.core.R.string.google_app_id))
                .setApiKey(org.openedx.core.BuildConfig.FIREBASE_API_KEY)
                .setGcmSenderId(org.openedx.core.BuildConfig.FIREBASE_GCM_SENDER_ID)
                .build()
            Firebase.initialize(this, options)
        }
    }

}