package com.puzheng.area_investigation

import android.app.Application
import android.content.Context
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Configure Kovenant with standard dispatchers
        // suitable for an Android environment.
        // It's just convenience, you can still use
        // `Kovenant.configure { }` if you want to keep
        // matters in hand.
        startKovenant()
        MyApplication.context = applicationContext
    }

    override fun onTerminate() {
        super.onTerminate()
        // Dispose of the Kovenant thread pools.
        // For quicker shutdown you could use
        // `force=true`, which ignores all current
        // scheduled tasks
        stopKovenant()
    }

    companion object {
        lateinit var context: Context
    }
}