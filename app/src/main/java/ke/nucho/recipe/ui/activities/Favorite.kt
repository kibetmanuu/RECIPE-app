 package ke.nucho.recipe.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import ke.nucho.recipe.InterstitialAdHelper
import ke.nucho.recipe.ui.theme.RecipeTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import ke.nucho.recipe.AdConstants

class FavoriteActivity : ComponentActivity() {

    private lateinit var interstitialAdHelper: InterstitialAdHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Interstitial Ad
        interstitialAdHelper = InterstitialAdHelper(this)

        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FavoriteScreen(
                        onBackClick = {
                            // Show ad before going back
                            interstitialAdHelper.showAd {
                                finish()
                            }
                        },
                        onRecipeClick = { recipe ->
                            // Show ad before navigating to recipe detail
                            interstitialAdHelper.showAd {
                                val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                                    putExtra("RECIPE_ID", recipe.id)
                                    putExtra("RECIPE_NAME", recipe.name)
                                }
                                startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Show ad when user presses back button
        interstitialAdHelper.showAd {
            super.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onBackClick: () -> Unit = {},
    onRecipeClick: (FavoriteRecipeItem) -> Unit = {}
) {
    val context = LocalContext.current
    var favoriteRecipes by remember { mutableStateOf<List<FavoriteRecipeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var sortBy by remember { mutableStateOf(SortOption.DATE_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Load favorite recipes
    LaunchedEffect(Unit) {
        delay(300)
        favoriteRecipes = getAllFavoriteRecipes(context)
        isLoading = false
    }

    // Function to refresh favorites
    fun refreshFavorites() {
        favoriteRecipes = getAllFavoriteRecipes(context)
    }

    // Function to remove from favorites
    fun removeFromFavorites(recipeId: String) {
        val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove(recipeId)
            remove("${recipeId}_name")
            remove("${recipeId}_image")
            remove("${recipeId}_category")
            remove("${recipeId}_area")
            remove("${recipeId}_description")
            remove("${recipeId}_cookingTime")
            remove("${recipeId}_date_added")
        }.apply()
        refreshFavorites()
    }

    // Sort recipes
    val sortedRecipes = remember(favoriteRecipes, sortBy) {
        when (sortBy) {
            SortOption.DATE_ADDED -> favoriteRecipes.sortedByDescending { it.dateAdded }
            SortOption.NAME -> favoriteRecipes.sortedBy { it.name }
            SortOption.CATEGORY -> favoriteRecipes.sortedBy { it.category }
            SortOption.AREA -> favoriteRecipes.sortedBy { it.area }
        }
    }

    Scaffold(
        topBar = {
            FavoriteTopBar(
                favoriteCount = favoriteRecipes.size,
                onBackClick = onBackClick,
                onSortClick = { showSortMenu = true }
            )
        },
        bottomBar = {
            // Banner Ad at the bottom
            BannerAdView(adUnitId = AdConstants.getBannerId()) // Test Ad Unit ID
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                isLoading -> {
                    LoadingFavoritesContent()
                }
                favoriteRecipes.isEmpty() -> {
                    EmptyFavoritesContent()
                }
                else -> {
                    FavoriteRecipesContent(
                        favoriteRecipes = sortedRecipes,
                        onRecipeClick = onRecipeClick,
                        onRemoveFromFavorites = { recipeId ->
                            removeFromFavorites(recipeId)
                        }
                    )
                }
            }
        }

        if (showSortMenu) {
            SortMenuDialog(
                currentSort = sortBy,
                onSortSelected = {
                    sortBy = it
                    showSortMenu = false
                },
                onDismiss = { showSortMenu = false }
            )
        }
    }
}

@Composable
fun BannerAdView(adUnitId: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteTopBar(
    favoriteCount: Int,
    onBackClick: () -> Unit,
    onSortClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "My Favorites",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (favoriteCount > 0) {
                        Text(
                            text = "$favoriteCount ${if (favoriteCount == 1) "recipe" else "recipes"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (favoriteCount > 0) {
                IconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Sort"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun FavoriteRecipesContent(
    favoriteRecipes: List<FavoriteRecipeItem>,
    onRecipeClick: (FavoriteRecipeItem) -> Unit,
    onRemoveFromFavorites: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FavoriteStatsCard(favoriteRecipes = favoriteRecipes)
        }

        // Add banner ad after stats card (every 5 items)
        itemsIndexed(
            items = favoriteRecipes,
            key = { _, recipe -> recipe.id }
        ) { index, recipe ->
            AnimatedFavoriteCard(
                recipe = recipe,
                index = index,
                onClick = { onRecipeClick(recipe) },
                onRemoveFromFavorites = { onRemoveFromFavorites(recipe.id) }
            )

            // Add banner ad every 5 items
            if ((index + 1) % 5 == 0 && index < favoriteRecipes.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                BannerAdView(adUnitId = AdConstants.getBannerId())
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FavoriteStatsCard(favoriteRecipes: List<FavoriteRecipeItem>) {
    val categories = favoriteRecipes.map { it.category }.distinct().size
    val cuisines = favoriteRecipes.map { it.area }.distinct().size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsItem(
                icon = Icons.Default.Favorite,
                value = favoriteRecipes.size.toString(),
                label = "Recipes",
                color = Color.Red
            )

            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )

            StatsItem(
                icon = Icons.Default.Star,
                value = categories.toString(),
                label = "Categories",
                color = MaterialTheme.colorScheme.primary
            )

            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )

            StatsItem(
                icon = Icons.Default.Place,
                value = cuisines.toString(),
                label = "Cuisines",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StatsItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AnimatedFavoriteCard(
    recipe: FavoriteRecipeItem,
    index: Int,
    onClick: () -> Unit,
    onRemoveFromFavorites: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        FavoriteRecipeCard(
            recipe = recipe,
            onClick = onClick,
            onRemoveFromFavorites = { showDeleteDialog = true }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Remove from Favorites?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "\"${recipe.name}\" will be removed from your favorites.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFromFavorites()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FavoriteRecipeCard(
    recipe: FavoriteRecipeItem,
    onClick: () -> Unit,
    onRemoveFromFavorites: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember(recipe.dateAdded) {
        if (recipe.dateAdded > 0) {
            dateFormatter.format(Date(recipe.dateAdded))
        } else {
            "Unknown date"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 150f
                                )
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Red.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = recipe.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (recipe.category.isNotEmpty()) {
                            CategoryChip(
                                label = recipe.category,
                                icon = Icons.Default.Star,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        if (recipe.area.isNotEmpty()) {
                            CategoryChip(
                                label = recipe.area,
                                icon = Icons.Default.Place,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = onRemoveFromFavorites,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from favorites",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: ImageVector,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LoadingFavoritesContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading your favorites...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyFavoritesContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = "No Favorites Yet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Start exploring delicious recipes and tap the ❤️ icon to save your favorites here!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tip: Build your personal cookbook!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortMenuDialog(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Sort By",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.values().forEach { option ->
                    SortOptionItem(
                        option = option,
                        isSelected = option == currentSort,
                        onClick = { onSortSelected(option) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SortOptionItem(
    option: SortOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) {
            BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = option.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class FavoriteRecipeItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val area: String,
    val dateAdded: Long = 0L
)

enum class SortOption(val label: String, val icon: ImageVector) {
    DATE_ADDED("Recently Added", Icons.Default.DateRange),
    NAME("Name (A-Z)", Icons.Default.Call),
    CATEGORY("Category", Icons.Default.Star),
    AREA("Cuisine", Icons.Default.Place)
}

fun getAllFavoriteRecipes(context: Context): List<FavoriteRecipeItem> {
    val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
    val allPrefs = prefs.all
    val favoriteRecipes = mutableListOf<FavoriteRecipeItem>()

    val favoriteIds = allPrefs.keys.filter { key ->
        !key.contains("_") && prefs.getBoolean(key, false)
    }

    favoriteIds.forEach { id ->
        val name = prefs.getString("${id}_name", "") ?: ""
        val imageUrl = prefs.getString("${id}_image", "") ?: ""
        val category = prefs.getString("${id}_category", "") ?: ""
        val area = prefs.getString("${id}_area", "") ?: ""
        val dateAdded = prefs.getLong("${id}_date_added", 0L)

        if (name.isNotEmpty()) {
            favoriteRecipes.add(
                FavoriteRecipeItem(
                    id = id,
                    name = name,
                    imageUrl = imageUrl,
                    category = category,
                    area = area,
                    dateAdded = dateAdded
                )
            )
        }
    }

    return favoriteRecipes
}

@Preview(showBackground = true)
@Composable
fun FavoriteRecipeCardPreview() {
    RecipeTheme {
        val sampleRecipe = FavoriteRecipeItem(
            id = "1",
            name = "Spaghetti Carbonara with Extra Cheese",
            imageUrl = "",
            category = "Pasta",
            area = "Italian",
            dateAdded = System.currentTimeMillis()
        )

        FavoriteRecipeCard(
            recipe = sampleRecipe,
            onClick = { },
            onRemoveFromFavorites = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FavoriteStatsCardPreview() {
    RecipeTheme {
        val sampleRecipes = listOf(
            FavoriteRecipeItem("1", "Recipe 1", "", "Pasta", "Italian", 0L),
            FavoriteRecipeItem("2", "Recipe 2", "", "Dessert", "French", 0L),
            FavoriteRecipeItem("3", "Recipe 3", "", "Pasta", "Italian", 0L)
        )
        FavoriteStatsCard(favoriteRecipes = sampleRecipes)
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyFavoritesContentPreview() {
    RecipeTheme {
        EmptyFavoritesContent()
    }
}