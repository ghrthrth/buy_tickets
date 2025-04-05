package com.example.buy_tickets

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.search.SearchFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MapKitFactory.setApiKey("00e2eb94-f74d-4aa4-9efd-62ea524b38fb") // Ensure this is correct
            MapKitFactory.initialize(this)

        } catch (e: Exception) {
            Log.e("YandexInit", "Initialization error", e)
        }
    }



}