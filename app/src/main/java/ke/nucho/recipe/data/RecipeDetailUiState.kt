package ke.nucho.recipe.data


data class RecipeDetailUiState(
    val isLoading: Boolean = false,
    val recipe: DetailedRecipe? = null,
    val error: String? = null,
    val isFavorite: Boolean = false
)
