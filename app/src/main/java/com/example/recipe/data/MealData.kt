package com.example.recipe.data

import com.google.gson.annotations.SerializedName

// Response wrapper
data class MealResponse(
    @SerializedName("meals")
    val meals: List<MealData>?
)

// Individual meal data from TheMealDB
data class MealData(
    @SerializedName("idMeal")
    val idMeal: String,

    @SerializedName("strMeal")
    val strMeal: String,

    @SerializedName("strDrinkAlternate")
    val strDrinkAlternate: String?,

    @SerializedName("strCategory")
    val strCategory: String,

    @SerializedName("strArea")
    val strArea: String,

    @SerializedName("strInstructions")
    val strInstructions: String,

    @SerializedName("strMealThumb")
    val strMealThumb: String,

    @SerializedName("strTags")
    val strTags: String?,

    @SerializedName("strYoutube")
    val strYoutube: String?,

    // Ingredients (TheMealDB has up to 20 ingredients)
    @SerializedName("strIngredient1")
    val strIngredient1: String?,
    @SerializedName("strIngredient2")
    val strIngredient2: String?,
    @SerializedName("strIngredient3")
    val strIngredient3: String?,
    @SerializedName("strIngredient4")
    val strIngredient4: String?,
    @SerializedName("strIngredient5")
    val strIngredient5: String?,
    @SerializedName("strIngredient6")
    val strIngredient6: String?,
    @SerializedName("strIngredient7")
    val strIngredient7: String?,
    @SerializedName("strIngredient8")
    val strIngredient8: String?,
    @SerializedName("strIngredient9")
    val strIngredient9: String?,
    @SerializedName("strIngredient10")
    val strIngredient10: String?,

    // Measurements
    @SerializedName("strMeasure1")
    val strMeasure1: String?,
    @SerializedName("strMeasure2")
    val strMeasure2: String?,
    @SerializedName("strMeasure3")
    val strMeasure3: String?,
    @SerializedName("strMeasure4")
    val strMeasure4: String?,
    @SerializedName("strMeasure5")
    val strMeasure5: String?,
    @SerializedName("strMeasure6")
    val strMeasure6: String?,
    @SerializedName("strMeasure7")
    val strMeasure7: String?,
    @SerializedName("strMeasure8")
    val strMeasure8: String?,
    @SerializedName("strMeasure9")
    val strMeasure9: String?,
    @SerializedName("strMeasure10")
    val strMeasure10: String?
) {
    // Convert to DetailedRecipe with ingredients and measurements
    fun toDetailedRecipe(): DetailedRecipe {
        val ingredientsWithMeasurements = mutableListOf<String>()

        // Combine ingredients with their measurements
        listOf(
            strIngredient1 to strMeasure1,
            strIngredient2 to strMeasure2,
            strIngredient3 to strMeasure3,
            strIngredient4 to strMeasure4,
            strIngredient5 to strMeasure5,
            strIngredient6 to strMeasure6,
            strIngredient7 to strMeasure7,
            strIngredient8 to strMeasure8,
            strIngredient9 to strMeasure9,
            strIngredient10 to strMeasure10
        ).forEach { (ingredient, measure) ->
            if (!ingredient.isNullOrBlank()) {
                val measurement = if (!measure.isNullOrBlank()) "$measure " else ""
                ingredientsWithMeasurements.add("$measurement$ingredient".trim())
            }
        }

        return DetailedRecipe(
            id = idMeal,
            name = strMeal,
            description = "$strCategory • $strArea cuisine",
            cookingTime = "30-45 mins",
            imageUrl = strMealThumb,
            category = strCategory,
            area = strArea,
            instructions = strInstructions,
            ingredients = ingredientsWithMeasurements,
            youtubeUrl = strYoutube,
            tags = strTags?.split(",")?.map { it.trim() } ?: emptyList()
        )
    }
}

// Updated Recipe data class for your app (for list display)
data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val cookingTime: String,
    val imageUrl: String,
    val category: String = "",
    val area: String = "",
    val instructions: String = "",
    val ingredients: List<String> = emptyList(),
    val youtubeUrl: String? = null
)

// DetailedRecipe data class for detailed view
data class DetailedRecipe(
    val id: String,
    val name: String,
    val description: String,
    val cookingTime: String,
    val imageUrl: String,
    val category: String,
    val area: String,
    val instructions: String,
    val ingredients: List<String>,
    val youtubeUrl: String?,
    val tags: List<String> = emptyList()
)

// Extension function to convert MealData to Recipe (for list display)
fun MealData.toRecipe(): Recipe {
    val ingredients = listOfNotNull(
        strIngredient1?.takeIf { it.isNotBlank() },
        strIngredient2?.takeIf { it.isNotBlank() },
        strIngredient3?.takeIf { it.isNotBlank() },
        strIngredient4?.takeIf { it.isNotBlank() },
        strIngredient5?.takeIf { it.isNotBlank() },
        strIngredient6?.takeIf { it.isNotBlank() },
        strIngredient7?.takeIf { it.isNotBlank() },
        strIngredient8?.takeIf { it.isNotBlank() },
        strIngredient9?.takeIf { it.isNotBlank() },
        strIngredient10?.takeIf { it.isNotBlank() }
    )

    return Recipe(
        id = idMeal,
        name = strMeal,
        description = "$strCategory • $strArea cuisine",
        cookingTime = "30-45 mins", // TheMealDB doesn't provide cooking time
        imageUrl = strMealThumb,
        category = strCategory,
        area = strArea,
        instructions = strInstructions,
        ingredients = ingredients,
        youtubeUrl = strYoutube
    )
}