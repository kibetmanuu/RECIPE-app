package com.example.recipe

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val application: Application) :
    DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var isShowingAd = false

    companion object {
        private const val TAG = "AppOpenAdManager"
        // Replace with your actual App Open Ad Unit ID
        private  val AD_UNIT_ID = AdConstants.getAppOpenId() // Test ID
        private const val TIMEOUT_HOURS = 4L
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Load an app open ad
     */
    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoadingAd = false
                    Log.d(TAG, "✅ App Open Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.e(TAG, "❌ App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Check if ad is available and not expired
     */
    private fun isAdAvailable(): Boolean {
        val wasLoadTimeLessThanHoursAgo = (Date().time - loadTime) < (TIMEOUT_HOURS * 3600000)
        return appOpenAd != null && wasLoadTimeLessThanHoursAgo
    }

    /**
     * Show the app open ad if available
     */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        // Don't show ad if already showing
        if (isShowingAd) {
            Log.d(TAG, "⚠️ Ad is already showing")
            return
        }

        // Check if ad is available
        if (!isAdAvailable()) {
            Log.d(TAG, "⚠️ Ad not available, loading new ad")
            loadAd()
            onAdDismissed()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ App Open Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                loadAd() // Load next ad
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "❌ App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd() // Load next ad
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ App Open Ad showed")
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    /**
     * Lifecycle methods - DefaultLifecycleObserver
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { activity ->
            // Show ad when app comes to foreground
            showAdIfAvailable(activity)
        }
        Log.d(TAG, "📱 App in foreground")
    }

    /**
     * Activity lifecycle callbacks
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Manually trigger ad load (call this from Application onCreate)
     */
    fun fetchAd() {
        loadAd()
    }
}