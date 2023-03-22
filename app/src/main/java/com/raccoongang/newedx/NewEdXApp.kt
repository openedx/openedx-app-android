package com.raccoongang.newedx

import android.app.Application
import com.raccoongang.newedx.di.appModule
import com.raccoongang.newedx.di.networkingModule
import com.raccoongang.newedx.di.screenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NewEdXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NewEdXApp)
            modules(
                appModule,
                networkingModule,
                screenModule
            )
        }
    }

}