package com.example.recipe.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit = {},
    onExploreRecipes: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35), // Orange
                        Color(0xFFFF8E53), // Light Orange
                        Color(0xFFFFA07A)  // Peach
                    )
                )
            )
    ) {
        // Background decorative elements
        BackgroundElements()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // App Logo/Icon with animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000)) +
                        scaleIn(animationSpec = tween(1000))
            ) {
                AppLogo()
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Welcome text with staggered animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 400)) +
                        slideInFromTop(animationSpec = tween(800, delayMillis = 400))
            ) {
                WelcomeText()
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature highlights
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 800)) +
                        slideInFromBottom(animationSpec = tween(800, delayMillis = 800))
            ) {
                FeatureHighlights()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 1200)) +
                        slideInFromBottom(animationSpec = tween(800, delayMillis = 1200))
            ) {
                ActionButtons(
                    onGetStarted = onGetStarted,
                    onExploreRecipes = onExploreRecipes
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BackgroundElements() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Floating circles for decoration
        repeat(3) { index ->
            val animatedOffset by rememberInfiniteTransition(label = "floating").animateFloat(
                initialValue = 0f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 3000 + (index * 500),
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset$index"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = (50 + index * 100).dp,
                        y = (100 + index * 150 + animatedOffset).dp
                    )
                    .size((60 + index * 20).dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = 0.1f)
                    )
            )
        }
    }
}

@Composable
private fun AppLogo() {
    Card(
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Using a simple text-based logo since icons are causing issues
            Text(
                text = "🍳",
                fontSize = 48.sp,
                color = Color(0xFFFF6B35)
            )
        }
    }
}

@Composable
private fun WelcomeText() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )

        Text(
            text = "RecipeBook",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Discover delicious recipes from around the world\nand create your own culinary masterpieces",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun FeatureHighlights() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeatureItem(
            icon = Icons.Default.Star,
            title = "10,000+ Recipes",
            description = "From quick meals to gourmet dishes"
        )

        FeatureItem(
            icon = Icons.Default.Favorite,
            title = "Step-by-Step Guide",
            description = "Easy to follow cooking instructions"
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onGetStarted: () -> Unit,
    onExploreRecipes: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFFFF6B35)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Secondary button
        OutlinedButton(
            onClick = onExploreRecipes,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = BorderStroke(2.dp, Color.White),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Explore Recipes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Animation extensions
fun slideInFromTop(animationSpec: FiniteAnimationSpec<IntOffset> = tween()) =
    slideIn(animationSpec) { IntOffset(0, -it.height / 2) }

fun slideInFromBottom(animationSpec: FiniteAnimationSpec<IntOffset> = tween()) =
    slideIn(animationSpec) { IntOffset(0, it.height / 2) }

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen()
    }
}