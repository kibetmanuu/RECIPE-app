package com.example.recipe.analytics

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object ApiUsageTracker {
    private const val TAG = "ApiUsageTracker"
    private val firestore = FirebaseFirestore.getInstance()

    // Collections
    private const val COLLECTION_API_USAGE = "api_usage"
    private const val COLLECTION_ACTIVE_USERS = "active_users"
    private const val COLLECTION_LIFETIME_STATS = "lifetime_stats"
    private const val LIFETIME_STATS_DOC = "global"

    // Daily limits
    private const val CALLS_PER_KEY = 150
    private const val TOTAL_KEYS = 5
    private const val DAILY_LIMIT = CALLS_PER_KEY * TOTAL_KEYS // 750

    /**
     * Track an API call to Spoonacular
     * Updates BOTH daily (resets) and lifetime (permanent) stats
     * @param apiKeyIndex Which key was used (0-4 for your 5 keys)
     * @param endpoint Which API endpoint was called
     * @param success Whether the call was successful
     */
    suspend fun trackApiCall(
        apiKeyIndex: Int,
        endpoint: String,
        success: Boolean
    ) {
        try {
            val today = getTodayDateString()

            // 1. Update DAILY stats (resets daily)
            val dailyDoc = firestore
                .collection(COLLECTION_API_USAGE)
                .document(today)

            val dailyUpdates = hashMapOf<String, Any>(
                "date" to today,
                "total_calls" to FieldValue.increment(1),
                "spoonacular_calls" to FieldValue.increment(1),
                "key_${apiKeyIndex}_calls" to FieldValue.increment(1),
                "successful_calls" to FieldValue.increment(if (success) 1 else 0),
                "failed_calls" to FieldValue.increment(if (!success) 1 else 0),
                "last_updated" to FieldValue.serverTimestamp(),
                "endpoints.$endpoint" to FieldValue.increment(1)
            )

            dailyDoc.set(dailyUpdates, com.google.firebase.firestore.SetOptions.merge()).await()

            // 2. Calculate and store all statistics in Firebase
            updateCalculatedStats(today)

            // 3. Update LIFETIME stats (never resets)
            val lifetimeDoc = firestore
                .collection(COLLECTION_LIFETIME_STATS)
                .document(LIFETIME_STATS_DOC)

            val lifetimeUpdates = hashMapOf<String, Any>(
                "total_api_calls_ever" to FieldValue.increment(1),
                "total_successful_calls_ever" to FieldValue.increment(if (success) 1 else 0),
                "total_failed_calls_ever" to FieldValue.increment(if (!success) 1 else 0),
                "lifetime_endpoints.$endpoint" to FieldValue.increment(1),
                "lifetime_key_${apiKeyIndex}_calls" to FieldValue.increment(1),
                "last_api_call" to FieldValue.serverTimestamp()
            )

            lifetimeDoc.set(lifetimeUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            updateLifetimeCalculatedStats()

            Log.d(TAG, "✅ API Call Tracked: key=$apiKeyIndex, endpoint=$endpoint, success=$success")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to track API call: ${e.message}", e)
        }
    }

    /**
     * Track a CACHE HIT (loaded from Firestore cache instead of API)
     * Updates BOTH daily and lifetime stats
     * @param cacheKey The cache key used
     * @param itemCount Number of items loaded from cache
     */
    suspend fun trackCacheHit(
        cacheKey: String,
        itemCount: Int
    ) {
        try {
            val today = getTodayDateString()

            // 1. Update DAILY cache stats
            val dailyDoc = firestore
                .collection(COLLECTION_API_USAGE)
                .document(today)

            val dailyUpdates = hashMapOf<String, Any>(
                "date" to today,
                "cache_hits" to FieldValue.increment(1),
                "cache_items_loaded" to FieldValue.increment(itemCount.toLong()),
                "last_updated" to FieldValue.serverTimestamp(),
                "cache_keys.$cacheKey" to FieldValue.increment(1)
            )

            dailyDoc.set(dailyUpdates, com.google.firebase.firestore.SetOptions.merge()).await()

            // Update calculated stats
            updateCalculatedStats(today)

            // 2. Update LIFETIME cache stats (never resets)
            val lifetimeDoc = firestore
                .collection(COLLECTION_LIFETIME_STATS)
                .document(LIFETIME_STATS_DOC)

            val lifetimeUpdates = hashMapOf<String, Any>(
                "total_cache_hits_ever" to FieldValue.increment(1),
                "total_cache_items_loaded_ever" to FieldValue.increment(itemCount.toLong()),
                "lifetime_cache_keys.$cacheKey" to FieldValue.increment(1),
                "last_cache_hit" to FieldValue.serverTimestamp()
            )

            lifetimeDoc.set(lifetimeUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            updateLifetimeCalculatedStats()

            Log.d(TAG, "💾 Cache Hit Tracked: key=$cacheKey, items=$itemCount")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to track cache hit: ${e.message}", e)
        }
    }

    /**
     * Track a CACHE MISS (had to fetch from API)
     * Updates BOTH daily and lifetime stats
     * @param cacheKey The cache key that missed
     * @param reason Why it missed (expired, not_found, error)
     */
    suspend fun trackCacheMiss(
        cacheKey: String,
        reason: String = "not_found"
    ) {
        try {
            val today = getTodayDateString()

            // 1. Update DAILY cache miss
            val dailyDoc = firestore
                .collection(COLLECTION_API_USAGE)
                .document(today)

            val dailyUpdates = hashMapOf<String, Any>(
                "date" to today,
                "cache_misses" to FieldValue.increment(1),
                "last_updated" to FieldValue.serverTimestamp(),
                "cache_miss_reasons.$reason" to FieldValue.increment(1)
            )

            dailyDoc.set(dailyUpdates, com.google.firebase.firestore.SetOptions.merge()).await()

            // Update calculated stats
            updateCalculatedStats(today)

            // 2. Update LIFETIME cache miss (never resets)
            val lifetimeDoc = firestore
                .collection(COLLECTION_LIFETIME_STATS)
                .document(LIFETIME_STATS_DOC)

            val lifetimeUpdates = hashMapOf<String, Any>(
                "total_cache_misses_ever" to FieldValue.increment(1),
                "lifetime_cache_miss_reasons.$reason" to FieldValue.increment(1),
                "last_cache_miss" to FieldValue.serverTimestamp()
            )

            lifetimeDoc.set(lifetimeUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            updateLifetimeCalculatedStats()

            Log.d(TAG, "❌ Cache Miss Tracked: key=$cacheKey, reason=$reason")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to track cache miss: ${e.message}", e)
        }
    }

    /**
     * Update calculated statistics in Firebase (DAILY)
     * This stores all percentages, remaining calls, etc. directly in Firestore
     */
    private suspend fun updateCalculatedStats(date: String) {
        try {
            val doc = firestore
                .collection(COLLECTION_API_USAGE)
                .document(date)
                .get()
                .await()

            if (doc.exists()) {
                // Get current values
                val spoonacularCalls = doc.getLong("spoonacular_calls")?.toInt() ?: 0
                val cacheHits = doc.getLong("cache_hits")?.toInt() ?: 0
                val cacheMisses = doc.getLong("cache_misses")?.toInt() ?: 0
                val key0Calls = doc.getLong("key_0_calls")?.toInt() ?: 0
                val key1Calls = doc.getLong("key_1_calls")?.toInt() ?: 0
                val key2Calls = doc.getLong("key_2_calls")?.toInt() ?: 0
                val key3Calls = doc.getLong("key_3_calls")?.toInt() ?: 0
                val key4Calls = doc.getLong("key_4_calls")?.toInt() ?: 0

                // Calculate statistics
                val remaining = DAILY_LIMIT - spoonacularCalls
                val percentage = (spoonacularCalls.toFloat() / DAILY_LIMIT) * 100f

                val cacheTotal = cacheHits + cacheMisses
                val cacheHitRate = if (cacheTotal > 0) {
                    (cacheHits.toFloat() / cacheTotal) * 100f
                } else 0f

                // Calculate per-key stats
                val key0Remaining = CALLS_PER_KEY - key0Calls
                val key0Percentage = (key0Calls.toFloat() / CALLS_PER_KEY) * 100f

                val key1Remaining = CALLS_PER_KEY - key1Calls
                val key1Percentage = (key1Calls.toFloat() / CALLS_PER_KEY) * 100f

                val key2Remaining = CALLS_PER_KEY - key2Calls
                val key2Percentage = (key2Calls.toFloat() / CALLS_PER_KEY) * 100f

                val key3Remaining = CALLS_PER_KEY - key3Calls
                val key3Percentage = (key3Calls.toFloat() / CALLS_PER_KEY) * 100f

                val key4Remaining = CALLS_PER_KEY - key4Calls
                val key4Percentage = (key4Calls.toFloat() / CALLS_PER_KEY) * 100f

                // Store all calculated values in Firebase
                val calculatedStats = hashMapOf<String, Any>(
                    // Overall stats
                    "remaining_calls" to remaining,
                    "usage_percentage" to String.format("%.2f", percentage),

                    // Cache stats
                    "cache_hit_rate" to String.format("%.2f", cacheHitRate),
                    "cache_total_requests" to cacheTotal,

                    // Key 0 stats
                    "key_0_remaining" to key0Remaining,
                    "key_0_percentage" to String.format("%.2f", key0Percentage),

                    // Key 1 stats
                    "key_1_remaining" to key1Remaining,
                    "key_1_percentage" to String.format("%.2f", key1Percentage),

                    // Key 2 stats
                    "key_2_remaining" to key2Remaining,
                    "key_2_percentage" to String.format("%.2f", key2Percentage),

                    // Key 3 stats
                    "key_3_remaining" to key3Remaining,
                    "key_3_percentage" to String.format("%.2f", key3Percentage),

                    // Key 4 stats
                    "key_4_remaining" to key4Remaining,
                    "key_4_percentage" to String.format("%.2f", key4Percentage),

                    // Summary
                    "daily_limit" to DAILY_LIMIT,
                    "key_limit" to CALLS_PER_KEY,
                    "total_keys" to TOTAL_KEYS
                )

                firestore
                    .collection(COLLECTION_API_USAGE)
                    .document(date)
                    .update(calculatedStats)
                    .await()

                Log.d(TAG, "✅ Calculated stats updated in Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update calculated stats: ${e.message}", e)
        }
    }

    /**
     * Update calculated statistics for LIFETIME stats
     */
    private suspend fun updateLifetimeCalculatedStats() {
        try {
            val doc = firestore
                .collection(COLLECTION_LIFETIME_STATS)
                .document(LIFETIME_STATS_DOC)
                .get()
                .await()

            if (doc.exists()) {
                val totalApiCalls = doc.getLong("total_api_calls_ever")?.toInt() ?: 0
                val totalCacheHits = doc.getLong("total_cache_hits_ever")?.toInt() ?: 0
                val totalCacheMisses = doc.getLong("total_cache_misses_ever")?.toInt() ?: 0
                val totalSuccessful = doc.getLong("total_successful_calls_ever")?.toInt() ?: 0

                val lifetimeCacheTotal = totalCacheHits + totalCacheMisses
                val lifetimeCacheHitRate = if (lifetimeCacheTotal > 0) {
                    (totalCacheHits.toFloat() / lifetimeCacheTotal) * 100f
                } else 0f

                val lifetimeSuccessRate = if (totalApiCalls > 0) {
                    (totalSuccessful.toFloat() / totalApiCalls) * 100f
                } else 0f

                val totalRequests = totalApiCalls + totalCacheHits
                val cacheSavings = if (totalRequests > 0) {
                    (totalCacheHits.toFloat() / totalRequests) * 100f
                } else 0f

                val lifetimeStats = hashMapOf<String, Any>(
                    "lifetime_cache_hit_rate" to String.format("%.2f", lifetimeCacheHitRate),
                    "lifetime_success_rate" to String.format("%.2f", lifetimeSuccessRate),
                    "lifetime_cache_savings" to String.format("%.2f", cacheSavings),
                    "total_requests_ever" to totalRequests
                )

                firestore
                    .collection(COLLECTION_LIFETIME_STATS)
                    .document(LIFETIME_STATS_DOC)
                    .update(lifetimeStats)
                    .await()

                Log.d(TAG, "✅ Lifetime calculated stats updated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update lifetime calculated stats: ${e.message}", e)
        }
    }

    /**
     * Track active user
     * @param userId Unique user ID (use Firebase Auth or generate UUID)
     */
    suspend fun trackActiveUser(userId: String) {
        try {
            val today = getTodayDateString()
            val userDoc = firestore
                .collection(COLLECTION_ACTIVE_USERS)
                .document(today)

            val updates = hashMapOf<String, Any>(
                "users.$userId" to FieldValue.serverTimestamp(),
                "last_updated" to FieldValue.serverTimestamp()
            )

            userDoc.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()

            val doc = userDoc.get().await()
            val users = doc.data?.get("users") as? Map<*, *>
            val userCount = users?.size ?: 0

            userDoc.update("user_count", userCount).await()

            Log.d(TAG, "✅ Tracked active user: $userId (total today: $userCount)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to track active user: ${e.message}", e)
        }
    }

    /**
     * Get today's API usage statistics (DAILY - resets at midnight)
     */
    suspend fun getTodayUsage(): ApiUsageStats? {
        return try {
            val today = getTodayDateString()
            val doc = firestore
                .collection(COLLECTION_API_USAGE)
                .document(today)
                .get()
                .await()

            if (doc.exists()) {
                ApiUsageStats(
                    date = today,
                    spoonacularCalls = doc.getLong("spoonacular_calls")?.toInt() ?: 0,
                    cacheHits = doc.getLong("cache_hits")?.toInt() ?: 0,
                    cacheMisses = doc.getLong("cache_misses")?.toInt() ?: 0,
                    cacheItemsLoaded = doc.getLong("cache_items_loaded")?.toInt() ?: 0,
                    key0Calls = doc.getLong("key_0_calls")?.toInt() ?: 0,
                    key1Calls = doc.getLong("key_1_calls")?.toInt() ?: 0,
                    key2Calls = doc.getLong("key_2_calls")?.toInt() ?: 0,
                    key3Calls = doc.getLong("key_3_calls")?.toInt() ?: 0,
                    key4Calls = doc.getLong("key_4_calls")?.toInt() ?: 0,
                    successfulCalls = doc.getLong("successful_calls")?.toInt() ?: 0,
                    failedCalls = doc.getLong("failed_calls")?.toInt() ?: 0
                )
            } else {
                // Return empty stats for today (fresh start)
                ApiUsageStats(
                    date = today,
                    spoonacularCalls = 0,
                    cacheHits = 0,
                    cacheMisses = 0,
                    cacheItemsLoaded = 0,
                    key0Calls = 0,
                    key1Calls = 0,
                    key2Calls = 0,
                    key3Calls = 0,
                    key4Calls = 0,
                    successfulCalls = 0,
                    failedCalls = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get today's usage: ${e.message}", e)
            null
        }
    }

    /**
     * Get LIFETIME statistics (NEVER resets)
     */
    suspend fun getLifetimeStats(): LifetimeStats? {
        return try {
            val doc = firestore
                .collection(COLLECTION_LIFETIME_STATS)
                .document(LIFETIME_STATS_DOC)
                .get()
                .await()

            if (doc.exists()) {
                LifetimeStats(
                    totalApiCallsEver = doc.getLong("total_api_calls_ever")?.toInt() ?: 0,
                    totalCacheHitsEver = doc.getLong("total_cache_hits_ever")?.toInt() ?: 0,
                    totalCacheMissesEver = doc.getLong("total_cache_misses_ever")?.toInt() ?: 0,
                    totalCacheItemsLoadedEver = doc.getLong("total_cache_items_loaded_ever")?.toInt() ?: 0,
                    totalSuccessfulCallsEver = doc.getLong("total_successful_calls_ever")?.toInt() ?: 0,
                    totalFailedCallsEver = doc.getLong("total_failed_calls_ever")?.toInt() ?: 0,
                    lifetimeKey0Calls = doc.getLong("lifetime_key_0_calls")?.toInt() ?: 0,
                    lifetimeKey1Calls = doc.getLong("lifetime_key_1_calls")?.toInt() ?: 0,
                    lifetimeKey2Calls = doc.getLong("lifetime_key_2_calls")?.toInt() ?: 0,
                    lifetimeKey3Calls = doc.getLong("lifetime_key_3_calls")?.toInt() ?: 0,
                    lifetimeKey4Calls = doc.getLong("lifetime_key_4_calls")?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get lifetime stats: ${e.message}", e)
            null
        }
    }

    /**
     * Get today's active user count
     */
    suspend fun getTodayActiveUsers(): Int {
        return try {
            val today = getTodayDateString()
            val doc = firestore
                .collection(COLLECTION_ACTIVE_USERS)
                .document(today)
                .get()
                .await()

            doc.getLong("user_count")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get active users: ${e.message}", e)
            0
        }
    }

    /**
     * Get usage for the last N days
     */
    suspend fun getUsageHistory(days: Int = 7): List<ApiUsageStats> {
        return try {
            val stats = mutableListOf<ApiUsageStats>()
            val calendar = Calendar.getInstance()

            repeat(days) {
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                val doc = firestore
                    .collection(COLLECTION_API_USAGE)
                    .document(dateString)
                    .get()
                    .await()

                if (doc.exists()) {
                    stats.add(
                        ApiUsageStats(
                            date = dateString,
                            spoonacularCalls = doc.getLong("spoonacular_calls")?.toInt() ?: 0,
                            cacheHits = doc.getLong("cache_hits")?.toInt() ?: 0,
                            cacheMisses = doc.getLong("cache_misses")?.toInt() ?: 0,
                            cacheItemsLoaded = doc.getLong("cache_items_loaded")?.toInt() ?: 0,
                            key0Calls = doc.getLong("key_0_calls")?.toInt() ?: 0,
                            key1Calls = doc.getLong("key_1_calls")?.toInt() ?: 0,
                            key2Calls = doc.getLong("key_2_calls")?.toInt() ?: 0,
                            key3Calls = doc.getLong("key_3_calls")?.toInt() ?: 0,
                            key4Calls = doc.getLong("key_4_calls")?.toInt() ?: 0,
                            successfulCalls = doc.getLong("successful_calls")?.toInt() ?: 0,
                            failedCalls = doc.getLong("failed_calls")?.toInt() ?: 0
                        )
                    )
                }

                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }

            stats.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get usage history: ${e.message}", e)
            emptyList()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

/**
 * Data class for DAILY API usage statistics (RESETS DAILY at midnight)
 */
data class ApiUsageStats(
    val date: String,
    val spoonacularCalls: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val cacheItemsLoaded: Int,
    val key0Calls: Int,
    val key1Calls: Int,
    val key2Calls: Int,
    val key3Calls: Int,
    val key4Calls: Int,
    val successfulCalls: Int,
    val failedCalls: Int
) {
    companion object {
        const val DAILY_LIMIT = 750  // 5 keys × 150 calls each
        const val KEY_LIMIT = 150    // Per key daily limit
    }

    // Spoonacular API Stats (DAILY)
    val spoonacularUsed: Int get() = spoonacularCalls
    val spoonacularRemaining: Int get() = DAILY_LIMIT - spoonacularCalls
    val spoonacularPercentage: Float get() = (spoonacularCalls.toFloat() / DAILY_LIMIT) * 100f

    // Cache Stats (DAILY)
    val cacheTotal: Int get() = cacheHits + cacheMisses
    val cacheHitRate: Float get() = if (cacheTotal > 0) {
        (cacheHits.toFloat() / cacheTotal) * 100f
    } else 0f

    // Per-Key Stats (DAILY)
    fun getKeyUsed(keyIndex: Int): Int = when (keyIndex) {
        0 -> key0Calls
        1 -> key1Calls
        2 -> key2Calls
        3 -> key3Calls
        4 -> key4Calls
        else -> 0
    }

    fun getKeyRemaining(keyIndex: Int): Int = KEY_LIMIT - getKeyUsed(keyIndex)

    fun getKeyPercentage(keyIndex: Int): Float =
        (getKeyUsed(keyIndex).toFloat() / KEY_LIMIT) * 100f

    /**
     * Get formatted daily summary with percentages
     */
    fun getDailySummary(): String {
        return """
            📊 DAILY USAGE - $date (Resets at Midnight)
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            🌐 SPOONACULAR API CALLS (TODAY)
            Used: $spoonacularUsed / $DAILY_LIMIT calls
            Remaining: $spoonacularRemaining calls
            Percentage: ${String.format("%.1f", spoonacularPercentage)}%
            
            💾 CACHE HITS (TODAY)
            Cache Hits: $cacheHits
            Cache Misses: $cacheMisses
            Hit Rate: ${String.format("%.1f", cacheHitRate)}%
            Items from Cache: $cacheItemsLoaded
            
            🔑 PER-KEY BREAKDOWN (Each key: 150 calls/day)
            Key 0: ${getKeyUsed(0)}/$KEY_LIMIT (${getKeyRemaining(0)} left) - ${String.format("%.1f", getKeyPercentage(0))}%
            Key 1: ${getKeyUsed(1)}/$KEY_LIMIT (${getKeyRemaining(1)} left) - ${String.format("%.1f", getKeyPercentage(1))}%
            Key 2: ${getKeyUsed(2)}/$KEY_LIMIT (${getKeyRemaining(2)} left) - ${String.format("%.1f", getKeyPercentage(2))}%
            Key 3: ${getKeyUsed(3)}/$KEY_LIMIT (${getKeyRemaining(3)} left) - ${String.format("%.1f", getKeyPercentage(3))}%
            Key 4: ${getKeyUsed(4)}/$KEY_LIMIT (${getKeyRemaining(4)} left) - ${String.format("%.1f", getKeyPercentage(4))}%
        """.trimIndent()
    }

    /**
     * Get compact summary for quick view
     */
    fun getCompactSummary(): String {
        return """
            📊 Today: $date
            🌐 API: $spoonacularUsed/$DAILY_LIMIT (${String.format("%.1f", spoonacularPercentage)}%)
            💾 Cache: $cacheHits hits (${String.format("%.1f", cacheHitRate)}%)
            🔑 Keys: ${(0..4).joinToString(" | ") { "K$it:${getKeyUsed(it)}" }}
        """.trimIndent()
    }

    /**
     * Check if any key is running low (>80% used)
     */
    fun getWarnings(): List<String> {
        val warnings = mutableListOf<String>()

        if (spoonacularPercentage > 80f) {
            warnings.add("⚠️ Daily limit at ${String.format("%.1f", spoonacularPercentage)}%")
        }

        for (i in 0..4) {
            val percentage = getKeyPercentage(i)
            if (percentage > 80f) {
                warnings.add("⚠️ Key $i at ${String.format("%.1f", percentage)}%")
            }
        }

        return warnings
    }
}

/**
 * Data class for LIFETIME statistics (NEVER RESETS)
 */
data class LifetimeStats(
    val totalApiCallsEver: Int,
    val totalCacheHitsEver: Int,
    val totalCacheMissesEver: Int,
    val totalCacheItemsLoadedEver: Int,
    val totalSuccessfulCallsEver: Int,
    val totalFailedCallsEver: Int,
    val lifetimeKey0Calls: Int,
    val lifetimeKey1Calls: Int,
    val lifetimeKey2Calls: Int,
    val lifetimeKey3Calls: Int,
    val lifetimeKey4Calls: Int
) {
    // Total requests ever (API + Cache)
    val totalRequestsEver: Int get() = totalApiCallsEver + totalCacheHitsEver

    // Lifetime cache efficiency
    val lifetimeCacheTotal: Int get() = totalCacheHitsEver + totalCacheMissesEver
    val lifetimeCacheHitRate: Float get() = if (lifetimeCacheTotal > 0) {
        (totalCacheHitsEver.toFloat() / lifetimeCacheTotal) * 100f
    } else 0f

    // Lifetime API success rate
    val lifetimeSuccessRate: Float get() = if (totalApiCallsEver > 0) {
        (totalSuccessfulCallsEver.toFloat() / totalApiCallsEver) * 100f
    } else 0f

    // Total API calls saved by cache
    val totalApiCallsSaved: Int get() = totalCacheHitsEver

    /**
     * Get formatted lifetime summary
     */
    fun getLifetimeSummary(): String {
        return """
            📈 LIFETIME STATISTICS (All-Time)
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            🌐 TOTAL API CALLS (ALL TIME)
            Total API Calls: ${String.format("%,d", totalApiCallsEver)}
            Successful: ${String.format("%,d", totalSuccessfulCallsEver)}
            Failed: ${String.format("%,d", totalFailedCallsEver)}
            Success Rate: ${String.format("%.1f", lifetimeSuccessRate)}%
            
            💾 TOTAL CACHE PERFORMANCE (ALL TIME)
            Total Cache Hits: ${String.format("%,d", totalCacheHitsEver)}
            Total Cache Misses: ${String.format("%,d", totalCacheMissesEver)}
            Lifetime Hit Rate: ${String.format("%.1f", lifetimeCacheHitRate)}%
            Items Loaded from Cache: ${String.format("%,d", totalCacheItemsLoadedEver)}
            API Calls Saved: ${String.format("%,d", totalApiCallsSaved)}
            
            🔑 LIFETIME PER-KEY USAGE
            Key 0: ${String.format("%,d", lifetimeKey0Calls)} calls
            Key 1: ${String.format("%,d", lifetimeKey1Calls)} calls
            Key 2: ${String.format("%,d", lifetimeKey2Calls)} calls
            Key 3: ${String.format("%,d", lifetimeKey3Calls)} calls
            Key 4: ${String.format("%,d", lifetimeKey4Calls)} calls
            
            📊 OVERALL EFFICIENCY
            Total Requests Ever: ${String.format("%,d", totalRequestsEver)}
            Actual API Cost: ${String.format("%,d", totalApiCallsEver)}
            Cache Savings: ${String.format("%.1f", if (totalRequestsEver > 0) (totalCacheHitsEver.toFloat() / totalRequestsEver * 100) else 0f)}%
        """.trimIndent()
    }
}