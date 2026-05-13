package com.viplove.licadvisornative.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseBootstrap {
    private const val APPLICATION_ID = "1:237541878473:android:631dd1c0a5f5063a36bb18"
    private const val API_KEY = "AIzaSyCF2Zgx0kMEgPaGEeIHHpqwwVObO6-spq0"
    private const val PROJECT_ID = "lic-advisor-native-27f3d"
    private const val STORAGE_BUCKET = "lic-advisor-native-27f3d.firebasestorage.app"
    private const val GCM_SENDER_ID = "237541878473"

    fun initialize(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setApplicationId(APPLICATION_ID)
            .setApiKey(API_KEY)
            .setProjectId(PROJECT_ID)
            .setStorageBucket(STORAGE_BUCKET)
            .setGcmSenderId(GCM_SENDER_ID)
            .build()

        FirebaseApp.initializeApp(context, options)
    }
}
