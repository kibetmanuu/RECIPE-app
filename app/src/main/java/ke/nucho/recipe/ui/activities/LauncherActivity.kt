package ke.nucho.recipe.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.nucho.recipe.MainActivity
import ke.nucho.recipe.R
import ke.nucho.recipe.ui.theme.RecipeTheme
import ke.nucho.recipe.utils.PreferencesManager
import kotlinx.coroutines.delay

/**
 * Launcher Activity - Decides where to navigate based on first launch
 *
 * First Launch Flow:
 * LauncherActivity -> WelcomeActivity -> GetStartedActivity -> MainActivity
 *
 * Subsequent Launch Flow:
 * LauncherActivity -> MainActivity
 */
@SuppressLint("CustomSplashScreen")
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RecipeTheme {
                LauncherScreen()
            }
        }
    }

    @Composable
    fun LauncherScreen() {
        var navigating by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            // Show splash for 2 seconds
            delay(2000)

            if (!navigating) {
                navigating = true
                navigateToNextScreen()
            }
        }

        // Splash Screen UI
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
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo
                Surface(
                    modifier = Modifier.size(150.dp),
                    shape = MaterialTheme.shapes.extraLarge,
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
                            modifier = Modifier.size(100.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "RecipeHub",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your Culinary Journey",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }
    }

    private fun navigateToNextScreen() {
        val isFirstLaunch = PreferencesManager.isFirstLaunch(this)

        val intent = if (isFirstLaunch) {
            // First time user - show full onboarding
            Intent(this, WelcomeActivity::class.java)
        } else {
            // Returning user - go directly to main app
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish() // Close launcher so user can't go back to it
    }
}