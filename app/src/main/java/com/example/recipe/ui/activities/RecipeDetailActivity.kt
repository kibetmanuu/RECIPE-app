package com.example.recipe.ui.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.recipe.data.DetailedRecipe
import com.example.recipe.ui.theme.RecipeTheme
import com.example.recipe.viewmodel.RecipeDetailViewModel
import kotlinx.coroutines.delay

class RecipeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getStringExtra("RECIPE_ID") ?: ""
        val recipeName = intent.getStringExtra("RECIPE_NAME") ?: ""

        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecipeDetailScreen(
                        recipeId = recipeId,
                        recipeName = recipeName
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    recipeName: String,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    // Simple favorite state management using SharedPreferences
    var isFavorite by remember { mutableStateOf(false) }

    // Load favorite state from SharedPreferences
    LaunchedEffect(recipeId) {
        val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
        isFavorite = prefs.getBoolean(recipeId, false)
        viewModel.loadRecipeDetails(recipeId)
    }

    // Function to toggle favorite
    fun toggleFavorite() {
        val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
        val newFavoriteState = !isFavorite

        prefs.edit().putBoolean(recipeId, newFavoriteState).apply()
        isFavorite = newFavoriteState

        // Optional: Save additional recipe data for favorites list
        if (newFavoriteState && uiState.recipe != null) {
            val recipe = uiState.recipe
            prefs.edit()
                .putString("${recipeId}_name", recipe.name)
                .putString("${recipeId}_image", recipe.imageUrl)
                .putString("${recipeId}_category", recipe.category)
                .putString("${recipeId}_area", recipe.area)
                .putLong("${recipeId}_date_added", System.currentTimeMillis())
                .apply()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = recipeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { (context as ComponentActivity).finish() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Updated Favorite Button
                IconButton(
                    onClick = { toggleFavorite() }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
                // Share Button
                IconButton(
                    onClick = {
                        uiState.recipe?.let { recipe ->
                            val shareText = buildString {
                                appendLine("🍽️ ${recipe.name}")
                                appendLine()
                                appendLine("📍 Category: ${recipe.category}")
                                appendLine("🌍 Cuisine: ${recipe.area}")
                                appendLine()
                                appendLine("📝 Ingredients:")
                                recipe.ingredients.forEach { ingredient ->
                                    appendLine("• $ingredient")
                                }
                                appendLine()
                                appendLine("👨‍🍳 Instructions:")
                                appendLine(recipe.instructions)

                                if (!recipe.youtubeUrl.isNullOrBlank()) {
                                    appendLine()
                                    appendLine("📹 Watch Tutorial: ${recipe.youtubeUrl}")
                                }

                                if (recipe.tags.isNotEmpty()) {
                                    appendLine()
                                    appendLine("🏷️ Tags: ${recipe.tags.joinToString(", ")}")
                                }

                                appendLine()
                                appendLine("🖼️ Recipe Image: ${recipe.imageUrl}")
                            }

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.name}")
                            }

                            val chooser = Intent.createChooser(intent, "Share Recipe")
                            context.startActivity(chooser)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Recipe"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // Content based on state
        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error,
                    onRetry = { viewModel.loadRecipeDetails(recipeId) },
                    onDismiss = { viewModel.clearError() }
                )
            }
            uiState.recipe != null -> {
                RecipeDetailContent(recipe = uiState.recipe)
            }
        }
    }
}

@Composable
fun SimpleRecipeSlideshow(
    recipe: DetailedRecipe,
    modifier: Modifier = Modifier,
    autoScrollDelay: Long = 3000L // 3 seconds
) {
    // Create a list with the main image and some cooking-related placeholder images
    val imageList = remember(recipe) {
        mutableListOf<String>().apply {
            add(recipe.imageUrl) // Original TheMealDB image

            // Add some cooking-related placeholder images based on category
            when (recipe.category.lowercase()) {
                "dessert" -> {
                    add("https://images.unsplash.com/photo-1551024506-0bccd828d307") // Baking
                    add("https://images.unsplash.com/photo-1578985545062-69928b1d9587") // Dessert
                }
                "chicken" -> {
                    add("https://images.unsplash.com/photo-1532550907401-a500c9a57435") // Cooking
                    add("https://images.unsplash.com/photo-1606728035253-49e8a23146de") // Chicken dish
                }
                "seafood" -> {
                    add("https://images.unsplash.com/photo-1544943910-4c1dc44aab44") // Seafood
                    add("https://images.unsplash.com/photo-1615141982883-c7ad0e69fd62") // Fish cooking
                }
                "vegetarian" -> {
                    add("https://images.unsplash.com/photo-1540420773420-3366772f4999") // Vegetables
                    add("https://images.unsplash.com/photo-1512621776951-a57141f2eefd") // Salad
                }
                else -> {
                    // Generic cooking images
                    add("https://images.unsplash.com/photo-1556909114-f6e7ad7d3136") // Kitchen
                    add("https://images.unsplash.com/photo-1556908114-4d4d6dea2b6d") // Cooking process
                }
            }
        }
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }

    // Auto-scroll effect
    LaunchedEffect(currentImageIndex, imageList.size) {
        if (imageList.size > 1) {
            delay(autoScrollDelay)
            currentImageIndex = (currentImageIndex + 1) % imageList.size
        }
    }

    // Only show slideshow if we have more than one image
    if (imageList.size > 1) {
        Box(
            modifier = modifier.clip(RoundedCornerShape(16.dp))
        ) {
            // Current image with crossfade animation
            AnimatedContent(
                targetState = currentImageIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(800)) togetherWith
                            fadeOut(animationSpec = tween(800))
                },
                label = "image_transition"
            ) { imageIndex ->
                AsyncImage(
                    model = imageList[imageIndex],
                    contentDescription = "Recipe image ${imageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentImageIndex + 1).toFloat() / imageList.size },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
    } else {
        // Single image display (fallback)
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun RecipeDetailContent(recipe: DetailedRecipe) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recipe Image Slideshow
        item {
            SimpleRecipeSlideshow(
                recipe = recipe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }

        // Recipe Basic Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = recipe.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoChip(
                            icon = "🍽️",
                            label = "Category",
                            value = recipe.category
                        )
                        InfoChip(
                            icon = "🌍",
                            label = "Cuisine",
                            value = recipe.area
                        )
                    }

                    if (recipe.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tags:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = recipe.tags.joinToString(" • "),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Ingredients Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🥘 Ingredients",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Ingredient Items
        items(recipe.ingredients) { ingredient ->
            IngredientCard(ingredient = ingredient)
        }

        // Instructions Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "👨‍🍳 Instructions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = recipe.instructions,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Video Section (if available)
        if (!recipe.youtubeUrl.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📹 Watch Tutorial",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.youtubeUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Watch on YouTube")
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoChip(
    icon: String,
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun IngredientCard(ingredient: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = ingredient,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading recipe details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unable to load recipe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

// Preview Functions
@Preview(showBackground = true)
@Composable
fun RecipeDetailContentPreview() {
    RecipeTheme {
        val sampleRecipe = DetailedRecipe(
            id = "1",
            name = "Spaghetti Carbonara",
            category = "Pasta",
            area = "Italian",
            instructions = "1. Cook spaghetti according to package instructions. 2. In a large pan, cook pancetta until crispy. 3. Beat eggs and mix with grated Parmesan cheese. 4. Drain pasta and add to pan with pancetta. 5. Remove from heat and quickly stir in egg mixture. 6. Season with black pepper and serve immediately.",
            imageUrl = "https://example.com/carbonara.jpg",
            youtubeUrl = "https://youtube.com/watch?v=example",
            ingredients = listOf(
                "400g spaghetti",
                "200g pancetta, diced",
                "4 large eggs",
                "100g Parmesan cheese, grated",
                "Black pepper to taste",
                "Salt for pasta water"
            ),
            tags = listOf("Quick", "Easy", "Classic"),
            cookingTime = "30 minutes",
            description = "A classic Italian pasta dish with eggs, cheese, and pancetta"
        )

        RecipeDetailContent(recipe = sampleRecipe)
    }
}

@Preview(showBackground = true)
@Composable
fun InfoChipPreview() {
    RecipeTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            InfoChip(
                icon = "🍽️",
                label = "Category",
                value = "Pasta"
            )
            InfoChip(
                icon = "🌍",
                label = "Cuisine",
                value = "Italian"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IngredientCardPreview() {
    RecipeTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            IngredientCard(ingredient = "400g spaghetti")
            IngredientCard(ingredient = "200g pancetta, diced")
            IngredientCard(ingredient = "4 large eggs")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingContentPreview() {
    RecipeTheme {
        LoadingContent()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorContentPreview() {
    RecipeTheme {
        ErrorContent(
            error = "Network connection failed. Please check your internet connection and try again.",
            onRetry = { },
            onDismiss = { }
        )
    }
}

// Utility function to get all favorite recipes (for a favorites screen)
fun getAllFavoriteRecipes(context: Context): List<FavoriteRecipeItem> {
    val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
    val allPrefs = prefs.all
    val favorites = mutableListOf<FavoriteRecipeItem>()

    val recipeIds = allPrefs.keys.filter { !it.contains("_") && allPrefs[it] == true }

    recipeIds.forEach { recipeId ->
        val name = prefs.getString("${recipeId}_name", "") ?: ""
        val imageUrl = prefs.getString("${recipeId}_image", "") ?: ""
        val category = prefs.getString("${recipeId}_category", "") ?: ""
        val area = prefs.getString("${recipeId}_area", "") ?: ""
        val dateAdded = prefs.getLong("${recipeId}_date_added", 0L)

        if (name.isNotEmpty()) {
            favorites.add(
                FavoriteRecipeItem(
                    id = recipeId,
                    name = name,
                    imageUrl = imageUrl,
                    category = category,
                    area = area,
                    dateAdded = dateAdded
                )
            )
        }
    }

    return favorites.sortedByDescending { it.dateAdded }
}

// Data class for favorite items
data class FavoriteRecipeItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val area: String,
    val dateAdded: Long
)