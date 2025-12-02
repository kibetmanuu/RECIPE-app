package ke.nucho.recipe

import android.app.Application
import android.util.Log
import ke.nucho.recipe.config.RemoteConfigManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
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

        // Initialize Remote Config
        applicationScope.launch {
            RemoteConfigManager.initialize()
        }

        // Initialize AdMob
        initializeAdMob()

        // ✨ Initialize App Open Ad Manager
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.fetchAd()
        Log.d("RecipeApp", "✅ App Open Ad Manager initialized")
    }

    private fun initializeAdMob() {
        MobileAds.initialize(this) { initializationStatus ->
            // Initialization complete
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d("AdMob", "Adapter: $adapterClass, Status: ${status?.initializationState}")
            }
        }

        // Set test device configuration if using test ads
        if (AdConstants.USE_TEST_ADS) {
            val testDeviceIds = listOf("YOUR_TEST_DEVICE_ID") // You can add your device ID here later
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }
    }
}