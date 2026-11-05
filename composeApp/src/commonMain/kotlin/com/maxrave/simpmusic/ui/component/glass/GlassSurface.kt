package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import com.kyant.backdrop.shadow.Shadow as BackdropShadow

/** How a [GlassSurface] is actually drawn, after the setting and the platform have had their say. */
enum class GlassRenderMode {
    /** haze blur of the source, tinted. Android 12+ and Desktop, with Liquid Glass on. */
    Blur,

    /** kyant backdrop: blur plus lens refraction and a lit rim. Android 13+ and Desktop, opt-in. */
    Refraction,

    /** A solid translucent tint, no blur. Liquid Glass off, or Android 11 and below. */
    Fallback,
}

object GlassDefaults {
    val BlurRadius: Dp = 24.dp

    /** Width of the light rim drawn round the edge. */
    val EdgeWidth: Dp = 1.dp

    /**
     * Alpha the tint is raised to when nothing is blurred behind it. Content behind a
     * barely-tinted surface stays sharp and fights the text on top, so the fallback has to be
     * close to opaque to stay readable.
     */
    const val FallbackAlpha: Float = 0.9f

    /** Alpha of [tint] in dark theme. See [tint] for why it is this high. */
    const val DarkTintAlpha: Float = 0.78f

    /** Alpha of [tint] in light theme. See [tint] for why it is this high. */
    const val LightTintAlpha: Float = 0.72f

    /** Near-black base of the dark tint, Apple's systemBackground in dark mode. */
    val DarkTintBase: Color = Color(0xFF1C1C1E)

    /**
     * The default wash over the blur: near-black in dark theme, white in light, matching the
     * system materials Apple layers over a blur.
     *
     * The alphas are set by WCAG AA (4.5:1) in the worst case, not by taste. Dark: secondary text
     * (onSurfaceVariant, about #C7C7C7) over the tint over a blurred pure-white area needs the
     * composite at or below about #4E4E4E, which takes alpha 0.78 of #1C1C1E. Light: onSurfaceVariant
     * (#474747) over the tint over a blurred pure-black area needs the composite at or above about
     * #B8B8B8, which takes 0.72 of white. Lower alphas look more like glass and fail on exactly the
     * bright or dark artwork this app is full of.
     */
    fun tint(isDark: Boolean): Color =
        if (isDark) {
            DarkTintBase.copy(alpha = DarkTintAlpha)
        } else {
            Color.White.copy(alpha = LightTintAlpha)
        }

    /**
     * Wash for static tiles ([glassTile]), which sit on the page background rather than on a blur.
     * They only need to lift off the page, so they are much lighter than [tint]; the text on them
     * is the page's own text over a near-page colour, so contrast is the page's. Light theme
     * darkens a touch instead of lightening: a white wash on the #FAFAFA page would not show.
     */
    fun tileTint(isDark: Boolean): Color =
        if (isDark) {
            Color.White.copy(alpha = 0.07f)
        } else {
            Color.Black.copy(alpha = 0.035f)
        }

    /** Rim for static tiles: the lit [edgeBrush] on dark, a plain dark hairline on a light page. */
    fun tileEdge(isDark: Boolean): Brush =
        if (isDark) edgeBrush(isDark = true) else SolidColor(Color.Black.copy(alpha = 0.08f))

    /** Rim that is brightest at the top-left, where light would catch a pane of glass. */
    fun edgeBrush(isDark: Boolean): Brush =
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (isDark) 0.30f else 0.90f),
            0.45f to Color.White.copy(alpha = if (isDark) 0.06f else 0.35f),
            1f to Color.White.copy(alpha = if (isDark) 0.16f else 0.60f),
        )

    fun shadow(isDark: Boolean): Shadow =
        Shadow(
            radius = 24.dp,
            color = Color.Black.copy(alpha = if (isDark) 0.36f else 0.14f),
            offset = DpOffset(0.dp, 8.dp),
        )
}

/**
 * Which path a [GlassSurface] given these arguments would take here: the effective Liquid Glass
 * setting, then platform support, then what the caller asked for.
 */
@Composable
fun glassRenderMode(
    state: GlassState,
    refraction: Boolean,
): GlassRenderMode =
    when {
        !LocalLiquidGlassEnabled.current || !isGlassBlurSupported() -> GlassRenderMode.Fallback
        refraction && state.hasRefraction && isGlassRefractionSupported() -> GlassRenderMode.Refraction
        else -> GlassRenderMode.Blur
    }

