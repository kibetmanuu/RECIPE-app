package ke.nucho.recipe.analytics

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Manages unique device identifiers for analytics tracking
 * Generates and persists a UUID per device installation
 */
object DeviceIdentifierManager {
    private const val PREFS_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    private var deviceId: String? = null

    /**
     * Get or create a unique device identifier
     * This persists across app launches but resets on app reinstall
     *
     * @param context Application or Activity context
     * @return Unique device UUID string
     */
    fun getDeviceId(context: Context): String {
        // Return cached ID if available
        deviceId?.let { return it }

        // Get SharedPreferences
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if ID already exists
        val existingId = prefs.getString(KEY_DEVICE_ID, null)
        if (existingId != null) {
            deviceId = existingId
            return existingId
        }

        // Generate new UUID for this device
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        deviceId = newId

        return newId
    }

    /**
     * Get short device ID for display purposes (first 8 characters)
     * Example: "a1b2c3d4" instead of "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     *
     * @param context Application or Activity context
     * @return Short device ID (8 characters)
     */
    fun getShortDeviceId(context: Context): String {
        return getDeviceId(context).take(8)
    }

    /**
     * Clear device ID (useful for testing or reset functionality)
     *
     * @param context Application or Activity context
     */
    fun clearDeviceId(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DEVICE_ID).apply()
        deviceId = null
    }
}