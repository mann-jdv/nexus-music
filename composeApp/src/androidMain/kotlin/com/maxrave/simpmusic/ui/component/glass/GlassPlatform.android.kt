package com.maxrave.simpmusic.ui.component.glass

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogWindowProvider
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled

// kyant backdrop's lens is an AGSL RuntimeShader, added in API 33 (Android 13, TIRAMISU).
actual fun isGlassRefractionSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Both Compose dialogs and M3 modal sheets compose their content inside an AbstractComposeView that
 * implements [DialogWindowProvider], so the hosting window is the parent of [LocalView]. Composed
 * anywhere else (the activity window), the cast fails and this does nothing.
 *
 * FLAG_BLUR_BEHIND + blurBehindRadius (API 31) blurs everything under the window. Devices with
 * cross-window blur switched off (some GPUs, battery saver) ignore the request rather than fail.
 */
@Composable
actual fun GlassWindowBlur(radius: Dp) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (!LocalLiquidGlassEnabled.current) return
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    DisposableEffect(window, radiusPx) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.also { it.blurBehindRadius = radiusPx }
        // The window is torn down with the popup, so there is nothing to undo.
        onDispose { }
    }
}
