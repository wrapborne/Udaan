package com.viplove.licadvisornative

import android.app.Application
import com.viplove.licadvisornative.firebase.FirebaseBootstrap
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.util.NotificationHelper
import com.viplove.licadvisornative.util.RemoteConfigManager
import com.viplove.licadvisornative.worker.DueDateWorker

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseBootstrap.initialize(this)

        // Initialize token manager
        TokenManager.init(this)

        // Initialize config (fetches ULIP NAV URL from API, uses hardcoded defaults otherwise)
        RemoteConfigManager.initializeAndFetch()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule periodic due date check
        DueDateWorker.schedule(this)
    }
}
