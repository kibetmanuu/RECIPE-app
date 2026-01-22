package ke.nucho.recipe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ke.nucho.recipe.data.Recipe
import ke.nucho.recipe.ui.activities.GetStartedActivity
import ke.nucho.recipe.ui.activities.RecipeDetailActivity
import ke.nucho.recipe.ui.activities.FavoriteActivity
import ke.nucho.recipe.ui.theme.RecipeTheme
import ke.nucho.recipe.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.lifecycleScope
import ke.nucho.recipe.config.RemoteConfigManager
import ke.nucho.recipe.analytics.ApiUsageTracker
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.crashlytics.FirebaseCrashlytics
class MainActivity : ComponentActivity() {

    // ✨ NEW FUNCTION - Generate or retrieve user ID
    private fun getUserId(): String {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)

        if (userId == null) {
            // Create new user ID
            userId = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", userId).apply()
            Log.d("MainActivity", "✨ New user ID created: $userId")
        } else {
            Log.d("MainActivity", "👤 Existing user ID loaded: $userId")
        }

        return userId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Remote Config and track active user
        lifecycleScope.launch {
            // Initialize Remote Config
            RemoteConfigManager.initialize()

            // ✨ NEW - Track this user as active
            val userId = getUserId()
            ApiUsageTracker.trackActiveUser(userId)
            Log.d("MainActivity", "✅ User tracked: $userId")

            // Force refresh config every time app opens
            val updated = RemoteConfigManager.forceRefresh()
            if (updated) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Configuration updated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Optional: Start periodic refresh (every 5 minutes)
        startPeriodicRefresh()

        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onBackPressed = {
                            // Exit app instead of going to onboarding
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun startPeriodicRefresh() {
        lifecycleScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes
                try {
                    val updated = RemoteConfigManager.forceRefresh()
                    if (updated) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Recipes refreshed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to refresh config: ${e.message}")
                }
            }
        }
    }
    // Handle back press - exit app instead of going to onboarding
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Exit the app instead of going back to onboarding
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}

// Navigation items
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Favorites : BottomNavItem("favorites", Icons.Default.Favorite, "Favorites")
    object Search : BottomNavItem("search", Icons.Default.Search, "Search")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

// Filter data classes
data class FilterChip(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val type: FilterType
)

