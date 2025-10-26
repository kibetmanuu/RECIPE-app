package com.example.recipe.data


import com.google.gson.annotations.SerializedName

data class Meal(
    @SerializedName("idMeal")
    val idMeal: String?,

    @SerializedName("strMeal")
    val strMeal: String?,

    @SerializedName("strCategory")
    val strCategory: String?,

    @SerializedName("strArea")
    val strArea: String?,

    @SerializedName("strMealThumb")
    val strMealThumb: String?,

    // Add other properties as needed from MealDB API
    @SerializedName("strInstructions")
    val strInstructions: String?,

    @SerializedName("strIngredient1")
    val strIngredient1: String?,
    // ... add more ingredients as needed
)