package com.example.lingoFlix.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * LingoFlix palette.
 *
 * The brand colour is a confident, slightly deeper green than the usual
 * gamified-learning green, paired with a cinema-leaning indigo secondary so the
 * app reads as "video first, learning second". The warm orange from the previous
 * theme survives only as the streak/warning accent, where it actually earns its
 * place; using it as the primary made every screen look like a food delivery app.
 */

// ── Brand ramp: green ───────────────────────────────────────────────────
private val Green10 = Color(0xFF00210B)
private val Green20 = Color(0xFF003916)
private val Green30 = Color(0xFF005322)
private val Green40 = Color(0xFF1B7A3C)
private val Green80 = Color(0xFF7FDB9C)
private val Green90 = Color(0xFF9CF8B6)
private val Green95 = Color(0xFFC8FFD5)

// ── Secondary ramp: cinema indigo ───────────────────────────────────────
private val Indigo10 = Color(0xFF12103A)
private val Indigo20 = Color(0xFF262457)
private val Indigo30 = Color(0xFF3D3B70)
private val Indigo40 = Color(0xFF55528A)
private val Indigo80 = Color(0xFFC0BFF8)
private val Indigo90 = Color(0xFFE2E0FF)

// ── Tertiary ramp: teal, used for translations and subtitles chrome ─────
private val Teal10 = Color(0xFF00201F)
private val Teal20 = Color(0xFF003736)
private val Teal30 = Color(0xFF00504E)
private val Teal40 = Color(0xFF006A67)
private val Teal80 = Color(0xFF4FDAD5)
private val Teal90 = Color(0xFF70F7F1)

// ── Error ramp ──────────────────────────────────────────────────────────
private val Red10 = Color(0xFF410002)
private val Red20 = Color(0xFF690005)
private val Red30 = Color(0xFF93000A)
private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)

// ── Neutrals ────────────────────────────────────────────────────────────
private val Neutral6 = Color(0xFF101410)
private val Neutral10 = Color(0xFF171D18)
private val Neutral12 = Color(0xFF1B211C)
private val Neutral17 = Color(0xFF252B26)
private val Neutral20 = Color(0xFF2C322D)
private val Neutral22 = Color(0xFF303630)
private val Neutral24 = Color(0xFF343A35)
private val Neutral90 = Color(0xFFDDE5DA)
private val Neutral92 = Color(0xFFE3EBE0)
private val Neutral94 = Color(0xFFE9F1E6)
private val Neutral96 = Color(0xFFEFF7EC)
private val Neutral98 = Color(0xFFF6FEF2)
private val NeutralVariant30 = Color(0xFF3F4940)
private val NeutralVariant50 = Color(0xFF707970)
private val NeutralVariant60 = Color(0xFF8A938A)
private val NeutralVariant80 = Color(0xFFBFC9BF)
private val NeutralVariant90 = Color(0xFFDBE5DA)

/**
 * Semantic accents that Material 3 has no slot for.
 *
 * Exposed as plain colours rather than scheme roles because they are used in
 * exactly two places each (streak chip, mastery badges) and inventing custom
 * roles for them would make theming harder, not easier.
 */
object LingoAccents {
    /** Streak / warning. The one warm hue we kept from the old theme. */
    val Warning = Color(0xFFE08A1E)
    val OnWarning = Color(0xFF2B1600)
    val WarningContainer = Color(0xFFFFE0B6)
    val OnWarningContainer = Color(0xFF2B1600)

    /** "Correct answer" / mastered. Brighter than [ColorScheme.primary] on purpose. */
    val Success = Color(0xFF2FA35A)
    val OnSuccess = Color(0xFFFFFFFF)
    val SuccessContainer = Green90
    val OnSuccessContainer = Green10

    /** Scrim used behind player subtitles so text stays legible on any frame. */
    val SubtitleScrim = Color(0xCC000000)
}

val LingoLightColorScheme: ColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    inversePrimary = Green80,

    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = Indigo90,
    onSecondaryContainer = Indigo10,

    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,

    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,

    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Green40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral96,

    surfaceBright = Neutral98,
    surfaceDim = Neutral90,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,

    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
)

val LingoDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green95,
    inversePrimary = Green40,

    secondary = Indigo80,
    onSecondary = Indigo20,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Indigo90,

    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,

    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,

    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Green80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,

    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,

    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color.Black,
)
