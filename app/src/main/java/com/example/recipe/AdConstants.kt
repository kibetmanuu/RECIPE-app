package com.example.recipe

object AdConstants {
    // AdMob Ad Unit IDs
    const val BANNER_AD_ID = "ca-app-pub-2431558807153061/9743452630"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-2431558807153061/2926762846"
    const val NATIVE_AD_ID = "ca-app-pub-2431558807153061/2892486714"
    const val APP_OPEN_AD_ID = "ca-app-pub-2431558807153061/8889428167"

    // Test Ad IDs (for testing only)
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // Set this to true for testing, false for production
    const val USE_TEST_ADS = true

    fun getBannerId(): String {
        return if (USE_TEST_ADS) TEST_BANNER_AD_ID else BANNER_AD_ID
    }

    fun getInterstitialId(): String {
        return if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else INTERSTITIAL_AD_ID
    }
}