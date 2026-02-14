package com.photocompressor.kbsize

import android.app.Application
import com.google.android.gms.ads.MobileAds

/**
 * Application class for Photo Compressor app.
 * Initializes AdMob on app start.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
