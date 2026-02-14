package com.photocompressor.kbsize

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Composable function to display a banner ad.
 * Only shown when user is not Pro.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Replace with your real Ad Unit ID from AdMob console
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * Manager for interstitial ads.
 * Preloads ads and shows them after compression (for free users only).
 */
class InterstitialAdManager(private val context: Context) {

    companion object {
        private const val TAG = "InterstitialAdManager"
        // Replace with your real Ad Unit ID from AdMob console
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    init {
        loadAd()
    }

    /**
     * Load an interstitial ad.
     */
    fun loadAd() {
        if (isLoading || interstitialAd != null) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Show the interstitial ad if loaded.
     * Automatically reloads a new ad after showing.
     */
    fun showAd(activity: Activity) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    loadAd() // Reload for next time
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "Failed to show interstitial ad: ${error.message}")
                    interstitialAd = null
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad shown")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not ready")
            // Try loading again
            loadAd()
        }
    }
}
