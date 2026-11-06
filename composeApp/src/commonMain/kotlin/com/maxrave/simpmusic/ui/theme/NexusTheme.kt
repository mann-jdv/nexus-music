package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nexus Violet: the fork's own look — near-black fading into deep purple, violet accents and a neon
 * glow on whatever is active. Ported from the Expo design prototype (`My Music APP/nexus-music`), so
 * the hex values below match that app's `constants/colors.ts` one for one.
 *
 * It is what [DataStoreManager.THEME_COLOR_DEFAULT][com.maxrave.domain.manager.DataStoreManager.THEME_COLOR_DEFAULT]
 * means in this fork. The pre-fork light-blue default is still one tap away as [THEME_COLOR_CLASSIC].
 */
object NexusColors {
    val Gradient0 = Color(0xFF06020F)
    val Gradient1 = Color(0xFF110626)
    val Gradient2 = Color(0xFF1F0B47)
    val Gradient3 = Color(0xFF341470)

    val AccentPurple = Color(0xFF6D28D9)
    val AccentGlow = Color(0xFFA855F7)
    val GlowSoft = Color(0xFFC4B5FD)

    val Glass = Color(0x0FFFFFFF) // white @ 6%
    val GlassStrong = Color(0x1FA78BFA) // violet @ 12%
    val GlassBorder = Color(0x2EC4AAFF) // lavender @ 18%
    val TextMuted = Color(0xFFA99AD6)
    val Track = Color(0x2EC4AAFF)

    val accentGradient = listOf(AccentPurple, AccentGlow)
}

/**
 * Stored value of the pre-fork light-blue theme. Lives here rather than in core's DataStoreManager so
 * the fork does not change the core submodule: the setting stores whatever string it is handed.
 */
const val THEME_COLOR_CLASSIC = "CLASSIC"

/**
 * True while the app runs Nexus Violet in its dark scheme. Screens with a Nexus layout of their own
 * (Home) switch to it on this; light theme and the other colour sources keep the stock layout.
 */
val LocalNexusTheme = staticCompositionLocalOf { false }

/** The seed the theme had before Nexus Violet; kept for [THEME_COLOR_CLASSIC]. */
val classicSeed = Color(0xFF8ECAE6)

/**
 * Deep-purple surfaces for the dark scheme. The generated TonalSpot neutrals from a violet seed are
 * nearly grey (tone 6 with a hint of hue); the prototype's page is unmistakably purple, so the
 * surface ramp is pinned instead. Primary/secondary/tertiary stay seed-derived.
 */
internal fun ColorScheme.withNexusDarkSurfaces(): ColorScheme =
    copy(
        background = Color(0xFF0B0518),
        onBackground = Color(0xFFF1ECFF),
        surface = Color(0xFF0B0518),
        onSurface = Color(0xFFF1ECFF),
        surfaceVariant = Color(0xFF2A1B4A),
        onSurfaceVariant = Color(0xFFB9A6D9),
        surfaceTint = primary,
        surfaceBright = Color(0xFF2E1F4F),
        surfaceDim = Color(0xFF0B0518),
        surfaceContainerLowest = Color(0xFF07030F),
        surfaceContainerLow = Color(0xFF120A24),
        surfaceContainer = Color(0xFF170D2E),
        surfaceContainerHigh = Color(0xFF1E1238),
        surfaceContainerHighest = Color(0xFF261745),
        outline = Color(0xFF7E6CA6),
        outlineVariant = Color(0xFF3A2A5E),
        inverseSurface = Color(0xFFE9E0FF),
        inverseOnSurface = Color(0xFF1E1238),
    )

/**
 * Neon glow behind a shape: a tight bright core plus a wide soft bloom, the same two-layer
 * `boxShadow` the prototype uses. Drawn with Compose's `dropShadow`, so it renders on Android and
 * Desktop alike (it is not an elevation shadow and needs no API level).
 */
fun Modifier.nexusGlow(
    shape: Shape,
    color: Color = NexusColors.AccentGlow,
    radius: Dp = 24.dp,
    intensity: Float = 0.8f,
): Modifier {
    if (intensity <= 0f) return this
    return this
        .dropShadow(shape, Shadow(radius = radius * 1.6f, color = color, spread = radius * 0.15f, alpha = (intensity * 0.45f).coerceIn(0f, 1f)))
        .dropShadow(shape, Shadow(radius = radius * 0.4f, color = color, alpha = intensity.coerceIn(0f, 1f)))
}

/**
 * Frosted panel without a live blur: translucent fill plus a 1dp lavender hairline. Live blur stays
 * with the Liquid Glass components; these panels sit on a dark gradient, where a tint reads the same
 * and costs nothing per frame.
 */
fun Modifier.nexusGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
): Modifier =
    this
        .clip(shape)
        .background(if (strong) NexusColors.GlassStrong else NexusColors.Glass, shape)
        .border(1.dp, NexusColors.GlassBorder, shape)

/**
 * The prototype's page: the four-stop violet gradient with two soft light orbs and a darkening veil
 * at the top and bottom, so white text keeps its contrast at both ends.
 */
@Composable
fun NexusBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            colorStops =
                                arrayOf(
                                    0f to NexusColors.Gradient0,
                                    0.38f to NexusColors.Gradient1,
                                    0.72f to NexusColors.Gradient2,
                                    1f to NexusColors.Gradient3,
                                ),
                            start = Offset(size.width * 0.15f, 0f),
                            end = Offset(size.width * 0.85f, size.height),
                        ),
                    )
                    // Orbs: radial gradients rather than solid circles, so they read as light, not shapes.
                    val topR = size.width * 0.9f
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0x4D7C3AED), Color.Transparent),
                            center = Offset(size.width * 1.05f, -size.width * 0.05f),
                            radius = topR,
                        ),
                        radius = topR,
                        center = Offset(size.width * 1.05f, -size.width * 0.05f),
                    )
                    val bottomR = size.width * 1.1f
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0x595B21B6), Color.Transparent),
                            center = Offset(-size.width * 0.1f, size.height * 1.02f),
                            radius = bottomR,
                        ),
                        radius = bottomR,
                        center = Offset(-size.width * 0.1f, size.height * 1.02f),
                    )
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color(0x9906020F),
                            0.45f to Color.Transparent,
                            1f to Color(0x7306020F),
                        ),
                    )
                },
        contentAlignment = Alignment.TopStart,
        content = content,
    )
}
