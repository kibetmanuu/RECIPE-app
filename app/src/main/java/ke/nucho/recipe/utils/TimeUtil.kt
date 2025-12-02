package ke.nucho.recipe.utils


/**
 * Utility class for extracting and formatting cooking times from recipe text
 * Provides intelligent time extraction and estimation for recipe cards and details
 */
object TimeUtils {

    /**
     * Extracts cooking time from recipe description or instructions
     * Looks for patterns like "30 minutes", "1 hour", "2 hrs", etc.
     *
     * @param description The recipe description text
     * @param instructions The recipe instructions text (optional)
     * @return Formatted time string (e.g., "30m", "1h 30m") or "Quick & Easy" if not found
     */
    fun extractCookingTime(description: String, instructions: String = ""): String {
        val text = "$description $instructions".lowercase()

        // Patterns to match different time formats
        val timePatterns = listOf(
            // Minutes patterns - matches "30 minutes", "15 mins", "10 min"
            Regex("""\b(\d+)\s*(?:minutes?|mins?|min)\b"""),
            // Hours patterns - matches "2 hours", "1 hrs", "3 hr"
            Regex("""\b(\d+)\s*(?:hours?|hrs?|hr)\b"""),
            // Combined patterns - matches "1 hour 30 minutes", "2 hrs 15 mins"
            Regex("""\b(\d+)\s*(?:hours?|hrs?|hr)\s*(?:and\s*)?(\d+)\s*(?:minutes?|mins?|min)\b"""),
            // Prep/cook time specific patterns - matches "prep time: 30 minutes"
            Regex("""(?:prep|cook|cooking|preparation)\s*(?:time)?:?\s*(\d+)\s*(?:minutes?|mins?|min)"""),
            Regex("""(?:prep|cook|cooking|preparation)\s*(?:time)?:?\s*(\d+)\s*(?:hours?|hrs?|hr)"""),
            // Total time patterns - matches "total time: 45 minutes"
            Regex("""(?:total|ready in|takes?)\s*(?:time)?:?\s*(\d+)\s*(?:minutes?|mins?|min)"""),
            // Decimal hours - matches "1.5 hours", "2.25 hrs"
            Regex("""\b(\d+\.?\d*)\s*(?:hours?|hrs?|hr)\b""")
        )

        for ((index, pattern) in timePatterns.withIndex()) {
            val match = pattern.find(text)
            if (match != null) {
                return when (index) {
                    0 -> { // Minutes only
                        val minutes = match.groupValues[1].toIntOrNull() ?: 0
                        formatTime(minutes)
                    }
                    1 -> { // Hours only
                        val hours = match.groupValues[1].toIntOrNull() ?: 0
                        formatTime(hours * 60)
                    }
                    2 -> { // Hours and minutes combined
                        val hours = match.groupValues[1].toIntOrNull() ?: 0
                        val minutes = match.groupValues[2].toIntOrNull() ?: 0
                        formatTime(hours * 60 + minutes)
                    }
                    3, 4, 5 -> { // Prep/cook/total time patterns
                        val time = match.groupValues[1].toIntOrNull() ?: 0
                        if (text.contains("hour")) formatTime(time * 60) else formatTime(time)
                    }
                    6 -> { // Decimal hours
                        val hours = match.groupValues[1].toDoubleOrNull() ?: 0.0
                        formatTime((hours * 60).toInt())
                    }
                    else -> formatTime(30) // Default fallback
                }
            }
        }

        // If no time pattern found, return indicator for estimation
        return "Quick & Easy"
    }

    /**
     * Formats time in minutes to a readable string
     *
     * @param totalMinutes Total cooking time in minutes
     * @return Formatted string (e.g., "30m", "1h", "1h 30m")
     */
    fun formatTime(totalMinutes: Int): String {
        return when {
            totalMinutes <= 0 -> "Quick"
            totalMinutes < 60 -> "${totalMinutes}m"
            totalMinutes % 60 == 0 -> "${totalMinutes / 60}h"
            totalMinutes < 120 -> "1h ${totalMinutes % 60}m"
            else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        }
    }

