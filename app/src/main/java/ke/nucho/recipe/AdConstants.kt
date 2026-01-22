package ke.nucho.recipe

object AdConstants {
    // Production AdMob Ad Unit IDs
    const val BANNER_AD_ID = "ca-app-pub-2431550807153061/9743452630"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-2431550807153061/2926762846"
    const val NATIVE_AD_ID = "ca-app-pub-2431550807153061/2892486714"
    const val APP_OPEN_AD_ID = "ca-app-pub-2431550807153061/8889428167"

    // Test Ad Unit IDs (for development/testing)
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"


    const val USE_TEST_ADS = false

    fun getBannerId(): String {
        return if (USE_TEST_ADS) TEST_BANNER_AD_ID else BANNER_AD_ID
    }

    fun getInterstitialId(): String {
        return if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else INTERSTITIAL_AD_ID
    }

    fun getNativeId(): String {
        return if (USE_TEST_ADS) TEST_NATIVE_AD_ID else NATIVE_AD_ID
    }

    fun getAppOpenId(): String {
        return if (USE_TEST_ADS) TEST_APP_OPEN_AD_ID else APP_OPEN_AD_ID
    }
}