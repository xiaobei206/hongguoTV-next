package com.xiaobei206.hongguotvnext

import android.app.Application
import android.util.Log

/** Application-scoped dependencies will be installed here as the core is migrated. */
class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("HongguoTVNext", "Starting ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME}")
    }
}