    /**
     * Estimates cooking time based on recipe complexity and keywords
     * This is used as a fallback when no explicit time is found
     *
     * @param description Recipe description text
     * @param instructions Recipe instructions text (optional)
     * @param ingredientCount Number of ingredients (optional)
     * @param category Recipe category (optional)
     * @return Estimated time range string
     */
    fun estimateCookingTime(
        description: String,
        instructions: String = "",
        ingredientCount: Int = 0,
        category: String = ""
    ): String {
        val text = "$description $instructions $category".lowercase()

        // Very quick dishes (5-15 minutes)
        val veryQuickIndicators = listOf(
            "instant", "microwave", "no-cook", "raw", "smoothie",
            "juice", "drink", "cocktail", "salad dressing"
        )

        // Quick dishes (10-25 minutes)
        val quickIndicators = listOf(
            "quick", "easy", "simple", "fast", "rapid", "express",
            "salad", "sandwich", "toast", "scrambled", "fried egg",
            "pasta salad", "green salad", "fruit salad"
        )

        // Medium dishes (25-45 minutes)
        val mediumIndicators = listOf(
            "saute", "sautéed", "pan-fried", "skillet", "stir-fry", "stir fry",
            "pasta", "rice", "risotto", "omelet", "pancake", "grilled",
            "broiled", "steamed", "poached", "scramble"
        )

        // Long dishes (45+ minutes)
        val longIndicators = listOf(
            "bake", "baked", "roast", "roasted", "slow", "braise", "braised",
            "stew", "casserole", "marinate", "marinated", "cure", "smoke",
            "barbecue", "bbq", "slow-cook", "oven", "baking"
        )

        // Very long dishes (2+ hours)
        val veryLongIndicators = listOf(
            "slow cooker", "crockpot", "overnight", "24 hour", "cure",
            "ferment", "age", "dry", "dehydrate", "smoking"
        )

        // Category-based estimation
        val categoryEstimates = mapOf(
            "beverages" to "5-10m",
            "salad" to "15-20m",
            "appetizer" to "20-30m",
            "soup" to "45m-1h",
            "main course" to "30-45m",
            "dessert" to "1h-2h",
            "bread" to "2h-4h",
            "cake" to "1h-1.5h"
        )

        return when {
            veryLongIndicators.any { text.contains(it) } -> "2h+"
            longIndicators.any { text.contains(it) } -> "1h-2h"
            mediumIndicators.any { text.contains(it) } -> "30-45m"
            quickIndicators.any { text.contains(it) } -> "15-25m"
            veryQuickIndicators.any { text.contains(it) } -> "5-15m"
            categoryEstimates[category.lowercase()] != null -> categoryEstimates[category.lowercase()]!!
            ingredientCount > 15 -> "1h-1.5h"
            ingredientCount > 10 -> "45m-1h"
            ingredientCount > 5 -> "30-45m"
            ingredientCount > 0 -> "20-30m"
            else -> "25-35m"
        }
    }

    /**
     * Gets a smart cooking time by first trying to extract, then estimating
     * This is the main function to use in your UI components
     *
     * @param description Recipe description
     * @param instructions Recipe instructions (optional)
     * @param ingredientCount Number of ingredients (optional)
     * @param category Recipe category (optional)
     * @return Best available time estimate
     */
    fun getSmartCookingTime(
        description: String,
        instructions: String = "",
        ingredientCount: Int = 0,
        category: String = ""
    ): String {
        val extractedTime = extractCookingTime(description, instructions)

        return if (extractedTime == "Quick & Easy") {
            // Fallback to estimation if no explicit time found
            estimateCookingTime(description, instructions, ingredientCount, category)
        } else {
            extractedTime
        }
    }

    /**
     * Determines recipe difficulty based on time and keywords
     * Returns emoji indicators for UI display
     *
     * @param cookingTime The cooking time string
     * @param description Recipe description
     * @param ingredientCount Number of ingredients
     * @return Emoji string representing difficulty level
     */
    fun getDifficultyIndicator(
        cookingTime: String,
        description: String = "",
        ingredientCount: Int = 0
    ): String? {
        val text = description.lowercase()

        // Check for explicit difficulty mentions
        when {
            text.contains("easy") || text.contains("simple") || text.contains("beginner") -> return "🟢"
            text.contains("intermediate") || text.contains("medium") -> return "🟡"
            text.contains("hard") || text.contains("difficult") || text.contains("advanced") || text.contains("expert") -> return "🔴"
        }

        // Estimate based on time and complexity
        return when {
            cookingTime.contains("5-") || cookingTime.endsWith("m") && cookingTime.replace("m", "").toIntOrNull()?.let { it <= 20 } == true -> "🟢"
            cookingTime.contains("2h+") || cookingTime.contains("3h") || cookingTime.contains("4h") -> "🔴"
            cookingTime.contains("1h") && cookingTime.contains("2h") -> "🟡"
            cookingTime.contains("1h") || ingredientCount > 12 -> "🟡"
            ingredientCount > 20 -> "🔴"
            else -> "🟢"
        }
    }

    /**
     * Converts time string to minutes for sorting or calculations
     *
     * @param timeString Formatted time string (e.g., "1h 30m", "45m")
     * @return Total minutes as integer, or -1 if cannot parse
     */
    fun timeStringToMinutes(timeString: String): Int {
        val cleanTime = timeString.lowercase().replace(" ", "")

        return try {
            when {
                cleanTime.contains("h") && cleanTime.contains("m") -> {
                    val parts = cleanTime.split("h")
                    val hours = parts[0].toInt()
                    val minutes = parts[1].replace("m", "").toInt()
                    hours * 60 + minutes
                }
                cleanTime.contains("h") -> {
                    cleanTime.replace("h", "").toInt() * 60
                }
                cleanTime.contains("m") -> {
                    cleanTime.replace("m", "").toInt()
                }
                cleanTime.contains("-") -> {
                    // Handle ranges like "30-45m" - return average
                    val range = cleanTime.replace("m", "").split("-")
                    val min = range[0].toInt()
                    val max = range[1].toInt()
                    (min + max) / 2
                }
                else -> -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Formats minutes back to readable time string
     * Useful when you need to convert back from calculations
     *
     * @param minutes Total minutes
     * @return Formatted time string
     */
    fun minutesToTimeString(minutes: Int): String = formatTime(minutes)
}