package com.example.recipe.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipe.data.Recipe
import com.example.recipe.repository.RecipeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = RecipeRepository()

    // This holds the full recipe list so we can filter/reset easily
    private var allRecipes: List<Recipe> = emptyList()

    // UI State
    var uiState by mutableStateOf(MainUiState())
        private set

    // Search job for debouncing
    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    /** Load initial popular recipes */
    private fun loadInitialData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            repository.getPopularRecipes().fold(
                onSuccess = { recipes ->
                    allRecipes = recipes
                    uiState = uiState.copy(
                        recipes = recipes,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Failed to load recipes: ${exception.message}"
                    )
                }
            )
        }
    }

    /** Search recipes with debouncing */
    fun searchRecipes(query: String) {
        uiState = uiState.copy(searchQuery = query)

        // Cancel previous search job
        searchJob?.cancel()

        if (query.isBlank()) {
            loadInitialData()
            return
        }

        // Debounce search - wait 500ms before searching
        searchJob = viewModelScope.launch {
            delay(500)
            uiState = uiState.copy(isLoading = true, error = null)
            repository.searchRecipes(query).fold(
                onSuccess = { recipes ->
                    allRecipes = recipes
                    uiState = uiState.copy(
                        recipes = recipes,
                        isLoading = false,
                        error = if (recipes.isEmpty()) "No recipes found for '$query'" else null
                    )
                },
                onFailure = { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Search failed: ${exception.message}"
                    )
                }
            )
        }
    }

    /** Filter recipes by category */
    fun filterByCategory(category: String) {
        uiState = uiState.copy(
            currentFilter = category,
            recipes = allRecipes.filter { recipe ->
                when (category) {
                    "easy" -> recipe.cookingTime.contains("30") ||
                            recipe.cookingTime.contains("15") ||
                            recipe.cookingTime.contains("20")
                    "low_calorie" -> recipe.description.contains("low", ignoreCase = true) ||
                            recipe.description.contains("light", ignoreCase = true)
                    "quick" -> recipe.cookingTime.contains("15") ||
                            recipe.cookingTime.contains("10")
                    "healthy" -> recipe.description.contains("healthy", ignoreCase = true) ||
                            recipe.description.contains("fresh", ignoreCase = true)
                    else -> true
                }
            }
        )
    }

    /** Clear filter */
    fun clearFilter() {
        uiState = uiState.copy(
            currentFilter = "",
            recipes = allRecipes
        )
    }

    /** Refresh recipes */
    fun refreshRecipes() {
        if (uiState.searchQuery.isBlank()) {
            loadInitialData()
        } else {
            searchRecipes(uiState.searchQuery)
        }
    }

    /** Clear error */
    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

/** UI State data class */
data class MainUiState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentFilter: String = ""
)
