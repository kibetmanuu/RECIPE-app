package com.example.recipe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF6B35),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF8E53),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFFA07A)
)

@Composable
fun RecipeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}