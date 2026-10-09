package com.arbonvata.kidstrackeractivity

import android.app.Application
import com.arbonvata.kidstrackeractivity.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KidsTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KidsTrackerApp)
            modules(appModule)
        }
    }
}
