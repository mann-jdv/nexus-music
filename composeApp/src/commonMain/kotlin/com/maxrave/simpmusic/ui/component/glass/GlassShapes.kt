package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle

/**
 * Continuous-corner ("squircle") shapes for glass surfaces, from kyant shapes.
 *
 * A plain `RoundedCornerShape` joins a straight edge to a circular arc, so the curvature jumps from
 * zero to 1/r at the tangent point. That jump is what reads as a hard corner under a light edge
 * highlight. [RoundedCornerStyle.Continuous] ramps the curvature instead, the way Apple's
 * corners do. Use these on glass so the highlight runs round the corner without a visible kink.
 */
object GlassShapes {
    /** Chips and small buttons. */
    val Small: Shape = squircle(12.dp)

    /** Cards and panels; the [GlassSurface] default. */
    val Medium: Shape = squircle(20.dp)

    /** Sheets and large hero panels. */
    val Large: Shape = squircle(28.dp)

    /** Fully rounded ends: tab bars, mini player, pills. */
    val Pill: Shape = Capsule(RoundedCornerStyle.Continuous)

    fun squircle(cornerRadius: Dp): Shape = RoundedRectangle(cornerRadius, RoundedCornerStyle.Continuous)
}
