package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled

/**
 * Glass for surfaces that open in their own window: bottom sheets, dialogs, menus.
 *
 * haze cannot see across windows, so these cannot blur the screen with [GlassSurface]. Instead they
 * get a translucent tint high enough to read on its own, plus, on Android 12+ dialogs and sheets,
 * the system's own blur of the window behind ([GlassWindowBlur]).
 */
object GlassPopupDefaults {
    /**
     * Popup body colour. Stays readable with no blur at all (Desktop, Android 11, cross-window blur
     * switched off by the device): white text on the dark one and #1B1B1B on the light one keep
     * well above 4.5:1 even with pure white or pure black behind the dimming scrim.
     */
    fun container(isDark: Boolean): Color =
        if (isDark) {
            GlassDefaults.DarkTintBase.copy(alpha = 0.86f)
        } else {
            Color(0xFFF7F7F7).copy(alpha = 0.84f)
        }

    fun border(isDark: Boolean): BorderStroke = BorderStroke(GlassDefaults.EdgeWidth, GlassDefaults.edgeBrush(isDark))

    val DialogShape: Shape = GlassShapes.Large
    val MenuShape: Shape = GlassShapes.squircle(16.dp)

    /** How far the system blurs the app behind a glass dialog or sheet. */
    val WindowBlurRadius: Dp = 30.dp
}

/**
 * Asks the platform to blur everything behind the popup window this is composed in.
 *
 * Android 12+: sets FLAG_BLUR_BEHIND with blurBehindRadius on the dialog's window (dialogs and M3
 * modal sheets both run in one). Devices with cross-window blur disabled ignore it, which is why
 * [GlassPopupDefaults.container] is readable without it. Desktop and older Android: nothing.
 * Emits no layout, so it can sit in any slot. Does nothing while Liquid Glass is off.
 */
@Composable
expect fun GlassWindowBlur(radius: Dp = GlassPopupDefaults.WindowBlurRadius)

/**
 * Material 3 [AlertDialog], glass-aware. Same parameters; with Liquid Glass off every default is
 * Material's own, so the dialog is unchanged. With it on, an unset [shape] or [containerColor]
 * becomes the glass one, and the window behind is blurred where the platform can.
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape? = null,
    containerColor: Color = Color.Unspecified,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val glass = LocalLiquidGlassEnabled.current
    val isDark = LocalIsDarkTheme.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // The confirm slot is the one slot every dialog has; GlassWindowBlur emits no layout there.
        confirmButton =
            if (glass) {
                {
                    GlassWindowBlur()
                    confirmButton()
                }
            } else {
                confirmButton
            },
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape ?: if (glass) GlassPopupDefaults.DialogShape else AlertDialogDefaults.shape,
        containerColor =
            when {
                containerColor.isSpecified -> containerColor
                glass -> GlassPopupDefaults.container(isDark)
                else -> AlertDialogDefaults.containerColor
            },
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}

/**
 * Material 3 [DropdownMenu], glass-aware. Same parameters; Material's defaults with Liquid Glass
 * off. With it on, unset shape/colour/border become the glass ones and tonal elevation drops to 0,
 * since its primary tint would muddy the glass. Menus are popups, not dialogs, so there is no
 * window blur here, only the translucent tint.
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape? = null,
    containerColor: Color = Color.Unspecified,
    tonalElevation: Dp? = null,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val isDark = LocalIsDarkTheme.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = shape ?: if (glass) GlassPopupDefaults.MenuShape else MenuDefaults.shape,
        containerColor =
            when {
                containerColor.isSpecified -> containerColor
                glass -> GlassPopupDefaults.container(isDark)
                else -> MenuDefaults.containerColor
            },
        tonalElevation = tonalElevation ?: if (glass) 0.dp else MenuDefaults.TonalElevation,
        shadowElevation = shadowElevation,
        border = border ?: if (glass) GlassPopupDefaults.border(isDark) else null,
        content = content,
    )
}

/**
 * Material 3 [ModalBottomSheet], glass-aware. Same parameters and defaults. The sheets in this app
 * mostly pass a transparent container and draw their body as a Card coloured from
 * [com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors], which turns glass on its own; this
 * wrapper adds the window blur behind and glasses an unset container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalLiquidGlassEnabled.current
    val resolvedContainer =
        when {
            containerColor.isSpecified -> containerColor
            glass -> GlassPopupDefaults.container(LocalIsDarkTheme.current)
            else -> BottomSheetDefaults.ContainerColor
        }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = shape,
        containerColor = resolvedContainer,
        contentColor = if (contentColor.isSpecified) contentColor else contentColorFor(resolvedContainer),
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
        content = {
            if (glass) GlassWindowBlur()
            content()
        },
    )
}
