package com.example.recipe.repository


import android.util.Log
import com.example.recipe.data.Recipe
import com.example.recipe.data.toRecipe
import com.example.recipe.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository {

    private val apiService = ApiClient.mealApiService

    /**
     * Search recipes by name
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val response = apiService.searchMealsByName(query)
                val recipes = response.meals?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for query: $query")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error searching recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get random recipes
     */
    suspend fun getRandomRecipes(count: Int = 6): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val recipes = mutableListOf<Recipe>()

                repeat(count) {
                    try {
                        val response = apiService.getRandomMeal()
                        response.meals?.firstOrNull()?.let { meal ->
                            recipes.add(meal.toRecipe())
                        }
                    } catch (e: Exception) {
                        Log.w("RecipeRepository", "Failed to get random recipe: ${e.message}")
                    }
                }

                Log.d("RecipeRepository", "Retrieved ${recipes.size} random recipes")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting random recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get recipes by category
     */
    suspend fun getRecipesByCategory(category: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMealsByCategory(category)
                val recipes = response.meals?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for category: $category")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipes by category: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get recipe details by ID
     */
    suspend fun getRecipeById(id: String): Result<Recipe?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMealById(id)
                val recipe = response.meals?.firstOrNull()?.toRecipe()

                Log.d("RecipeRepository", "Retrieved recipe details for ID: $id")
                Result.success(recipe)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipe by ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get popular recipes (using common search terms)
     */
    suspend fun getPopularRecipes(): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val popularSearchTerms = listOf("chicken", "pasta", "beef", "fish", "vegetarian")
                val recipes = mutableListOf<Recipe>()

                for (term in popularSearchTerms) {
                    try {
                        val response = apiService.searchMealsByName(term)
                        response.meals?.take(2)?.forEach { meal ->
                            recipes.add(meal.toRecipe())
                        }
                    } catch (e: Exception) {
                        Log.w("RecipeRepository", "Failed to get recipes for term: $term")
                    }
                }

                // Remove duplicates and shuffle
                val uniqueRecipes = recipes.distinctBy { it.id }.shuffled()

                Log.d("RecipeRepository", "Retrieved ${uniqueRecipes.size} popular recipes")
                Result.success(uniqueRecipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting popular recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}