enum class FilterType {
    DIET, CUISINE, TYPE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var selectedNavItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 || searchQuery.isEmpty()) {
            delay(500)
            viewModel.searchRecipes(searchQuery)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient
        AnimatedVisibility(
            visible = !isSearchExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (!isSearchExpanded) {
                    EnhancedTopBar(
                        onBackClick = onBackPressed, // Use the callback parameter
                        onRefreshClick = { viewModel.refreshRecipes() },
                        onFilterClick = { showFilterSheet = true }
                    )
                }
            },
            bottomBar = {
                if (!isSearchExpanded) {
                    // Column to stack Banner Ad + Navigation Bar
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // AdMob Banner Ad
                        BannerAdView(
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Navigation Bar
                        ModernBottomBar(
                            selectedItem = selectedNavItem,
                            onItemSelected = { item ->
                                selectedNavItem = item
                                when (item) {
                                    is BottomNavItem.Home -> {
                                        // Already on home
                                    }
                                    is BottomNavItem.Favorites -> {
                                        val intent = Intent(context, FavoriteActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                    is BottomNavItem.Search -> {
                                        isSearchExpanded = true
                                    }
                                    is BottomNavItem.Profile -> {
                                        showProfileSheet = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            // Full Screen Search Mode
            if (isSearchExpanded) {
                FullScreenSearchMode(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onBack = {
                        searchQuery = ""
                        isSearchExpanded = false
                    },
                    isSearching = uiState.isLoading && searchQuery.isNotBlank(),
                    recipes = if (searchQuery.length >= 2) uiState.recipes else emptyList(),
                    showResults = searchQuery.length >= 2
                )
            } else {
                // Normal Home Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Welcome Header


                    // Collapsed Search Bar - Clickable
                    CollapsedSearchBar(
                        onClick = { isSearchExpanded = true }
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
                            searchQuery = searchQuery,
                            currentFilter = uiState.currentFilter,
                            onFilterClick = { filter ->
                                when (filter.type) {
                                    FilterType.DIET -> viewModel.filterByDiet(filter.value)
                                    FilterType.CUISINE -> viewModel.filterByCuisine(filter.value)
                                    FilterType.TYPE -> viewModel.filterByCategory(filter.value)
                                }
                            },
                            onClearFilter = { viewModel.clearFilter() }
                        )
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            AdvancedFilterSheet(
                onDismiss = { showFilterSheet = false },
                onApplyFilter = { filter ->
                    when (filter.type) {
                        FilterType.DIET -> viewModel.filterByDiet(filter.value)
                        FilterType.CUISINE -> viewModel.filterByCuisine(filter.value)
                        FilterType.TYPE -> viewModel.filterByCategory(filter.value)
                    }
                    showFilterSheet = false
                }
            )
        }

        // Profile Bottom Sheet
        if (showProfileSheet) {
            ProfileSheet(
                onDismiss = { showProfileSheet = false }
            )
        }
    }
}

@Composable
fun ModernBottomBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Favorites,
        BottomNavItem.Search,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Special treatment for Profile icon
                        if (item is BottomNavItem.Profile) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedItem == item) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selectedItem == item) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedItem == item) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Avatar
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Guest User",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Explore delicious recipes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(
                    icon = Icons.Default.Favorite,
                    value = "0",
                    label = "Favorites",
                    color = Color.Red
                )
                ProfileStat(
                    icon = Icons.Default.Star,
                    value = "0",
                    label = "Recipes",
                    color = MaterialTheme.colorScheme.primary
                )
                ProfileStat(
                    icon = Icons.Default.DateRange,
                    value = "New",
                    label = "Member",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Options
            ProfileOption(
                icon = Icons.Default.Favorite,
                title = "My Favorites",
                subtitle = "View saved recipes",
                onClick = {
                    val intent = Intent(context, FavoriteActivity::class.java)
                    context.startActivity(intent)
                    onDismiss()
                }
            )

            ProfileOption(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "App preferences",
                onClick = { /* TODO: Navigate to settings */ }
            )

            ProfileOption(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Learn more about the app",
                onClick = { /* TODO: Show about dialog */ }
            )

            ProfileOption(
                icon = Icons.Default.ExitToApp,
                title = "Exit",
                subtitle = "Close the app",
                onClick = {
                    // Exit the app
                    (context as? ComponentActivity)?.finish()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileStat(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopBar(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Recipe Discovery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Powered by Spoonacular API",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Filters",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun FullScreenSearchMode(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    isSearching: Boolean,
    recipes: List<Recipe>,
    showResults: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Search Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back Button
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Search TextField
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = "Search recipes...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(28.dp)
                    )
                }
            }

            // Search Content
            when {
                // Show search results
                showResults && recipes.isNotEmpty() -> {
                    SearchResults(
                        query = query,
                        recipes = recipes,
                        onRecipeClick = { recipe ->
                            val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                                putExtra("RECIPE_ID", recipe.id)
                                putExtra("RECIPE_NAME", recipe.name)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                // Show loading
                showResults && isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "Searching for \"$query\"...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Show no results
                showResults && recipes.isEmpty() && !isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "🔍",
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 72.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try searching for different keywords",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Show suggestions
                else -> {
                    SearchSuggestions(
                        onSuggestionClick = { suggestion ->
                            onQueryChange(suggestion)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResults(
    query: String,
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Results header
        item {
            Text(
                text = "Results for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Recipe results
        items(recipes, key = { it.id }) { recipe ->
            SearchRecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe) },
                query = query
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    query: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recipe Image
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(12.dp)
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
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🍽️",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            // Recipe Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (recipe.category.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = recipe.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (recipe.area.isNotBlank()) {
                            Text(
                                text = "• ${recipe.area}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (recipe.cookingTime.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = recipe.cookingTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CollapsedSearchBar(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = "Search delicious recipes...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SearchSuggestions(
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = remember {
        listOf(
            "🍝 Pasta" to "pasta",
            "🍕 Pizza" to "pizza",
            "🥗 Salad" to "salad",
            "🍰 Dessert" to "dessert",
            "🍜 Soup" to "soup",
            "🥘 Curry" to "curry",
            "🍔 Burger" to "burger",
            "🌮 Tacos" to "tacos",
            "🍱 Sushi" to "sushi",
            "🥩 Steak" to "steak"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Popular Searches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(suggestions) { (display, search) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onSuggestionClick(search) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFilters(
    currentFilter: String,
    onFilterClick: (FilterChip) -> Unit,
    onClearFilter: () -> Unit
) {
    val filters = remember {
        listOf(
            FilterChip("🥗 Vegan", "vegan", Icons.Default.Star, FilterType.DIET),
            FilterChip("🌱 Vegetarian", "vegetarian", Icons.Default.Star, FilterType.DIET),
            FilterChip("🍝 Italian", "italian", Icons.Default.Place, FilterType.CUISINE),
            FilterChip("🌮 Mexican", "mexican", Icons.Default.Place, FilterType.CUISINE),
            FilterChip("🍜 Asian", "asian", Icons.Default.Place, FilterType.CUISINE),
            FilterChip("🍰 Dessert", "dessert", Icons.Default.ShoppingCart, FilterType.TYPE),
            FilterChip("🥞 Breakfast", "breakfast", Icons.Default.ShoppingCart, FilterType.TYPE)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 Quick Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (currentFilter.isNotEmpty()) {
                TextButton(
                    onClick = onClearFilter,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChipItem(
                    filter = filter,
                    isSelected = currentFilter.equals(filter.value, ignoreCase = true),
                    onClick = { onFilterClick(filter) }
                )
            }
        }
    }
}

@Composable
fun FilterChipItem(
    filter: FilterChip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = filter.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (isSelected) 4.dp else 0.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterSheet(
    onDismiss: () -> Unit,
    onApplyFilter: (FilterChip) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Advanced Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Diet Section
            FilterSection(
                title = "🥗 Dietary Preferences",
                icon = Icons.Default.Star,
                filters = listOf(
                    FilterChip("Vegan", "vegan", Icons.Default.Star, FilterType.DIET),
                    FilterChip("Vegetarian", "vegetarian", Icons.Default.Star, FilterType.DIET),
                    FilterChip("Gluten Free", "gluten free", Icons.Default.Star, FilterType.DIET),
                    FilterChip("Ketogenic", "ketogenic", Icons.Default.Star, FilterType.DIET),
                    FilterChip("Paleo", "paleo", Icons.Default.Star, FilterType.DIET)
                ),
                onFilterClick = { filter ->
                    onApplyFilter(filter)
                }
            )

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            // Cuisine Section
            FilterSection(
                title = "🌍 World Cuisines",
                icon = Icons.Default.Place,
                filters = listOf(
                    FilterChip("Italian", "italian", Icons.Default.Place, FilterType.CUISINE),
                    FilterChip("Mexican", "mexican", Icons.Default.Place, FilterType.CUISINE),
                    FilterChip("Chinese", "chinese", Icons.Default.Place, FilterType.CUISINE),
                    FilterChip("Indian", "indian", Icons.Default.Place, FilterType.CUISINE),
                    FilterChip("Thai", "thai", Icons.Default.Place, FilterType.CUISINE),
                    FilterChip("Japanese", "japanese", Icons.Default.Place, FilterType.CUISINE)
                ),
                onFilterClick = { filter ->
                    onApplyFilter(filter)
                }
            )

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            // Meal Type Section
            FilterSection(
                title = "🍽️ Meal Types",
                icon = Icons.Default.ShoppingCart,
                filters = listOf(
                    FilterChip("Breakfast", "breakfast", Icons.Default.ShoppingCart, FilterType.TYPE),
                    FilterChip("Lunch", "main course", Icons.Default.ShoppingCart, FilterType.TYPE),
                    FilterChip("Dinner", "main course", Icons.Default.ShoppingCart, FilterType.TYPE),
                    FilterChip("Dessert", "dessert", Icons.Default.ShoppingCart, FilterType.TYPE),
                    FilterChip("Snack", "snack", Icons.Default.ShoppingCart, FilterType.TYPE),
                    FilterChip("Appetizer", "appetizer", Icons.Default.ShoppingCart, FilterType.TYPE)
                ),
                onFilterClick = { filter ->
                    onApplyFilter(filter)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    icon: ImageVector,
    filters: List<FilterChip>,
    onFilterClick: (FilterChip) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                SuggestionChip(
                    onClick = { onFilterClick(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

    // Shimmer effect - moves from left to right
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image skeleton with shimmer
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = if (startAnimation) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                                start = Offset(shimmerTranslate - 200f, 0f),
                                end = Offset(shimmerTranslate, 200f)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text content skeleton with shimmer
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = if (startAnimation) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    start = Offset(shimmerTranslate - 200f, 0f),
                                    end = Offset(shimmerTranslate, 200f)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Second line skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = if (startAnimation) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    start = Offset(shimmerTranslate - 200f, 0f),
                                    end = Offset(shimmerTranslate, 200f)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags skeleton
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = if (startAnimation) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        start = Offset(shimmerTranslate - 200f, 0f),
                                        end = Offset(shimmerTranslate, 200f)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                    )

                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = if (startAnimation) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        start = Offset(shimmerTranslate - 200f, 0f),
                                        end = Offset(shimmerTranslate, 200f)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                    )
                }
            }

            // Arrow icon skeleton
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (startAnimation) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                ),
                                start = Offset(shimmerTranslate - 200f, 0f),
                                end = Offset(shimmerTranslate, 200f)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
fun RecipeContent(
    recipes: List<Recipe>,
    searchQuery: String,
    currentFilter: String,
    onFilterClick: (FilterChip) -> Unit,
    onClearFilter: () -> Unit
) {
    val listState = rememberLazyListState()

    // Track if we should show header elements based on scroll position
    val showHeaderElements by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Filters - Hides when scrolling
        AnimatedVisibility(
            visible = searchQuery.isEmpty() && showHeaderElements,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            QuickFilters(
                currentFilter = currentFilter,
                onFilterClick = onFilterClick,
                onClearFilter = onClearFilter
            )
        }

        // Animated visibility for the header card
        AnimatedVisibility(
            visible = showHeaderElements && recipes.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "Results for \"$searchQuery\""
                                currentFilter.isNotEmpty() -> "Filtered: ${currentFilter.replaceFirstChar { it.uppercase() }}"
                                else -> "✨ Discover Recipes"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (currentFilter.isNotEmpty()) {
                            Text(
                                text = "Showing filtered results",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (recipes.size >= 50) "50+ recipes" else "${recipes.size} recipes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (recipes.isEmpty()) {
            EmptyState(searchQuery = searchQuery, currentFilter = currentFilter)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    EnhancedRecipeCard(recipe = recipe)
                }

                // Bottom padding for bottom bar
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyState(searchQuery: String, currentFilter: String) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || currentFilter.isNotEmpty()) "🔍" else "🍽️",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "No recipes found"
                        currentFilter.isNotEmpty() -> "No recipes match this filter"
                        else -> "Start exploring recipes"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "Try different keywords or browse by category"
                        currentFilter.isNotEmpty() -> "Try a different filter or clear it"
                        else -> "Search for your favorite dishes or explore categories"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(context, FavoriteActivity::class.java)
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View My Favorites")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedRecipeCard(recipe: Recipe) {
    val context = LocalContext.current

    // Image zoom animation
    val infiniteTransition = rememberInfiniteTransition(label = "image_zoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra("RECIPE_ID", recipe.id)
                putExtra("RECIPE_NAME", recipe.name)
            }
            context.startActivity(intent)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recipe Image with gradient and zoom animation
                Card(
                    modifier = Modifier.size(126.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Image with scale animation
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = recipe.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f)
                                        ),
                                        startY = 200f
                                    )
                                )
                        )

                        if (recipe.imageUrl.isBlank()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🍽️",
                                            style = MaterialTheme.typography.displayLarge
                                        )
                                    }
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )

                    if (recipe.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cooking Time
                        if (recipe.cookingTime.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = recipe.cookingTime,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Category/Area tag
                        if (recipe.category.isNotBlank() || recipe.area.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = recipe.category.ifBlank { recipe.area },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Arrow Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// FlowRow composable for filter chips
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        content()
    }
}

// ==================== PREVIEW FUNCTIONS ====================

@Preview(name = "Main Screen", showBackground = true)
@Composable
fun MainScreenPreview() {
    RecipeTheme {
        MainScreen()
    }
}

@Preview(name = "Bottom Navigation Bar", showBackground = true)
@Composable
fun ModernBottomBarPreview() {
    RecipeTheme {
        ModernBottomBar(
            selectedItem = BottomNavItem.Home,
            onItemSelected = {}
        )
    }
}



@Preview(name = "Enhanced Recipe Card", showBackground = true)
@Composable
fun EnhancedRecipeCardPreview() {
    RecipeTheme {
        EnhancedRecipeCard(
            recipe = Recipe(
                id = "1",
                name = "Spaghetti Carbonara",
                description = "Main Course • Italian cuisine",
                imageUrl = "",
                cookingTime = "30 mins",
                category = "Main Course",
                area = "Italian"
            )
        )
    }
}