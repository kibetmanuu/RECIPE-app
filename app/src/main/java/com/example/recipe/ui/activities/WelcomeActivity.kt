package com.example.recipe.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipe.InterstitialAdHelper
import com.example.recipe.MainActivity
import com.example.recipe.R
import com.example.recipe.ui.theme.RecipeTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import com.example.recipe.AdConstants
class WelcomeActivity : ComponentActivity() {

    private lateinit var interstitialAdHelper: InterstitialAdHelper
    // ✨ App Open Ad will be managed by AppOpenAdManager in Application class

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
                    WelcomeScreenWithSplash(
                        onGetStartedClick = {
                            // Show interstitial ad before navigating to GetStartedActivity
                            interstitialAdHelper.showAd {
                                val intent = Intent(this, GetStartedActivity::class.java)
                                startActivity(intent)
                            }
                        },
                        onBrowseAsGuestClick = {
                            // Show interstitial ad before navigating to MainActivity
                            interstitialAdHelper.showAd {
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        // Show ad when user presses back button
        interstitialAdHelper.showAd {
            super.onBackPressed()
        }
    }
}

@Composable
fun WelcomeScreenWithSplash(
    onGetStartedClick: () -> Unit = {},
    onBrowseAsGuestClick: () -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = true) {
        delay(3000) // 3 seconds splash
        showSplash = false
        // ✨ App Open Ad will show automatically after splash via AppOpenAdManager
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(800))
        ) {
            WelcomeScreen(
                onGetStartedClick = onGetStartedClick,
                onBrowseAsGuestClick = onBrowseAsGuestClick
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            SplashScreen()
        }
    }
}

@Composable
fun SplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        delay(100)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFFF8E53),
                        Color(0xFFFFB380)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // App Logo with glow effect
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 20.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cheflogo),
                            contentDescription = "Recipe App Logo",
                            modifier = Modifier.size(120.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "RecipeHub",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Culinary Journey Starts Here",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit = {},
    onBrowseAsGuestClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(100)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF5F0),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Hero Section
            AnimatedScaleCard(startAnimation = startAnimation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Logo with gradient background
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFE5D9),
                                            Color(0xFFFFD4BF)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cheflogo),
                                contentDescription = "Recipe App Logo",
                                modifier = Modifier.size(110.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Welcome to RecipeHub",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3142),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Discover, Cook & Share Amazing Recipes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF6B35),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Explore thousands of delicious recipes from around the world. Save your favorites, create shopping lists, and become a master chef!",
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF666666),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights Section
            Text(
                text = "Why Choose RecipeHub?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3142),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature Cards - All in vertical layout
            FeatureHighlightWithDrawable(
                iconDrawable = R.drawable.youtube,
                title = "Watch Video Tutorials",
                description = "Learn cooking with step-by-step video guides from expert chefs",
                color = Color(0xFFFF0000),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureHighlightLarge(
                icon = Icons.Default.Search,
                title = "10,000+ Global Recipes",
                description = "Explore cuisines from around the world with our extensive recipe database",
                color = Color(0xFFFF6B35),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureHighlightLarge(
                icon = Icons.Default.Favorite,
                title = "Save & Share Favorites",
                description = "Bookmark your favorite recipes and share them with friends and family",
                color = Color(0xFFE74C3C),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureHighlightLarge(
                icon = Icons.Default.Star,
                title = "Smart Recipe Filters",
                description = "Find the perfect recipe with advanced search and filtering options",
                color = Color(0xFFF39C12),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Call to Action
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Primary Action Button
                    Button(
                        onClick = onGetStartedClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Action Button
                    OutlinedButton(
                        onClick = onBrowseAsGuestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF6B35)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp
                        )
                    ) {
                        Text(
                            text = "Browse as Guest",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Terms and Privacy
                    ClickableTermsPrivacyText(
                        onTermsClick = {
                            val intent = Intent(context, WebViewActivity::class.java)
                            intent.putExtra("title", "Terms of Service")
                            intent.putExtra("url", "https://kibetmanuu.github.io/recipehub-legal/#terms")
                            context.startActivity(intent)
                        },
                        onPrivacyClick = {
                            val intent = Intent(context, WebViewActivity::class.java)
                            intent.putExtra("title", "Privacy Policy")
                            intent.putExtra("url", "https://kibetmanuu.github.io/recipehub-legal/#privacy")
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright Footer
            Text(
                text = "© ${Calendar.getInstance().get(Calendar.YEAR)} RecipeHub Masters. All rights reserved.",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnimatedScaleCard(
    startAnimation: Boolean,
    content: @Composable () -> Unit
) {
    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val alpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        content()
    }
}

@Composable
fun FeatureHighlight(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3142),
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FeatureHighlightLarge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3142)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun FeatureHighlightWithDrawable(
    iconDrawable: Int,
    title: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = title,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3142)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ClickableTermsPrivacyText(
    modifier: Modifier = Modifier,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append("By continuing, you agree to our ")

        pushStringAnnotation(tag = "terms", annotation = "terms")
        withStyle(
            style = SpanStyle(
                color = Color(0xFFFF6B35),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append("Terms of Service")
        }
        pop()

        append(" and ")

        pushStringAnnotation(tag = "privacy", annotation = "privacy")
        withStyle(
            style = SpanStyle(
                color = Color(0xFFFF6B35),
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append("Privacy Policy")
        }
        pop()
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        ),
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "terms",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                onTermsClick()
            }

            annotatedString.getStringAnnotations(
                tag = "privacy",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                onPrivacyClick()
            }
        }
    )
}

// ==================== PREVIEW FUNCTIONS ====================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    RecipeTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    RecipeTheme {
        WelcomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureHighlightPreview() {
    RecipeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureHighlightWithDrawable(
                iconDrawable = R.drawable.youtube,
                title = "Watch Video Tutorials",
                description = "Learn cooking with step-by-step video guides from expert chefs",
                color = Color(0xFFFF0000),
                modifier = Modifier.fillMaxWidth()
            )

            FeatureHighlightLarge(
                icon = Icons.Default.Search,
                title = "10,000+ Global Recipes",
                description = "Explore cuisines from around the world with our extensive recipe database",
                color = Color(0xFFFF6B35),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}