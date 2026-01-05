package ke.nucho.recipe

import android.app.Application
import android.util.Log
import ke.nucho.recipe.config.RemoteConfigManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecipeApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ✨ App Open Ad Manager
    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // ✅ Initialize Crashlytics (catches ALL crashes automatically)
        initializeCrashlytics()

        // Initialize Remote Config
        applicationScope.launch {
            try {
                RemoteConfigManager.initialize()
                FirebaseCrashlytics.getInstance().log("Remote Config initialized successfully")
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e("RecipeApp", "Failed to initialize Remote Config", e)
            }
        }

        // Initialize AdMob
        initializeAdMob()

        // ✨ Initialize App Open Ad Manager
        try {
            appOpenAdManager = AppOpenAdManager(this)
            appOpenAdManager.fetchAd()
            Log.d("RecipeApp", "✅ App Open Ad Manager initialized")
            FirebaseCrashlytics.getInstance().log("App Open Ad Manager initialized")
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e("RecipeApp", "Failed to initialize App Open Ad Manager", e)
        }
    }

    // ✅ Initialize Crashlytics - This catches ALL crashes in your entire app
    private fun initializeCrashlytics() {
        try {
            FirebaseCrashlytics.getInstance().apply {
                // Enable crash collection for entire app
                setCrashlyticsCollectionEnabled(true)

                // Add app version info to crash reports (hardcoded for now)
                setCustomKey("version_code", 3)
                setCustomKey("version_name", "1.2.0")
                setCustomKey("testing_track", "closed_testing")

                // Log app startup
                log("Recipe App started - Version 1.2.0")

                Log.d("RecipeApp", "✅ Crashlytics initialized - Now tracking all crashes")
            }
        } catch (e: Exception) {
            // Even if Crashlytics fails to initialize, app continues normally
            Log.e("RecipeApp", "Failed to initialize Crashlytics", e)
        }
    }

    private fun initializeAdMob() {
        try {
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d("AdMob", "Adapter: $adapterClass, Status: ${status?.initializationState}")
                }
                FirebaseCrashlytics.getInstance().log("AdMob initialized successfully")
            }

            // Set test device configuration if using test ads
            if (AdConstants.USE_TEST_ADS) {
                val testDeviceIds = listOf("YOUR_TEST_DEVICE_ID")
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(configuration)

                FirebaseCrashlytics.getInstance().setCustomKey("ad_test_mode", true)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e("RecipeApp", "Failed to initialize AdMob", e)
        }
    }
}