package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Lowest tint alpha a glass top bar may use. Bars carry titles and icons over whatever scrolls under
 * them, so they are held to the same WCAG AA worst case as [GlassDefaults.tint]. The thinner 0.3
 * washes several bars use outside glass mode fail it over bright artwork.
 */
const val GlassBarMinTintAlpha: Float = 0.75f

/**
 * A plain top bar's frosted blur ([ultraThinBarStyle]), glass-aware.
 *
 * Liquid Glass off: exactly the `hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())` this
 * replaced. On: the glass tint for the theme at [GlassBarMinTintAlpha] and a hairline along the
 * bottom edge where the content scrolls under.
 */
@Composable
fun Modifier.glassBarBlur(hazeState: HazeState): Modifier {
    if (!LocalLiquidGlassEnabled.current) {
        return this.hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())
    }
    val isDark = LocalIsDarkTheme.current
    return this.glassBar(hazeState, if (isDark) GlassDefaults.DarkTintBase else Color.White, GlassBarMinTintAlpha, isDark)
}

/**
 * A tinted top bar's frosted blur ([barBlurStyle], washed with the page or artwork colour),
 * glass-aware.
 *
 * Liquid Glass off: exactly the `hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(tint, tintAlpha))`
 * this replaced. On: the same colour, raised to at least [GlassBarMinTintAlpha], plus the bottom
 * hairline.
 */
@Composable
fun Modifier.glassBarBlur(
    hazeState: HazeState,
    tint: Color,
    tintAlpha: Float,
): Modifier {
    if (!LocalLiquidGlassEnabled.current) {
        return this.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(tint, tintAlpha))
    }
    return this.glassBar(hazeState, tint, maxOf(tintAlpha, GlassBarMinTintAlpha), LocalIsDarkTheme.current)
}

private fun Modifier.glassBar(
    hazeState: HazeState,
    tint: Color,
    tintAlpha: Float,
    isDark: Boolean,
): Modifier {
    // Same separator Apple draws under a bar once content is under it: light on dark, dark on light.
    val edge = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
    return this
        .hazeBlur(
            HazeInput.Sources(hazeState),
            // Where haze cannot blur (Android 11 and below) barBlurStyle's own opaque fallback
            // applies, as it always has for these bars.
            barBlurStyle(tint, tintAlpha).then { noiseFactor(0.08f) },
        ).drawWithContent {
            drawContent()
            val px = 1.dp.toPx()
            drawRect(edge, topLeft = Offset(0f, size.height - px), size = Size(size.width, px))
        }
}

/**
 * Static glass for tiles inside scrolling content: translucent tint, light rim and squircle, with
 * no live blur. A blur per tile would re-render the whole list under every one of them on every
 * scroll frame, and a tile inside the haze source cannot sample that source anyway.
 *
 * Returns this modifier unchanged while Liquid Glass is off, so other themes look as before.
 */
@Composable
fun Modifier.glassTile(shape: Shape = GlassShapes.Medium): Modifier {
    if (!LocalLiquidGlassEnabled.current) return this
    val isDark = LocalIsDarkTheme.current
    return this
        .padding(4.dp)
        .clip(shape)
        .background(GlassDefaults.tileTint(isDark))
        .border(GlassDefaults.EdgeWidth, GlassDefaults.tileEdge(isDark), shape)
}
