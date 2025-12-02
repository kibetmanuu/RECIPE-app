package ke.nucho.recipe.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.nucho.recipe.data.RecipeDetailUiState
import ke.nucho.recipe.network.ApiClient
import kotlinx.coroutines.launch

class RecipeDetailViewModel : ViewModel() {
    var uiState by mutableStateOf(RecipeDetailUiState())
        private set

    // Use ApiClient instead of creating a new Retrofit instance
    private val api = ApiClient.mealApiService

    fun loadRecipeDetails(recipeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            try {
                // Convert String ID to Int for Spoonacular
                val id = recipeId.toIntOrNull()

                if (id == null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Invalid recipe ID"
                    )
                    return@launch
                }

                // Get detailed recipe information from Spoonacular
                val recipeDetail = api.getRecipeById(
                    recipeId = id,
                    includeNutrition = false
                )

                uiState = uiState.copy(
                    isLoading = false,
                    recipe = recipeDetail.toDetailedRecipe(),
                    error = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Failed to load recipe: ${e.localizedMessage}"
                )
            }
        }
    }

    fun toggleFavorite() {
        uiState = uiState.copy(isFavorite = !uiState.isFavorite)
        // Here you can implement actual favorite saving logic
        // For example, save to SharedPreferences or Room database
    }

    fun shareRecipe() {
        // Implement sharing logic here
        uiState.recipe?.let { recipe ->
            // Create share text with recipe details
            val shareText = buildString {
                appendLine("Check out this amazing recipe: ${recipe.name}")
                appendLine()
                appendLine("Category: ${recipe.category}")
                appendLine("Cuisine: ${recipe.area}")
                appendLine("Cooking Time: ${recipe.cookingTime}")
                appendLine()

                if (recipe.tags.isNotEmpty()) {
                    appendLine("Tags: ${recipe.tags.joinToString(", ")}")
                    appendLine()
                }

                appendLine("Ingredients:")
                recipe.ingredients.forEach { ingredient ->
                    appendLine("• $ingredient")
                }
                appendLine()
                appendLine("Instructions:")
                appendLine(recipe.instructions)
            }

            // In a real app, you would use an Intent to share this text
            println("Sharing recipe: $shareText")
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}