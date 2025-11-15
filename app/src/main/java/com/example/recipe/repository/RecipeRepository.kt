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
     * Search recipes by name (50 results)
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val response = apiService.searchRecipes(
                    query = query,
                    number = 50,
                    addRecipeInformation = false
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for query: $query")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error searching recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get random recipes (50–100 results)
     */
    suspend fun getRandomRecipes(count: Int = 100): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRandomRecipes(number = count)
                val recipes = response.recipes.map { it.toRecipe() }

                Log.d("RecipeRepository", "Retrieved ${recipes.size} random recipes")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting random recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get recipes by category/type (50 results)
     */
    suspend fun getRecipesByCategory(category: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRecipesByType(
                    type = category,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for category: $category")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipes by category: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by category (50 results)
     */
    suspend fun filterRecipesByCategory(category: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val type = when (category.lowercase()) {
                    "easy" -> "main course"
                    "quick" -> "fingerfood"
                    "healthy" -> "salad"
                    "low_calorie" -> "side dish"
                    else -> category
                }

                val response = apiService.getRecipesByType(
                    type = type,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Filtered ${recipes.size} recipes for category: $category")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by category: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by diet (50 results)
     */
    suspend fun filterRecipesByDiet(diet: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRecipesByDiet(
                    diet = diet,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for diet: $diet")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by diet: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by cuisine (50 results)
     */
    suspend fun filterRecipesByCuisine(cuisine: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRecipesByCuisine(
                    cuisine = cuisine,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                Log.d("RecipeRepository", "Found ${recipes.size} recipes for cuisine: $cuisine")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by cuisine: ${e.message}", e)
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
                val recipeId = id.toIntOrNull()
                if (recipeId == null) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid recipe ID: $id"))
                }

                val recipeDetail = apiService.getRecipeById(
                    recipeId = recipeId,
                    includeNutrition = false
                )
                val recipe = recipeDetail.toRecipe()

                Log.d("RecipeRepository", "Retrieved recipe details for ID: $id")
                Result.success(recipe)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipe by ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get popular recipes (100 results)
     */
    suspend fun getPopularRecipes(): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRandomRecipes(number = 100)
                val recipes = response.recipes.map { it.toRecipe() }

                Log.d("RecipeRepository", "Retrieved ${recipes.size} popular recipes")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting popular recipes: ${e.message}", e)
                getPopularRecipesFallback()
            }
        }
    }

    /**
     * Fallback for popular recipes (up to 15 results)
     */
    private suspend fun getPopularRecipesFallback(): Result<List<Recipe>> {
        return try {
            val popularSearchTerms = listOf("chicken", "pasta", "pizza", "salad", "soup")
            val recipes = mutableListOf<Recipe>()

            for (term in popularSearchTerms.take(5)) {
                try {
                    val response = apiService.searchRecipes(
                        query = term,
                        number = 3,
                        addRecipeInformation = false
                    )
                    response.results?.forEach { recipe ->
                        recipes.add(recipe.toRecipe())
                    }
                } catch (_: Exception) {}
            }

            val uniqueRecipes = recipes.distinctBy { it.id }
            Result.success(uniqueRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search by ingredients (50 results)
     */
    suspend fun searchRecipesByIngredients(ingredients: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRecipesByIngredients(
                    ingredients = ingredients,
                    number = 50
                )

                val recipes = response.map { result ->
                    Recipe(
                        id = result.id.toString(),
                        name = result.title,
                        description = "Uses ${result.usedIngredientCount} of your ingredients",
                        cookingTime = "30-45 mins",
                        imageUrl = result.image ?: "",
                        category = "",
                        area = ""
                    )
                }

                Result.success(recipes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
