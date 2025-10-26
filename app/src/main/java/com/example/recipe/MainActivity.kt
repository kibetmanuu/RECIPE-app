package com.example.recipe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.recipe.data.Recipe
import com.example.recipe.ui.activities.GetStartedActivity
import com.example.recipe.ui.activities.RecipeDetailActivity
import com.example.recipe.ui.activities.FavoriteActivity
import com.example.recipe.ui.theme.RecipeTheme
import com.example.recipe.viewmodel.MainViewModel
import com.example.recipe.utils.TimeUtils
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// ==================== PREVIEW FUNCTIONS ====================

@Preview(name = "Main Screen - Light", showBackground = true)
@Composable
fun MainScreenPreview() {
    RecipeTheme {
        MainScreen()
    }
}

@Preview(name = "Search Bar - Empty", showBackground = true)
@Composable
fun SearchBarEmptyPreview() {
    RecipeTheme {
        SearchBar(
            query = "",
            onQueryChange = {},
            onClear = {},
            isSearching = false
        )
    }
}

@Preview(name = "Search Bar - With Text", showBackground = true)
@Composable
fun SearchBarWithTextPreview() {
    RecipeTheme {
        SearchBar(
            query = "Chicken pasta",
            onQueryChange = {},
            onClear = {},
            isSearching = false
        )
    }
}

@Preview(name = "Search Bar - Searching", showBackground = true)
@Composable
fun SearchBarSearchingPreview() {
    RecipeTheme {
        SearchBar(
            query = "Pizza",
            onQueryChange = {},
            onClear = {},
            isSearching = true
        )
    }
}

@Preview(name = "Loading Content", showBackground = true)
@Composable
fun LoadingContentPreview() {
    RecipeTheme {
        LoadingContent()
    }
}

@Preview(name = "Skeleton Card", showBackground = true)
@Composable
fun SkeletonRecipeCardPreview() {
    RecipeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonRecipeCard(delay = 0)
            SkeletonRecipeCard(delay = 100)
            SkeletonRecipeCard(delay = 200)
        }
    }
}

