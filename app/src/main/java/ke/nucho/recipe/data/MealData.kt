package ke.nucho.recipe.data

import com.google.gson.annotations.SerializedName

// Spoonacular Recipe Search Response
data class SpoonacularSearchResponse(
    @SerializedName("results")
    val results: List<SpoonacularRecipe>?,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("number")
    val number: Int,
    @SerializedName("totalResults")
    val totalResults: Int
)

// Spoonacular Recipe (simplified for search results)
data class SpoonacularRecipe(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("image")
    val image: String?,
    @SerializedName("imageType")
    val imageType: String?
)

// Spoonacular Detailed Recipe Information
data class SpoonacularRecipeDetail(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("image")
    val image: String?,

    @SerializedName("servings")
    val servings: Int?,

    @SerializedName("readyInMinutes")
    val readyInMinutes: Int?,

    @SerializedName("cookingMinutes")
    val cookingMinutes: Int?,

    @SerializedName("preparationMinutes")
    val preparationMinutes: Int?,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("cuisines")
    val cuisines: List<String>?,

    @SerializedName("dishTypes")
    val dishTypes: List<String>?,

    @SerializedName("diets")
    val diets: List<String>?,

    @SerializedName("instructions")
    val instructions: String?,

    @SerializedName("extendedIngredients")
    val extendedIngredients: List<ExtendedIngredient>?,

    @SerializedName("sourceUrl")
    val sourceUrl: String?,

    @SerializedName("spoonacularSourceUrl")
    val spoonacularSourceUrl: String?,

    @SerializedName("cheap")
    val cheap: Boolean?,

    @SerializedName("healthScore")
    val healthScore: Double?,

    @SerializedName("vegan")
    val vegan: Boolean?,

    @SerializedName("vegetarian")
    val vegetarian: Boolean?,

    @SerializedName("glutenFree")
    val glutenFree: Boolean?,

    @SerializedName("dairyFree")
    val dairyFree: Boolean?
) {
    // Convert to DetailedRecipe
    fun toDetailedRecipe(): DetailedRecipe {
        val ingredientsList = extendedIngredients?.map { ingredient ->
            "${ingredient.measures?.metric?.amount ?: ""} ${ingredient.measures?.metric?.unitShort ?: ""} ${ingredient.name}".trim()
        } ?: emptyList()

        val category = dishTypes?.firstOrNull() ?: "Main Course"
        val area = cuisines?.firstOrNull() ?: "International"
        val cookTime = readyInMinutes?.let { "$it mins" } ?: "30-45 mins"

        // Create tags from diets and dish types
        val tagsList = mutableListOf<String>()
        diets?.let { tagsList.addAll(it) }
        if (vegan == true) tagsList.add("Vegan")
        if (vegetarian == true) tagsList.add("Vegetarian")
        if (glutenFree == true) tagsList.add("Gluten Free")
        if (dairyFree == true) tagsList.add("Dairy Free")

        return DetailedRecipe(
            id = id.toString(),
            name = title,
            description = "$category • $area cuisine",
            cookingTime = cookTime,
            imageUrl = image ?: "",
            category = category,
            area = area,
            instructions = instructions ?: summary ?: "",
            ingredients = ingredientsList,
            youtubeUrl = null,
            tags = tagsList
        )
    }
}

// Extended Ingredient from Spoonacular
data class ExtendedIngredient(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("aisle")
    val aisle: String?,

    @SerializedName("image")
    val image: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("original")
    val original: String?,

    @SerializedName("originalName")
    val originalName: String?,

    @SerializedName("amount")
    val amount: Double?,

    @SerializedName("unit")
    val unit: String?,

    @SerializedName("measures")
    val measures: Measures?
)

// Measurement details
data class Measures(
    @SerializedName("us")
    val us: Measurement?,

    @SerializedName("metric")
    val metric: Measurement?
)

data class Measurement(
    @SerializedName("amount")
    val amount: Double?,

    @SerializedName("unitShort")
    val unitShort: String?,

    @SerializedName("unitLong")
    val unitLong: String?
)

// Recipe data class for your app (for list display)
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

// Extension function to convert SpoonacularRecipe to Recipe (for list display)
fun SpoonacularRecipe.toRecipe(): Recipe {
    return Recipe(
        id = id.toString(),
        name = title,
        description = "Recipe",
        cookingTime = "30-45 mins",
        imageUrl = image ?: "",
        category = "",
        area = "",
        instructions = "",
        ingredients = emptyList(),
        youtubeUrl = null
    )
}

// Extension function to convert SpoonacularRecipeDetail to Recipe (for list display)
fun SpoonacularRecipeDetail.toRecipe(): Recipe {
    val category = dishTypes?.firstOrNull() ?: "Main Course"
    val area = cuisines?.firstOrNull() ?: "International"
    val cookTime = readyInMinutes?.let { "$it mins" } ?: "30-45 mins"

    return Recipe(
        id = id.toString(),
        name = title,
        description = "$category • $area cuisine",
        cookingTime = cookTime,
        imageUrl = image ?: "",
        category = category,
        area = area,
        instructions = instructions ?: "",
        ingredients = extendedIngredients?.map { it.original ?: it.name } ?: emptyList(),
        youtubeUrl = null
    )
}