/**
 * A translucent, tinted surface that blurs what is behind it, with a thin light rim and a soft
 * shadow. Light and dark variants follow [LocalIsDarkTheme].
 *
 * It must be drawn ABOVE and as a SIBLING of the layer carrying [glassSource] for the same
 * [state], never inside it.
 *
 * Rendering, in order of preference:
 * - [GlassRenderMode.Refraction] when [refraction] is set, [state] was created with refraction, and
 *   the platform runs kyant's lens. Meant for top-level chrome only (tab bar, mini player, the
 *   player's top bar): the refraction layer is re-recorded every frame.
 * - [GlassRenderMode.Blur]: a haze blur of the source, washed with [tint].
 * - [GlassRenderMode.Fallback] with Liquid Glass off or on Android 11 and below: [tint] raised to
 *   [GlassDefaults.FallbackAlpha], with no blur. Same shape, rim and shadow, so layouts do not move.
 *
 * @param tint wash over the blur; [Color.Unspecified] picks [GlassDefaults.tint] for the theme.
 * @param blurRadius how soft the content behind gets. Larger is more frosted and costs more.
 * @param shadow whether to draw the soft drop shadow. Turn it off for surfaces that sit flush
 *   against an edge, where a shadow only reads as a dark band.
 */
@Composable
fun GlassSurface(
    state: GlassState,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.Medium,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = GlassDefaults.BlurRadius,
    refraction: Boolean = false,
    shadow: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val resolvedTint = if (tint.isSpecified) tint else GlassDefaults.tint(isDark)
    val fallbackTint = resolvedTint.copy(alpha = maxOf(resolvedTint.alpha, GlassDefaults.FallbackAlpha))
    val mode = glassRenderMode(state, refraction)

    val surface =
        when (mode) {
            GlassRenderMode.Fallback ->
                Modifier
                    .dropShadowIf(shadow, shape, isDark)
                    .clip(shape)
                    .background(fallbackTint)
                    .border(GlassDefaults.EdgeWidth, GlassDefaults.edgeBrush(isDark), shape)

            GlassRenderMode.Blur ->
                Modifier
                    .dropShadowIf(shadow, shape, isDark)
                    .clip(shape)
                    .hazeBlur(
                        HazeInput.Sources(state.hazeState),
                        glassBlurStyle(
                            blurRadius = blurRadius,
                            tint = resolvedTint,
                            fallbackTint = fallbackTint,
                            background = MaterialTheme.colorScheme.surface,
                        ),
                    ).border(GlassDefaults.EdgeWidth, GlassDefaults.edgeBrush(isDark), shape)

            // kyant draws its own shape clip, rim (Highlight) and shadow, so none are added here.
            GlassRenderMode.Refraction ->
                Modifier.drawBackdrop(
                    backdrop = requireNotNull(state.backdrop),
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(blurRadius.toPx())
                        // Same proportions as the shared liquidGlass: the refraction band stays under
                        // the inradius so the top and bottom bands never meet in a seam.
                        lens(size.minDimension / 4f, size.minDimension / 2f)
                    },
                    highlight = { Highlight.Default },
                    shadow = {
                        if (shadow) {
                            BackdropShadow(radius = 16.dp, alpha = if (isDark) 0.36f else 0.14f)
                        } else {
                            null
                        }
                    },
                    onDrawSurface = { drawRect(resolvedTint) },
                )
        }

    Box(
        modifier = modifier.then(surface),
        contentAlignment = contentAlignment,
        content = content,
    )
}

private fun Modifier.dropShadowIf(
    enabled: Boolean,
    shape: Shape,
    isDark: Boolean,
): Modifier = if (enabled) this.dropShadow(shape, GlassDefaults.shadow(isDark)) else this

/**
 * Not remembered, on purpose: `HazeBlurStyle { }` records its writes and compares by them, so an
 * unchanged style recomposes into an equal one and leaves the node alone (same as `barBlurStyle`).
 */
private fun glassBlurStyle(
    blurRadius: Dp,
    tint: Color,
    fallbackTint: Color,
    background: Color,
): HazeBlurStyle =
    HazeBlurStyle {
        blurRadius(blurRadius)
        backgroundColor(background)
        colorEffects(listOf(HazeColorEffect.tint(tint)))
        // A touch of grain keeps large blurred areas from banding on 8-bit panels.
        noiseFactor(0.08f)
        // Only reached if haze itself declines to blur; GlassSurface already routes unsupported
        // platforms to the Fallback path before haze is involved.
        fallbackColorEffect(HazeColorEffect.tint(fallbackTint))
    }
