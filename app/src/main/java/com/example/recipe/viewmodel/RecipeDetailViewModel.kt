package com.example.recipe.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.data.RecipeDetailUiState
import com.example.recipe.network.MealApiService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeDetailViewModel : ViewModel() {
    var uiState by mutableStateOf(RecipeDetailUiState())
        private set

    private val api = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MealApiService::class.java)

    fun loadRecipeDetails(recipeId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            try {
                // Using your existing getMealById method
                val response = api.getMealById(recipeId)
                val meal = response.meals?.firstOrNull()

                uiState = if (meal != null) {
                    uiState.copy(
                        isLoading = false,
                        recipe = meal.toDetailedRecipe(),
                        error = null
                    )
                } else {
                    uiState.copy(
                        isLoading = false,
                        error = "Recipe not found"
                    )
                }
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
                appendLine()
                appendLine("Ingredients:")
                recipe.ingredients.forEach { ingredient ->
                    appendLine("• $ingredient")
                }
                appendLine()
                appendLine("Instructions:")
                appendLine(recipe.instructions)

                if (!recipe.youtubeUrl.isNullOrBlank()) {
                    appendLine()
                    appendLine("Watch video: ${recipe.youtubeUrl}")
                }
            }

            // In a real app, you would use an Intent to share this text
            println("Sharing recipe: $shareText")
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}