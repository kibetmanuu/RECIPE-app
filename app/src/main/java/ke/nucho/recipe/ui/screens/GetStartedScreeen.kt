package ke.nucho.recipe.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipe.R
import ke.nucho.recipe.ui.theme.RecipeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GetStartedScreen(
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        delay(4000)
        if (pagerState.currentPage < 2) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFFF8E53),
                        Color(0xFFFFA726),
                        Color(0xFFFF6B35)
                    ),
                    start = Offset(
                        animatedOffset * screenWidth.value / 360f,
                        0f
                    ),
                    end = Offset(
                        (animatedOffset + 180f) * screenWidth.value / 360f,
                        screenHeight.value
                    )
                )
            )
    ) {
        DecorativeCircles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            AppLogo()
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    OnboardingPage(
                        page = when (page) {
                            0 -> OnboardingData(
                                imageResId = R.drawable.recipe,
                                title = "Discover Amazing Recipes",
                                description = "Explore thousands of delicious recipes from around the world, curated just for you.",
                                color = Color.White
                            )

                            1 -> OnboardingData(
                                imageResId = R.drawable.recipe,
                                title = "Save Your Favorites",
                                description = "Keep track of recipes you love and create your personal cookbook collection.",
                                color = Color.White
                            )

                            else -> OnboardingData(
                                imageResId = R.drawable.recipe,
                                title = "Share & Connect",
                                description = "Share your culinary creations with friends and discover new cooking ideas together.",
                                color = Color.White
                            )
                        }
                    )
                }
            }

            PageIndicators(
                pageCount = 3,
                currentPage = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(32.dp))

            ActionButtons(
                currentPage = pagerState.currentPage,
                onGetStarted = onContinue,
                onSkip = onSkip,
                onNext = {
                    scope.launch {
                        if (pagerState.currentPage < 2) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DecorativeCircles() {
    Box(
        modifier = Modifier
            .offset(x = 200.dp, y = (-100).dp)
            .size(200.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
    )
    Box(
        modifier = Modifier
            .offset(x = (-80).dp, y = 500.dp)
            .size(120.dp)
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
    )
    Box(
        modifier = Modifier
            .offset(x = 50.dp, y = 100.dp)
            .size(60.dp)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
    )
}

@Composable
private fun AppLogo() {
    val scale by rememberInfiniteTransition(label = "logo_animation").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_recipe_logo),
                    contentDescription = "Recipe App",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RecipeBook",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your Culinary Companion",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPage(page: OnboardingData) {
    val animatedVisibility = remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = animatedVisibility.value,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(600)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = page.imageResId),
                        contentDescription = page.title,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = page.color,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.description,
                fontSize = 16.sp,
                color = page.color.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val animatedWidth by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 8.dp,
                animationSpec = tween(300),
                label = "indicator_width"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.5f,
                animationSpec = tween(300),
                label = "indicator_alpha"
            )
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = animatedAlpha))
            )
        }
    }
}

@Composable
private fun ActionButtons(
    currentPage: Int,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentPage < 2) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }
        } else {
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Get Started"
                )
            }
        }
    }
}

private data class OnboardingData(
    @DrawableRes val imageResId: Int,
    val title: String,
    val description: String,
    val color: Color
)

@Preview(showBackground = true, name = "Get Started Screen - Light")
@Composable
private fun GetStartedScreenPreview() {
    RecipeTheme {
        GetStartedScreen()
    }
}
