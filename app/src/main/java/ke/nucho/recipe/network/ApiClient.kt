package ke.nucho.recipe.network

import android.util.Log
import ke.nucho.recipe.config.RemoteConfigManager
import ke.nucho.recipe.analytics.ApiUsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"
    private const val BASE_URL = "https://api.spoonacular.com/"

    // API Key Interceptor - gets RANDOM API key from Remote Config + tracks usage
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // 🎲 Get RANDOM API key from Remote Config (distributes load!)
        val apiKey = RemoteConfigManager.getRandomApiKey()
        val keyIndex = RemoteConfigManager.getLastUsedKeyIndex()

        // Add API key as query parameter
        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("apiKey", apiKey)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        // Make the request
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(newRequest)
        val duration = System.currentTimeMillis() - startTime

        // 📊 Track the API call (async - doesn't block the request)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val endpoint = originalUrl.encodedPath
                val success = response.isSuccessful

                Log.d(TAG, "📞 API Call: endpoint=$endpoint, key=$keyIndex, success=$success, duration=${duration}ms")

                ApiUsageTracker.trackApiCall(
                    apiKeyIndex = keyIndex,
                    endpoint = endpoint,
                    success = success
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to track API call: ${e.message}")
            }
        }

        response
    }

    // Create OkHttp client with logging and API key interceptor
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(apiKeyInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Create Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Create API service
    val mealApiService: MealApiService = retrofit.create(MealApiService::class.java)
}