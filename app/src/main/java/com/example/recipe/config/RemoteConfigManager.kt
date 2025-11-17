package com.example.recipe.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    private const val API_KEYS_PARAM = "spoonacular_api_keys"

    // Default values (fallback if Remote Config fails)
    private val DEFAULT_API_KEYS = listOf(
        "2baaf2e0594848ccb5eb711fa24b16c0"
    )

    private val gson = Gson()

    // Track which key was last used (for analytics)
    private var lastUsedKeyIndex: Int = 0

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 // 1 hour for production
            }
            setConfigSettingsAsync(configSettings)

            // Set default values
            setDefaultsAsync(mapOf(
                API_KEYS_PARAM to gson.toJson(DEFAULT_API_KEYS)
            ))
        }
    }

    /**
     * Initialize Remote Config - Call this when app starts
     */
    suspend fun initialize(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            Log.d(TAG, "✅ Remote Config initialized. Updated: $updated")
            Log.d(TAG, "📊 Available API keys: ${getApiKeys().size}")
            Log.d(TAG, "⚡ Daily capacity: ${getApiKeys().size * 150} requests")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Remote Config: ${e.message}", e)
            false
        }
    }

    /**
     * Force fetch latest config from server
     */
    suspend fun forceRefresh(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            Log.d(TAG, "🔄 Force refresh completed. Updated: $updated")
            if (updated) {
                Log.d(TAG, "✨ New API keys count: ${getApiKeys().size}")
            }
            updated
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to force refresh: ${e.message}", e)
            false
        }
    }

    /**
     * Get list of all API keys from Remote Config
     */
    private fun getApiKeys(): List<String> {
        return try {
            val jsonString = remoteConfig.getString(API_KEYS_PARAM)
            if (jsonString.isEmpty()) {
                Log.w(TAG, "⚠️ API keys empty, using default")
                return DEFAULT_API_KEYS
            }

            val type = object : TypeToken<List<String>>() {}.type
            val keys: List<String> = gson.fromJson(jsonString, type)

            if (keys.isEmpty()) {
                Log.w(TAG, "⚠️ Parsed API keys list is empty, using default")
                DEFAULT_API_KEYS
            } else {
                keys
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse API keys: ${e.message}", e)
            DEFAULT_API_KEYS
        }
    }

    /**
     * Get a random API key from the available keys
     * This distributes load evenly across all API keys
     * Also stores the index for tracking purposes
     */
    fun getRandomApiKey(): String {
        val keys = getApiKeys()
        val randomIndex = Random.nextInt(keys.size)
        val selectedKey = keys[randomIndex]

        // Store the index for tracking
        lastUsedKeyIndex = randomIndex

        Log.d(TAG, "🎲 Selected key $randomIndex: ${selectedKey.take(10)}... (from ${keys.size} keys)")
        return selectedKey
    }

    /**
     * Get the index of the last used API key
     * Used by ApiUsageTracker to track which key was used
     */
    fun getLastUsedKeyIndex(): Int = lastUsedKeyIndex

    /**
     * Get statistics about API key usage (for debugging)
     */
    fun getApiKeyStats(): String {
        val keys = getApiKeys()
        return """
            📊 API Key Statistics
            ━━━━━━━━━━━━━━━━━━━
            Total Keys: ${keys.size}
            Per Key Limit: 150 requests/day
            Total Capacity: ${keys.size * 150} requests/day
        """.trimIndent()
    }

    /**
     * Get total number of API keys configured
     */
    fun getApiKeyCount(): Int = getApiKeys().size
}