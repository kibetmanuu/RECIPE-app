package ke.nucho.recipe.network

import ke.nucho.recipe.data.SpoonacularSearchResponse
import ke.nucho.recipe.data.SpoonacularRecipeDetail
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MealApiService {

    // Search recipes by query (replaces searchMealsByName)
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false
    ): SpoonacularSearchResponse

    // Get random recipes (replaces getRandomMeal)
    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 1
    ): RandomRecipesResponse

    // Get recipe details by ID (replaces getMealById)
    @GET("recipes/{id}/information")
    suspend fun getRecipeById(
        @Path("id") recipeId: Int,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): SpoonacularRecipeDetail

    // Search recipes by cuisine (replaces getMealsByArea)
    @GET("recipes/complexSearch")
    suspend fun getRecipesByCuisine(
        @Query("cuisine") cuisine: String,
        @Query("number") number: Int = 10
    ): SpoonacularSearchResponse

    // Search recipes by type/category (replaces getMealsByCategory)
    @GET("recipes/complexSearch")
    suspend fun getRecipesByType(
        @Query("type") type: String,
        @Query("number") number: Int = 10
    ): SpoonacularSearchResponse

    // Search recipes by diet (new feature)
    @GET("recipes/complexSearch")
    suspend fun getRecipesByDiet(
        @Query("diet") diet: String,
        @Query("number") number: Int = 10
    ): SpoonacularSearchResponse

    // Search recipes by ingredients (new feature)
    @GET("recipes/findByIngredients")
    suspend fun getRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 10,
        @Query("ranking") ranking: Int = 2
    ): List<RecipeByIngredientsResponse>

    // Autocomplete recipe search (new feature)
    @GET("recipes/autocomplete")
    suspend fun autocompleteRecipeSearch(
        @Query("query") query: String,
        @Query("number") number: Int = 5
    ): List<AutocompleteRecipe>
}

// Response for random recipes
data class RandomRecipesResponse(
    val recipes: List<SpoonacularRecipeDetail>
)

// Response for ingredient-based search
data class RecipeByIngredientsResponse(
    val id: Int,
    val title: String,
    val image: String?,
    val imageType: String?,
    val usedIngredientCount: Int,
    val missedIngredientCount: Int,
    val likes: Int
)

// Autocomplete suggestion
data class AutocompleteRecipe(
    val id: Int,
    val title: String,
    val imageType: String?
)