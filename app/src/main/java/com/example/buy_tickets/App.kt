package com.example.buy_tickets

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yandex.mapkit.MapKitFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MapKitFactory.setApiKey("9b7f6f55-badb-4fa7-8f81-680640b868c3")
        MapKitFactory.initialize(this)
    }
}