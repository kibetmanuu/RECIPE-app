package ke.nucho.recipe.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ke.nucho.recipe.ui.screens.WelcomeScreen
import ke.nucho.recipe.ui.theme.RecipeTheme

// Define your app routes
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object RecipeList : Screen("recipe_list")
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        // Welcome Screen
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    // Navigate to recipe list or onboarding
                    navController.navigate(Screen.RecipeList.route) {
                        // Remove welcome from back stack
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onExploreRecipes = {
                    // Navigate directly to recipes
                    navController.navigate(Screen.RecipeList.route)
                }
            )
        }

        // Recipe List Screen (placeholder for now)
        composable(Screen.RecipeList.route) {
            PlaceholderRecipeListScreen()
        }

        // Recipe Detail Screen (placeholder for now)
        composable(Screen.RecipeDetail.route) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            PlaceholderRecipeDetailScreen(recipeId = recipeId)
        }
    }
}

// Placeholder composables for preview purposes
@Composable
fun PlaceholderRecipeListScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Recipe List Screen\n(Coming Soon)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlaceholderRecipeDetailScreen(recipeId: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Recipe Detail Screen\nRecipe ID: $recipeId\n(Coming Soon)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

// Preview for the main navigation (starts with Welcome screen)
@Preview(
    name = "App Navigation - Welcome",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun AppNavigationPreview() {
    RecipeTheme {
        AppNavigation()
    }
}

// Preview for the navigation in dark theme
@Preview(
    name = "App Navigation - Welcome Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AppNavigationDarkPreview() {
    RecipeTheme {
        AppNavigation()
    }
}

// Preview for individual placeholder screens
@Preview(
    name = "Recipe List Placeholder",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun RecipeListPlaceholderPreview() {
    RecipeTheme {
        PlaceholderRecipeListScreen()
    }
}

@Preview(
    name = "Recipe Detail Placeholder",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun RecipeDetailPlaceholderPreview() {
    RecipeTheme {
        PlaceholderRecipeDetailScreen(recipeId = "sample_recipe_123")
    }
}

// Preview for Recipe List in dark theme
@Preview(
    name = "Recipe List Placeholder Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun RecipeListPlaceholderDarkPreview() {
    RecipeTheme {
        PlaceholderRecipeListScreen()
    }
}

// Preview for Recipe Detail in dark theme
@Preview(
    name = "Recipe Detail Placeholder Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun RecipeDetailPlaceholderDarkPreview() {
    RecipeTheme {
        PlaceholderRecipeDetailScreen(recipeId = "sample_recipe_456")
    }
}

// Preview for tablet layout
@Preview(
    name = "App Navigation - Tablet",
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun AppNavigationTabletPreview() {
    RecipeTheme {
        AppNavigation()
    }
}

// Preview for landscape orientation
@Preview(
    name = "App Navigation - Landscape",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,dpi=420,orientation=landscape"
)
@Composable
fun AppNavigationLandscapePreview() {
    RecipeTheme {
        AppNavigation()
    }
}