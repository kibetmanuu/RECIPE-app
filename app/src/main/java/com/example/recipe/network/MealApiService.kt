package com.example.recipe.network


import com.example.recipe.data.MealResponse
import retrofit2.http.GET
import retrofit2.http.Query



interface MealApiService {

    // Search meals by name
    @GET("search.php")
    suspend fun searchMealsByName(@Query("s") mealName: String): MealResponse

    // Get random meal
    @GET("random.php")
    suspend fun getRandomMeal(): MealResponse

    // Search meals by first letter
    @GET("search.php")
    suspend fun searchMealsByFirstLetter(@Query("f") firstLetter: String): MealResponse

    // Get meal details by ID
    @GET("lookup.php")
    suspend fun getMealById(@Query("i") mealId: String): MealResponse

    // Filter meals by category
    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): MealResponse

    // Filter meals by area
    @GET("filter.php")
    suspend fun getMealsByArea(@Query("a") area: String): MealResponse

    // Get all categories
    @GET("categories.php")
    suspend fun getAllCategories(): MealResponse
}