package org.openedx.app

import android.app.Application
import com.google.firebase.FirebaseOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import io.branch.referral.Branch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.openedx.app.di.appModule
import org.openedx.app.di.networkingModule
import org.openedx.app.di.screenModule
import org.openedx.core.config.Config

class OpenEdXApp : Application() {

    private val config by inject<Config>()

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
        if (config.getFirebaseConfig().enabled) {
            Firebase.initialize(this)
        }

        if (config.getBranchConfig().enabled) {
            if (BuildConfig.DEBUG) {
                Branch.enableTestMode()
                Branch.enableLogging()
            }
            Branch.getAutoInstance(this)
        }
    }
}
