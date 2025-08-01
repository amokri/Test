package com.ahm.mydalil

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyDalilApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-wide components here
    }
    
    override fun onTerminate() {
        // Clean up application resources
        super.onTerminate()
    }
}