@Preview(name = "Error Content", showBackground = true)
@Composable
fun ErrorContentPreview() {
    RecipeTheme {
        ErrorContent(
            error = "Failed to load recipes. Please check your internet connection and try again.",
            onRetry = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Empty State - No Search", showBackground = true)
@Composable
fun EmptyStateNoSearchPreview() {
    RecipeTheme {
        EmptyState(searchQuery = "")
    }
}

@Preview(name = "Empty State - With Search", showBackground = true)
@Composable
fun EmptyStateWithSearchPreview() {
    RecipeTheme {
        EmptyState(searchQuery = "Nonexistent recipe")
    }
}

@Preview(name = "Recipe Card - Full Details", showBackground = true)
@Composable
fun RecipeCardFullPreview() {
    RecipeTheme {
        RecipeCard(
            recipe = Recipe(
                id = "1",
                name = "Spaghetti Carbonara",
                description = "A classic Italian pasta dish with eggs, cheese, and pancetta",
                imageUrl = "https://www.themealdb.com/images/media/meals/llcbn01574260722.jpg",
                cookingTime = "30 minutes"
            )
        )
    }
}

@Preview(name = "Recipe Card - No Image", showBackground = true)
@Composable
fun RecipeCardNoImagePreview() {
    RecipeTheme {
        RecipeCard(
            recipe = Recipe(
                id = "2",
                name = "Quick Caesar Salad",
                description = "Fresh romaine lettuce with parmesan cheese and croutons",
                imageUrl = "",
                cookingTime = "15 minutes"
            )
        )
    }
}

@Preview(name = "Recipe Card - Long Title", showBackground = true)
@Composable
fun RecipeCardLongTitlePreview() {
    RecipeTheme {
        RecipeCard(
            recipe = Recipe(
                id = "3",
                name = "Mediterranean Grilled Chicken with Roasted Vegetables and Herbs",
                description = "Juicy grilled chicken breast marinated in Mediterranean spices, served with a colorful array of roasted seasonal vegetables",
                imageUrl = "",
                cookingTime = "45 minutes"
            )
        )
    }
}

@Preview(name = "Recipe Card - Minimal Info", showBackground = true)
@Composable
fun RecipeCardMinimalPreview() {
    RecipeTheme {
        RecipeCard(
            recipe = Recipe(
                id = "4",
                name = "Beef Stew",
                description = "",
                imageUrl = "",
                cookingTime = "2 hours"
            )
        )
    }
}

@Preview(name = "Recipe List - Multiple Items", showBackground = true, heightDp = 800)
@Composable
fun RecipeContentMultiplePreview() {
    RecipeTheme {
        RecipeContent(
            recipes = listOf(
                Recipe(
                    id = "1",
                    name = "Beef Stroganoff",
                    description = "Tender beef strips in creamy mushroom sauce",
                    imageUrl = "https://www.themealdb.com/images/media/meals/svprys1511176755.jpg",
                    cookingTime = "40 minutes"
                ),
                Recipe(
                    id = "2",
                    name = "Vegetable Stir Fry",
                    description = "Quick and healthy mixed vegetables with soy sauce",
                    imageUrl = "",
                    cookingTime = "20 minutes"
                ),
                Recipe(
                    id = "3",
                    name = "Chicken Tikka Masala",
                    description = "Creamy and flavorful Indian curry",
                    imageUrl = "https://www.themealdb.com/images/media/meals/wyxwsp1486979827.jpg",
                    cookingTime = "50 minutes"
                )
            ),
            searchQuery = ""
        )
    }
}

@Preview(name = "Recipe List - Search Results", showBackground = true, heightDp = 600)
@Composable
fun RecipeContentSearchResultsPreview() {
    RecipeTheme {
        RecipeContent(
            recipes = listOf(
                Recipe(
                    id = "1",
                    name = "Chicken Curry",
                    description = "Spicy and aromatic chicken curry with basmati rice",
                    imageUrl = "https://www.themealdb.com/images/media/meals/wyxwsp1486979827.jpg",
                    cookingTime = "50 minutes"
                ),
                Recipe(
                    id = "2",
                    name = "Chicken Parmesan",
                    description = "Breaded chicken breast with marinara sauce and melted cheese",
                    imageUrl = "",
                    cookingTime = "35 minutes"
                )
            ),
            searchQuery = "chicken"
        )
    }
}

@Preview(name = "Recipe List - Empty", showBackground = true)
@Composable
fun RecipeContentEmptyPreview() {
    RecipeTheme {
        RecipeContent(
            recipes = emptyList(),
            searchQuery = "nonexistent"
        )
    }
}

@Preview(name = "Recipe Cards - Variety", showBackground = true, heightDp = 800)
@Composable
fun RecipeCardsVarietyPreview() {
    RecipeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecipeCard(
                recipe = Recipe(
                    id = "1",
                    name = "Quick Breakfast Bowl",
                    description = "Healthy morning meal ready in minutes",
                    imageUrl = "",
                    cookingTime = "10 minutes"
                )
            )

            RecipeCard(
                recipe = Recipe(
                    id = "2",
                    name = "Classic Margherita Pizza",
                    description = "Traditional Italian pizza with fresh mozzarella, tomatoes, and basil",
                    imageUrl = "https://www.themealdb.com/images/media/meals/x0lk931587671540.jpg",
                    cookingTime = "25 minutes"
                )
            )

            RecipeCard(
                recipe = Recipe(
                    id = "3",
                    name = "Slow-Cooked Lamb Shanks",
                    description = "Fall-off-the-bone tender lamb in rich red wine sauce",
                    imageUrl = "",
                    cookingTime = "3 hours"
                )
            )

            RecipeCard(
                recipe = Recipe(
                    id = "4",
                    name = "Thai Green Curry",
                    description = "Aromatic coconut curry with vegetables and tofu",
                    imageUrl = "https://www.themealdb.com/images/media/meals/sstssx1487349585.jpg",
                    cookingTime = "35 minutes"
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf("") }

    // Debounced search
    LaunchedEffect(searchQuery) {
        delay(300)
        viewModel.searchRecipes(searchQuery)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Professional Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Recipe Discovery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        val intent = Intent(context, GetStartedActivity::class.java)
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshRecipes() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }

                BadgedBox(
                    badge = {
                        // Optional: Add badge count if you track favorites count
                    }
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, FavoriteActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorites",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Modern Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" },
            isSearching = uiState.isLoading && searchQuery.isNotBlank()
        )

        // Content based on state
        when {
            uiState.isLoading -> LoadingContent()
            uiState.error != null -> ErrorContent(
                error = uiState.error,
                onRetry = { viewModel.refreshRecipes() },
                onDismiss = { viewModel.clearError() }
            )
            else -> RecipeContent(
                recipes = uiState.recipes,
                searchQuery = searchQuery
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            placeholder = {
                Text(
                    "Search recipes...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Skeleton loading cards
        repeat(5) { index ->
            SkeletonRecipeCard(
                modifier = Modifier.padding(bottom = 12.dp),
                delay = index * 100
            )
        }
    }
}

@Composable
fun SkeletonRecipeCard(modifier: Modifier = Modifier, delay: Int = 0) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        startAnimation = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (startAnimation) alpha else 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeContent(recipes: List<Recipe>, searchQuery: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Section Header
        if (recipes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Results for \"$searchQuery\""
                    } else {
                        "Discover Recipes"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${recipes.size} found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recipes.isEmpty()) {
            EmptyState(searchQuery = searchQuery)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe)
                }
            }
        }
    }
}

@Composable
fun EmptyState(searchQuery: String) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) "🔍" else "🍽️",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No recipes found"
                } else {
                    "Start exploring recipes"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotBlank()) {
                    "Try different keywords or check your favorites"
                } else {
                    "Search for your favorite dishes or ingredients"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = {
                    val intent = Intent(context, FavoriteActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Favorites")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(recipe: Recipe) {
    val context = LocalContext.current

    val cookingTime = remember(recipe) {
        TimeUtils.getSmartCookingTime(
            description = recipe.description,
            instructions = "",
            ingredientCount = 0,
            category = ""
        )
    }

    val difficulty = remember(recipe, cookingTime) {
        TimeUtils.getDifficultyIndicator(
            cookingTime = cookingTime,
            description = recipe.description,
            ingredientCount = 0
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra("RECIPE_ID", recipe.id)
                putExtra("RECIPE_NAME", recipe.name)
            }
            context.startActivity(intent)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recipe Image
            Card(
                modifier = Modifier
                    .size(108.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (recipe.imageUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🍽️",
                                    style = MaterialTheme.typography.displayMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Recipe Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (recipe.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cooking Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = cookingTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Difficulty Indicator
                    difficulty?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Favorite Icon
            IconButton(
                onClick = {
                    val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                        putExtra("RECIPE_ID", recipe.id)
                        putExtra("RECIPE_NAME", recipe.name)
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}