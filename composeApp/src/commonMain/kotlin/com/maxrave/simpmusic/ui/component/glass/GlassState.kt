package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * What a screen's [GlassSurface]s sample from: a haze source for the blur, plus (only when asked
 * for) a kyant backdrop layer for refraction.
 *
 * Create one per screen with [rememberGlassState], mark the layer to be seen through with
 * [glassSource], and draw every [GlassSurface] as a SIBLING of that layer, on top of it. A surface
 * nested inside its own source would sample itself; for kyant that is a render-feedback loop that
 * crashes the RuntimeShader (see [com.maxrave.simpmusic.ui.component.liquidGlass]).
 */
@Stable
class GlassState internal constructor(
    internal val hazeState: HazeState,
    internal val backdrop: PlatformBackdrop?,
) {
    /** True when this state records a refraction layer, so surfaces may ask for refraction. */
    val hasRefraction: Boolean get() = backdrop != null
}

/**
 * Sets up the glass source for one screen.
 *
 * @param refraction also record a kyant backdrop layer, which [GlassSurface] needs for
 *   `refraction = true`. Off by default: the layer is re-recorded every frame, so only screens with
 *   refracting top-level chrome should pay for it.
 * @param refractionBackground painted under the content in the refraction layer, so transparent
 *   areas refract the page colour rather than black.
 */
@Composable
fun rememberGlassState(
    refraction: Boolean = false,
    refractionBackground: Color = MaterialTheme.colorScheme.background,
): GlassState {
    val hazeState = rememberHazeState()
    val backdrop = if (refraction) rememberBackdrop(refractionBackground) else null
    return remember(hazeState, backdrop) { GlassState(hazeState, backdrop) }
}

/**
 * Marks this layer as what the screen's [GlassSurface]s blur (and, with refraction, bend).
 *
 * Records nothing while Liquid Glass is off or blur is unsupported: surfaces then draw a flat tint
 * and never read the source, so recording it would only cost frames.
 */
@Composable
fun Modifier.glassSource(state: GlassState): Modifier {
    if (!LocalLiquidGlassEnabled.current || !isGlassBlurSupported()) return this
    val withRefraction =
        if (state.backdrop != null && isGlassRefractionSupported()) {
            this.layerBackdrop(state.backdrop)
        } else {
            this
        }
    return withRefraction.hazeSource(state.hazeState)
}
