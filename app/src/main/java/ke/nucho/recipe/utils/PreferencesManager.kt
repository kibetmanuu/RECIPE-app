package ke.nucho.recipe.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app preferences including first launch detection
 */
object PreferencesManager {
    private const val PREF_NAME = "recipe_app_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if this is the first time the app is launched
     */
    fun isFirstLaunch(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark that the app has been launched before
     */
    fun setFirstLaunchComplete(context: Context) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }
    }

    /**
     * Check if onboarding has been completed
     */
    fun isOnboardingCompleted(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Mark onboarding as completed
     */
    fun setOnboardingCompleted(context: Context) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            apply()
        }
    }

    /**
     * Reset all preferences (useful for testing)
     */
    fun resetPreferences(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}