package com.example.recipe.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipe.BannerAdView
import com.example.recipe.MainActivity
import com.example.recipe.R
import com.example.recipe.ui.theme.RecipeTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import com.example.recipe.AdConstants
class GetStartedActivity : ComponentActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false

    companion object {
        private const val TAG = "GetStartedActivity"
        // Replace with your actual AdMob Interstitial Ad Unit ID
        private  val AD_UNIT_ID = AdConstants.getInterstitialId() // Test Ad Unit ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the interstitial ad
        loadInterstitialAd()

        setContent {
            RecipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ✨ Main content with banner ad at bottom
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Main screen content
                        Box(modifier = Modifier.weight(1f)) {
                            ProfessionalGetStartedScreen(
                                onBackPressed = {
                                    showAdAndNavigate {
                                        val intent = Intent(this@GetStartedActivity, WelcomeActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                },
                                onStartExploring = {
                                    showAdAndNavigate {
                                        val intent = Intent(this@GetStartedActivity, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            )
                        }

                        // ✨ Banner Ad at the bottom
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
                    loadInterstitialAd() // Load next ad
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

data class OnboardingFeature(
    val icon: ImageVector? = null,
    val iconDrawable: Int? = null,
    val title: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalGetStartedScreen(
    onBackPressed: () -> Unit = {},
    onStartExploring: () -> Unit = {}
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val features = remember {
        listOf(
            OnboardingFeature(
                icon = Icons.Default.Search,
                title = "Discover Recipes",
                description = "Browse thousands of recipes from cuisines worldwide powered by TheMealDB API",
                color = Color(0xFF6B4EFF)
            ),
            OnboardingFeature(
                icon = Icons.Default.Favorite,
                title = "Save Favorites",
                description = "Bookmark your favorite recipes and access them anytime, anywhere",
                color = Color(0xFFFF4E6B)
            ),
            OnboardingFeature(
                icon = Icons.Default.Star,
                title = "Smart Filters",
                description = "Filter by diet, cuisine, ingredients, and cooking time to find perfect recipes",
                color = Color(0xFFFFB84E)
            ),
            OnboardingFeature(
                icon = Icons.Default.Info,
                title = "Detailed Instructions",
                description = "Step-by-step cooking instructions with ingredients and nutritional information",
                color = Color(0xFF4EFFB8)
            ),
            OnboardingFeature(
                iconDrawable = R.drawable.youtube,
                title = "Watch Video Tutorials",
                description = "Learn cooking techniques with step-by-step video guides from YouTube",
                color = Color(0xFFFF0000)
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hero Section with Image
                        HeroSection()

                        Spacer(modifier = Modifier.height(32.dp))

                        // Welcome Text
                        Text(
                            text = "Let's Get Cooking!",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Everything you need to discover and create amazing dishes",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                    }

                    // Features List
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            features.forEachIndexed { index, feature ->
                                AnimatedFeatureCard(
                                    feature = feature,
                                    animationDelay = (index * 100L)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Stats Row
                        StatsRow()

                        Spacer(modifier = Modifier.height(40.dp))

                        // Continue Button
                        Button(
                            onClick = onStartExploring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = "Start Exploring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                    }
                }
            }
        }
    }
}

@Composable
fun AutoSlideshow(
    images: List<Int>,
    modifier: Modifier = Modifier,
    intervalMillis: Long = 2000
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(intervalMillis)
            currentIndex = (currentIndex + 1) % images.size
        }
    }

    Crossfade(
        targetState = currentIndex,
        label = "Image Crossfade",
        animationSpec = tween(durationMillis = 800)
    ) { index ->
        Image(
            painter = painterResource(id = images[index]),
            contentDescription = "Slideshow Image",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun HeroSection() {
    val images = listOf(
        R.drawable.book001,
        R.drawable.chef001,
        R.drawable.food002,
        R.drawable.book002,
        R.drawable.book003,
        R.drawable.chef002,
        R.drawable.food003,
        R.drawable.food004,
        R.drawable.youtube
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Auto-playing slideshow background
            AutoSlideshow(
                images = images,
                modifier = Modifier.fillMaxSize(),
                intervalMillis = 2000
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 300f
                        )
                    )
            )

            // Floating badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Premium Features",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Bottom text overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = "10,000+ Recipes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "From cuisines worldwide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AnimatedFeatureCard(
    feature: OnboardingFeature,
    animationDelay: Long = 0L
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec = tween(500)
                )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with colored background
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = feature.color.copy(alpha = 0.15f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (feature.iconDrawable != null) {
                            Image(
                                painter = painterResource(id = feature.iconDrawable),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else if (feature.icon != null) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = feature.color
                            )
                        }
                    }
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Default.Star,
            value = "10K+",
            label = "Recipes",
            color = Color(0xFFFFB84E)
        )

        StatItem(
            icon = Icons.Default.Person,
            value = "5K+",
            label = "Users",
            color = Color(0xFF6B4EFF)
        )

        StatItem(
            icon = Icons.Default.Favorite,
            value = "4.8",
            label = "Rating",
            color = Color(0xFFFF4E6B)
        )

        StatItem(
            icon = Icons.Default.PlayArrow,
            value = "1K+",
            label = "Videos",
            color = Color(0xFFFF0000)
        )
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
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

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Previews
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GetStartedScreenPreview() {
    RecipeTheme {
        ProfessionalGetStartedScreen()
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun HeroSectionPreview() {
    RecipeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            HeroSection()
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun FeatureCardsPreview() {
    RecipeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedFeatureCard(
                feature = OnboardingFeature(
                    icon = Icons.Default.Search,
                    title = "Discover Recipes",
                    description = "Browse thousands of recipes from cuisines worldwide powered by the Spoonacular API.",
                    color = Color(0xFF6B4EFF)
                )
            )

            AnimatedFeatureCard(
                feature = OnboardingFeature(
                    icon = Icons.Default.Favorite,
                    title = "Save Favorites",
                    description = "Bookmark your favorite recipes and access them anytime, anywhere",
                    color = Color(0xFFFF4E6B)
                )
            )

            AnimatedFeatureCard(
                feature = OnboardingFeature(
                    icon = Icons.Default.Star,
                    title = "Smart Filters",
                    description = "Filter by diet, cuisine, ingredients, and cooking time to find perfect recipes",
                    color = Color(0xFFFFB84E)
                )
            )

            AnimatedFeatureCard(
                feature = OnboardingFeature(
                    iconDrawable = R.drawable.youtube,
                    title = "Watch Video Tutorials",
                    description = "Learn cooking techniques with step-by-step video guides from YouTube",
                    color = Color(0xFFFF0000)
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun StatsRowPreview() {
    RecipeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            StatsRow()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatItemPreview() {
    RecipeTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                icon = Icons.Default.Star,
                value = "10K+",
                label = "Recipes",
                color = Color(0xFFFFB84E)
            )

            StatItem(
                icon = Icons.Default.PlayArrow,
                value = "1K+",
                label = "Videos",
                color = Color(0xFFFF0000)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun SingleFeatureCardPreview() {
    RecipeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AnimatedFeatureCard(
                feature = OnboardingFeature(
                    iconDrawable = R.drawable.youtube,
                    title = "Watch Video Tutorials",
                    description = "Learn cooking techniques with step-by-step video guides from YouTube",
                    color = Color(0xFFFF0000)
                )
            )
        }
    }
}