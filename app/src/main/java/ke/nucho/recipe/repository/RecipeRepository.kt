package ke.nucho.recipe.repository

import android.util.Log
import ke.nucho.recipe.analytics.ApiUsageTracker
import ke.nucho.recipe.data.Recipe
import ke.nucho.recipe.data.toRecipe
import ke.nucho.recipe.network.ApiClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class RecipeRepository {

    private val apiService = ApiClient.mealApiService
    private val firestore = FirebaseFirestore.getInstance()
    private val recipesCollection = firestore.collection("recipes")
    private val searchCacheCollection = firestore.collection("search_cache")

    // Cache duration: 7 days in milliseconds
    private val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L

    /**
     * Search recipes by name (50 results) - WITH CACHING
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                // Try to get from cache first
                val cacheKey = "search_${query.lowercase().replace(" ", "_")}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for: $query")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                // Cache miss or expired - fetch from API
                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API for: $query")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.searchRecipes(
                    query = query,
                    number = 50,
                    addRecipeInformation = false
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                // Save to cache
                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Found ${recipes.size} recipes for query: $query")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error searching recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get random recipes (50–100 results) - WITH CACHING
     */
    suspend fun getRandomRecipes(count: Int = 100): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we have cached random recipes from today
                val cacheKey = "random_${getCurrentDateKey()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} random recipes from cache")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                // Fetch from API
                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching random recipes from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRandomRecipes(number = count)
                val recipes = response.recipes.map { it.toRecipe() }

                // Cache for today
                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Retrieved ${recipes.size} random recipes")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting random recipes: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get recipes by category/type (50 results) - WITH CACHING
     */
    suspend fun getRecipesByCategory(category: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "category_${category.lowercase()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for category: $category")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRecipesByType(
                    type = category,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Found ${recipes.size} recipes for category: $category")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipes by category: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by category (50 results) - WITH CACHING
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

                val cacheKey = "filter_cat_${category.lowercase()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for filter: $category")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRecipesByType(
                    type = type,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Filtered ${recipes.size} recipes for category: $category")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by category: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by diet (50 results) - WITH CACHING
     */
    suspend fun filterRecipesByDiet(diet: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "diet_${diet.lowercase()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for diet: $diet")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRecipesByDiet(
                    diet = diet,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Found ${recipes.size} recipes for diet: $diet")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by diet: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Filter recipes by cuisine (50 results) - WITH CACHING
     */
    suspend fun filterRecipesByCuisine(cuisine: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "cuisine_${cuisine.lowercase()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for cuisine: $cuisine")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRecipesByCuisine(
                    cuisine = cuisine,
                    number = 50
                )
                val recipes = response.results?.map { it.toRecipe() } ?: emptyList()

                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Found ${recipes.size} recipes for cuisine: $cuisine")
                Result.success(recipes)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error filtering by cuisine: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get recipe details by ID - WITH CACHING
     */
    suspend fun getRecipeById(id: String): Result<Recipe?> {
        return withContext(Dispatchers.IO) {
            try {
                val recipeId = id.toIntOrNull()
                if (recipeId == null) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid recipe ID: $id"))
                }

                // Try to get from cache first
                val cachedRecipe = getCachedRecipe(id)
                if (cachedRecipe != null && !isCacheExpired(cachedRecipe.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded recipe from cache for ID: $id")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit("recipe_$id", 1)

                    return@withContext Result.success(cachedRecipe.recipe)
                }

                // Fetch from API
                val missReason = if (cachedRecipe != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching recipe from Spoonacular API for ID: $id")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss("recipe_$id", missReason)

                val recipeDetail = apiService.getRecipeById(
                    recipeId = recipeId,
                    includeNutrition = false
                )
                val recipe = recipeDetail.toRecipe()

                // Cache the recipe
                cacheRecipe(id, recipe)

                Log.d("RecipeRepository", "🌐 API: Retrieved recipe details for ID: $id")
                Result.success(recipe)
            } catch (e: Exception) {
                Log.e("RecipeRepository", "Error getting recipe by ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get popular recipes (100 results) - WITH CACHING
     */
    suspend fun getPopularRecipes(): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "popular_${getCurrentDateKey()}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} popular recipes from cache")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching popular recipes from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

                val response = apiService.getRandomRecipes(number = 100)
                val recipes = response.recipes.map { it.toRecipe() }

                cacheSearchResults(cacheKey, recipes)

                Log.d("RecipeRepository", "🌐 API: Retrieved ${recipes.size} popular recipes")
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
     * Search by ingredients (50 results) - WITH CACHING
     */
    suspend fun searchRecipesByIngredients(ingredients: String): Result<List<Recipe>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "ingredients_${ingredients.lowercase().replace(" ", "_")}"
                val cachedRecipes = getCachedSearchResults(cacheKey)

                if (cachedRecipes != null && !isCacheExpired(cachedRecipes.timestamp)) {
                    Log.d("RecipeRepository", "✅ CACHE HIT: Loaded ${cachedRecipes.recipes.size} recipes from cache for ingredients: $ingredients")

                    // 📊 Track cache hit
                    ApiUsageTracker.trackCacheHit(cacheKey, cachedRecipes.recipes.size)

                    return@withContext Result.success(cachedRecipes.recipes)
                }

                val missReason = if (cachedRecipes != null) "expired" else "not_found"
                Log.d("RecipeRepository", "❌ CACHE MISS ($missReason) - fetching from Spoonacular API")

                // 📊 Track cache miss
                ApiUsageTracker.trackCacheMiss(cacheKey, missReason)

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

                cacheSearchResults(cacheKey, recipes)

                Result.success(recipes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ==========================================
    // FIREBASE CACHING HELPER METHODS
    // ==========================================

    /**
     * Get cached search results from Firestore
     */
    private suspend fun getCachedSearchResults(cacheKey: String): CachedSearchResults? {
        return try {
            val doc = searchCacheCollection.document(cacheKey).get().await()
            if (doc.exists()) {
                val timestamp = doc.getLong("timestamp") ?: 0L
                val recipesData = doc.get("recipes") as? List<Map<String, Any>> ?: return null

                val recipes = recipesData.mapNotNull { recipeMap ->
                    try {
                        Recipe(
                            id = recipeMap["id"] as? String ?: "",
                            name = recipeMap["name"] as? String ?: "",
                            description = recipeMap["description"] as? String ?: "",
                            cookingTime = recipeMap["cookingTime"] as? String ?: "",
                            imageUrl = recipeMap["imageUrl"] as? String ?: "",
                            category = recipeMap["category"] as? String ?: "",
                            area = recipeMap["area"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                CachedSearchResults(recipes, timestamp)
            } else null
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error getting cached search results: ${e.message}", e)
            null
        }
    }

    /**
     * Cache search results to Firestore
     */
    private suspend fun cacheSearchResults(cacheKey: String, recipes: List<Recipe>) {
        try {
            val recipesData = recipes.map { recipe ->
                hashMapOf(
                    "id" to recipe.id,
                    "name" to recipe.name,
                    "description" to recipe.description,
                    "cookingTime" to recipe.cookingTime,
                    "imageUrl" to recipe.imageUrl,
                    "category" to recipe.category,
                    "area" to recipe.area
                )
            }

            val cacheData = hashMapOf(
                "recipes" to recipesData,
                "timestamp" to System.currentTimeMillis()
            )

            searchCacheCollection.document(cacheKey).set(cacheData).await()
            Log.d("RecipeRepository", "💾 Cached ${recipes.size} recipes with key: $cacheKey")
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error caching search results: ${e.message}", e)
        }
    }

    /**
     * Get cached single recipe from Firestore
     */
    private suspend fun getCachedRecipe(recipeId: String): CachedRecipe? {
        return try {
            val doc = recipesCollection.document(recipeId).get().await()
            if (doc.exists()) {
                val timestamp = doc.getLong("timestamp") ?: 0L
                val recipe = Recipe(
                    id = doc.getString("id") ?: "",
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    cookingTime = doc.getString("cookingTime") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    category = doc.getString("category") ?: "",
                    area = doc.getString("area") ?: ""
                )
                CachedRecipe(recipe, timestamp)
            } else null
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error getting cached recipe: ${e.message}", e)
            null
        }
    }

    /**
     * Cache single recipe to Firestore
     */
    private suspend fun cacheRecipe(recipeId: String, recipe: Recipe) {
        try {
            val recipeData = hashMapOf(
                "id" to recipe.id,
                "name" to recipe.name,
                "description" to recipe.description,
                "cookingTime" to recipe.cookingTime,
                "imageUrl" to recipe.imageUrl,
                "category" to recipe.category,
                "area" to recipe.area,
                "timestamp" to System.currentTimeMillis()
            )

            recipesCollection.document(recipeId).set(recipeData).await()
            Log.d("RecipeRepository", "💾 Cached recipe: $recipeId")
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error caching recipe: ${e.message}", e)
        }
    }

    /**
     * Check if cache is expired
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS
    }

    /**
     * Get current date key (format: YYYYMMDD)
     */
    private fun getCurrentDateKey(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}" +
                "${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}" +
                "${String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))}"
    }

    // Data classes for caching
    private data class CachedRecipe(val recipe: Recipe, val timestamp: Long)
    private data class CachedSearchResults(val recipes: List<Recipe>, val timestamp: Long)
}