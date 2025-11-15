package com.example.recipe.data

import com.google.gson.annotations.SerializedName

data class Meal(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("image")
    val image: String?,

    @SerializedName("dishTypes")
    val dishTypes: List<String>?,

    @SerializedName("cuisines")
    val cuisines: List<String>?,

    @SerializedName("instructions")
    val instructions: String?,

    @SerializedName("readyInMinutes")
    val readyInMinutes: Int?
) {
    fun toRecipe(): Recipe {
        return Recipe(
            id = id?.toString() ?: "",
            name = title ?: "",
            description = "${dishTypes?.firstOrNull() ?: "Recipe"} • ${cuisines?.firstOrNull() ?: "International"} cuisine",
            cookingTime = readyInMinutes?.let { "$it mins" } ?: "30-45 mins",
            imageUrl = image ?: "",
            category = dishTypes?.firstOrNull() ?: "",
            area = cuisines?.firstOrNull() ?: "",
            instructions = instructions ?: "",
            ingredients = emptyList(),
            youtubeUrl = null
        )
    }
}