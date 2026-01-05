package ke.nucho.recipe.ui.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ke.nucho.recipe.R
import coil.compose.AsyncImage
import ke.nucho.recipe.data.DetailedRecipe
import ke.nucho.recipe.ui.theme.RecipeTheme
import ke.nucho.recipe.viewmodel.RecipeDetailViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import ke.nucho.recipe.BannerAdView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import ke.nucho.recipe.AdConstants

class RecipeDetailActivity : ComponentActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false

    companion object {
        private const val TAG = "RecipeDetailActivity"
        private val AD_UNIT_ID = AdConstants.getInterstitialId()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadInterstitialAd()

        val recipeId = intent.getStringExtra("RECIPE_ID") ?: ""
        val recipeName = intent.getStringExtra("RECIPE_NAME") ?: "Recipe"

        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            RecipeDetailScreen(
                                recipeId = recipeId,
                                recipeName = recipeName,
                                onBackClick = {
                                    showAdAndNavigate {
                                        finish()
                                    }
                                }
                            )
                        }
                        BannerAdView(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        if (isLoadingAd || interstitialAd != null) {
            return
        }

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isLoadingAd = false
                }
            }
        )
    }

    private fun showAdAndNavigate(onComplete: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    interstitialAd = null
                    loadInterstitialAd()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }

            interstitialAd?.show(this)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            loadInterstitialAd()
            onComplete()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        showAdAndNavigate {
            super.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    recipeName: String,
    viewModel: RecipeDetailViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var isFavorite by remember { mutableStateOf(false) }
    var showNutritionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
        isFavorite = prefs.getBoolean(recipeId, false)
        viewModel.loadRecipeDetails(recipeId)
    }

    fun toggleFavorite() {
        val prefs = context.getSharedPreferences("recipe_favorites", Context.MODE_PRIVATE)
        val newState = !isFavorite
        prefs.edit().putBoolean(recipeId, newState).apply()

        if (newState && uiState.recipe != null) {
            val recipe = uiState.recipe
            prefs.edit()
                .putString("${recipeId}_name", recipe.name)
                .putString("${recipeId}_image", recipe.imageUrl)
                .putString("${recipeId}_category", recipe.category)
                .putString("${recipeId}_area", recipe.area)
                .putString("${recipeId}_description", recipe.description)
                .putString("${recipeId}_cookingTime", recipe.cookingTime)
                .putLong("${recipeId}_date_added", System.currentTimeMillis())
                .apply()
        }
        isFavorite = newState
    }

    Scaffold(
        topBar = {
            EnhancedDetailTopBar(
                recipeName = recipeName,
                isFavorite = isFavorite,
                onBackClick = onBackClick,
                onFavoriteClick = { toggleFavorite() },
                onShareClick = { shareRecipe(context, uiState.recipe) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> EnhancedLoadingContent()
                uiState.error != null -> EnhancedErrorContent(
                    error = uiState.error,
                    onRetry = { viewModel.loadRecipeDetails(recipeId) },
                    onDismiss = { viewModel.clearError() }
                )
                uiState.recipe != null -> EnhancedRecipeContent(
                    recipe = uiState.recipe,
                    onNutritionClick = { showNutritionSheet = true }
                )
            }
        }

        if (showNutritionSheet) {
            NutritionSheet(
                recipe = uiState.recipe,
                onDismiss = { showNutritionSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDetailTopBar(
    recipeName: String,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = recipeName,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            IconButton(onClick = onFavoriteClick) {
                AnimatedContent(
                    targetState = isFavorite,
                    transitionSpec = {
                        scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                    },
                    label = "favorite_animation"
                ) { favorite ->
                    Icon(
                        imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (favorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun EnhancedRecipeContent(
    recipe: DetailedRecipe,
    onNutritionClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EnhancedRecipeHeroImage(recipe = recipe) }
        item { RecipeInfoCard(recipe = recipe) }
        item { QuickStatsRow(recipe = recipe) }

        if (recipe.tags.isNotEmpty()) {
            item { TagsSection(tags = recipe.tags) }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ingredients") },
                    icon = {
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Instructions") },
                    icon = {
                        Icon(
                            Icons.Default.List,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                // COMMENTED OUT: YouTube feature - will be re-enabled after publishing
                /*
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Video") },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.youtube),
                            contentDescription = "YouTube",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                */
            }
        }
        when (selectedTab) {
            0 -> {
                item {
                    Text(
                        text = "${recipe.ingredients.size} Ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                itemsIndexed(recipe.ingredients) { index, ingredient ->
                    AnimatedIngredientCard(ingredient = ingredient, index = index)
                }
            }
            1 -> {
                item { InstructionsSection(instructions = recipe.instructions) }
            }
            // COMMENTED OUT: YouTube feature - will be re-enabled after publishing
            /*
            2 -> {
                item { YouTubeSection(recipeName = recipe.name) }
            }
            */
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// COMMENTED OUT: YouTube feature - will be re-enabled after publishing
/*
@Composable
fun YouTubeSection(recipeName: String) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFF0000),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = "Watch Video Tutorial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Learn how to make \"$recipeName\" with step-by-step video tutorials on YouTube",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose Your Platform:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                YouTubeSearchButton(
                    icon = Icons.Default.PlayArrow,
                    title = "Search on YouTube",
                    subtitle = "Find recipe videos",
                    color = Color(0xFFFF0000),
                    onClick = {
                        val query = "$recipeName recipe how to make"
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                        )
                        context.startActivity(intent)
                    }
                )

                YouTubeSearchButton(
                    icon = Icons.Default.Star,
                    title = "Open in YouTube App",
                    subtitle = "Better viewing experience",
                    color = Color(0xFFFF0000),
                    onClick = {
                        val query = "$recipeName recipe tutorial"
                        try {
                            val intent = Intent(Intent.ACTION_SEARCH).apply {
                                setPackage("com.google.android.youtube")
                                putExtra("query", query)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                            )
                            context.startActivity(webIntent)
                        }
                    }
                )

                YouTubeSearchButton(
                    icon = Icons.Default.Search,
                    title = "Search on Google",
                    subtitle = "Find more video tutorials",
                    color = Color(0xFF4285F4),
                    onClick = {
                        val query = "$recipeName recipe video tutorial"
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "We'll search YouTube for video tutorials of this recipe for you!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeSearchButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
*/

@Composable
fun EnhancedRecipeHeroImage(recipe: DetailedRecipe) {
    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 300f
                    )
                )
            )
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            )
        }
    }
}

@Composable
fun RecipeInfoCard(recipe: DetailedRecipe) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBadge(icon = Icons.Default.Star, label = "Category", value = recipe.category)
                InfoBadge(icon = Icons.Default.Place, label = "Cuisine", value = recipe.area)
            }
        }
    }
}

@Composable
fun InfoBadge(icon: ImageVector, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickStatsRow(recipe: DetailedRecipe) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.DateRange,
            value = recipe.cookingTime,
            label = "Cook Time",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.ShoppingCart,
            value = "${recipe.ingredients.size}",
            label = "Ingredients",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TagsSection(tags: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = { },
                    label = { Text(tag) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
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
fun AnimatedIngredientCard(ingredient: String, index: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(8.dp)
                ) {}
                Text(
                    text = ingredient,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InstructionsSection(instructions: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Cooking Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Divider()
            val steps = instructions.split(Regex("\\n\\d+\\.\\s*|\\n\\n+")).filter { it.isNotBlank() }
            if (steps.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    steps.forEachIndexed { index, step ->
                        InstructionStep(stepNumber = index + 1, instruction = step.trim())
                    }
                }
            } else {
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun InstructionStep(stepNumber: Int, instruction: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionSheet(recipe: DetailedRecipe?, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Nutrition Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Detailed nutrition data coming soon!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EnhancedLoadingContent() {
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
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading delicious details...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EnhancedErrorContent(error: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("Dismiss") }
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

private fun shareRecipe(context: Context, recipe: DetailedRecipe?) {
    recipe?.let {
        val shareText = buildString {
            appendLine("🍽️ ${it.name}")
            appendLine()
            appendLine("📍 ${it.category} • ${it.area} Cuisine")
            appendLine("⏱️ ${it.cookingTime}")
            appendLine()
            if (it.tags.isNotEmpty()) {
                appendLine("🏷️ ${it.tags.joinToString(", ")}")
                appendLine()
            }
            appendLine("📝 Ingredients:")
            it.ingredients.forEach { ing ->
                appendLine("• $ing")
            }
            appendLine()
            appendLine("👨‍🍳 Instructions:")
            appendLine(it.instructions)
            appendLine()
            appendLine("🎥 Watch on YouTube:")
            appendLine("https://www.youtube.com/results?search_query=${Uri.encode("${it.name} recipe")}")
        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${it.name}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Recipe"))
    }
}

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

@Preview(showBackground = true)
@Composable
fun EnhancedRecipeContentPreview() {
    RecipeTheme {
        EnhancedRecipeContent(
            recipe = DetailedRecipe(
                id = "1",
                name = "Spaghetti Carbonara",
                category = "Pasta",
                area = "Italian",
                instructions = "1. Cook spaghetti. 2. Prepare sauce. 3. Mix together. 4. Serve hot.",
                imageUrl = "",
                youtubeUrl = null,
                ingredients = listOf(
                    "400g spaghetti",
                    "200g pancetta",
                    "4 eggs",
                    "100g Parmesan"
                ),
                tags = listOf("Quick", "Easy", "Italian"),
                cookingTime = "30 mins",
                description = "Main Course • Italian cuisine"
            ),
            onNutritionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoBadgePreview() {
    RecipeTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBadge(Icons.Default.Star, "Category", "Dessert")
            InfoBadge(Icons.Default.Place, "Cuisine", "Italian")
        }
    }
}

// COMMENTED OUT: YouTube preview - will be re-enabled after publishing
/*
@Preview(showBackground = true)
@Composable
fun YouTubeSectionPreview() {
    RecipeTheme {
        YouTubeSection(recipeName = "Spaghetti Carbonara")
    }
}
*/