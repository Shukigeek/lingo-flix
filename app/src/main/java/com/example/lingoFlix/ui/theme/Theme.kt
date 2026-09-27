package com.example.lingoFlix.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat

/**
 * Root theme for the whole app.
 *
 * Dynamic colour is opt-in and off by default: the green brand identity is part
 * of the product, and letting the wallpaper repaint the app would make the
 * "saved / due / mastered" colour language meaningless.
 *
 * @param dynamicColor when true and running on Android 12+, derives the scheme
 * from the user's wallpaper instead of the brand palette.
 */
@Composable
fun LingoFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> LingoDarkColorScheme
        else -> LingoLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // The app draws edge to edge, so the bars are transparent and only their
        // icon tint needs to follow the theme. window.statusBarColor is a no-op
        // from API 35 and is deprecated, hence appearance-only handling here.
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
private fun PalettePreviewContent() {
    val scheme = MaterialTheme.colorScheme
    val swatches = listOf(
        "primary" to (scheme.primary to scheme.onPrimary),
        "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
        "secondary" to (scheme.secondary to scheme.onSecondary),
        "tertiary" to (scheme.tertiary to scheme.onTertiary),
        "error" to (scheme.error to scheme.onError),
        "surfaceContainer" to (scheme.surfaceContainer to scheme.onSurface),
        "warning" to (LingoAccents.Warning to LingoAccents.OnWarning),
        "success" to (LingoAccents.Success to LingoAccents.OnSuccess),
    )
    Surface(color = scheme.background) {
        Column(
            modifier = Modifier.padding(LingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LingoSpacing.xs),
        ) {
            swatches.forEach { (name, colors) ->
                Surface(
                    color = colors.first,
                    contentColor = colors.second,
                    shape = RoundedCornerShape(LingoRadius.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(LingoSpacing.sm),
                    )
                }
            }
        }
    }
}

@Preview(name = "Palette light", showBackground = true)
@Composable
private fun ThemeLightPreview() {
    LingoFlixTheme(darkTheme = false) { PalettePreviewContent() }
}

@Preview(name = "Palette dark", showBackground = true)
@Composable
private fun ThemeDarkPreview() {
    LingoFlixTheme(darkTheme = true) { PalettePreviewContent() }
}
