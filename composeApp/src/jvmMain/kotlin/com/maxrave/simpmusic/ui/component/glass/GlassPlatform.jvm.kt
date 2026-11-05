package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

// Desktop runs kyant's lens through SkikoRuntimeShader, whatever the OS.
actual fun isGlassRefractionSupported(): Boolean = true

// No cross-window blur on Desktop: popups rely on GlassPopupDefaults.container being readable alone.
@Composable
actual fun GlassWindowBlur(radius: Dp) = Unit
