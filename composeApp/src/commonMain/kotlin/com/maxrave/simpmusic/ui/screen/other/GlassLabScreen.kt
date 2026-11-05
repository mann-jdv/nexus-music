package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.glass.GlassRenderMode
import com.maxrave.simpmusic.ui.component.glass.GlassShapes
import com.maxrave.simpmusic.ui.component.glass.GlassSurface
import com.maxrave.simpmusic.ui.component.glass.glassRenderMode
import com.maxrave.simpmusic.ui.component.glass.glassSource
import com.maxrave.simpmusic.ui.component.glass.isGlassBlurSupported
import com.maxrave.simpmusic.ui.component.glass.isGlassRefractionSupported
import com.maxrave.simpmusic.ui.component.glass.rememberGlassState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.glass_lab
import kotlin.math.roundToInt

/**
 * Developer options → Glass lab. Shows [GlassSurface] over album art in a light and a dark panel
 * side by side, with the switches that decide how it renders, so the three paths (blur, refraction,
 * flat fallback) can be compared on the device in hand.
 *
 * The artwork is the now-playing track's when something is playing. Under it, and on its own when
 * nothing is, sits a generated high-contrast pattern, because a blur is easiest to judge over hard
 * edges.
 *
 * Developer-only, so its control labels are not translated.
 */
@Composable
fun GlassLabScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val screenData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val artworkUrl = screenData.thumbnailURL
    val seed = MaterialTheme.colorScheme.primary

    // Starts from the real setting; flipping it here overrides it for this screen only.
    val settingOn = LocalLiquidGlassEnabled.current
    var glassOn by rememberSaveable { mutableStateOf(settingOn) }
    var refraction by rememberSaveable { mutableStateOf(false) }
    var blurRadius by rememberSaveable { mutableFloatStateOf(24f) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.glass_lab), style = typo().titleMedium) },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(SimpIcons.ArrowBackIosNew, Modifier.size(32.dp), true, tint = onSurface) {
                        navController.navigateUp()
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Blur: ${if (isGlassBlurSupported()) "supported" else "not supported, fallback tint"} · " +
                    "Refraction: ${if (isGlassRefractionSupported()) "supported" else "not supported, uses blur"} · " +
                    "Setting: ${if (settingOn) "on" else "off"}",
                style = typo().bodySmall,
            )
            LabSwitch("Liquid Glass (this screen only)", glassOn) { glassOn = it }
            LabSwitch("Refraction (top bar only, kyant backdrop)", refraction) { refraction = it }
            Text("Blur radius: ${blurRadius.roundToInt()} dp", style = typo().bodyMedium)
            Slider(value = blurRadius, onValueChange = { blurRadius = it }, valueRange = 0f..48f)

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 640.dp
                val panel: @Composable (Boolean, Modifier) -> Unit = { isDark, modifier ->
                    GlassLabPanel(
                        isDark = isDark,
                        seed = seed,
                        artworkUrl = artworkUrl,
                        title = screenData.nowPlayingTitle.ifBlank { "Nothing playing" },
                        artist = screenData.artistName.ifBlank { "Generated pattern" },
                        glassOn = glassOn,
                        refraction = refraction,
                        blurRadius = blurRadius,
                        modifier = modifier,
                    )
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        panel(false, Modifier.weight(1f))
                        panel(true, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        panel(false, Modifier.fillMaxWidth())
                        panel(true, Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
        }
    }
}

@Composable
private fun LabSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = typo().bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * One themed panel: a full light or dark scheme from the app's seed, with [LocalIsDarkTheme] and
 * [LocalLiquidGlassEnabled] overridden so the glass inside renders exactly as it would in that theme.
 */
@Composable
private fun GlassLabPanel(
    isDark: Boolean,
    seed: Color,
    artworkUrl: String?,
    title: String,
    artist: String,
    glassOn: Boolean,
    refraction: Boolean,
    blurRadius: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = rememberDynamicColorScheme(seedColor = seed, isDark = isDark, style = PaletteStyle.TonalSpot)
    MaterialTheme(colorScheme = scheme, typography = typo(scheme, forceDark = false)) {
        CompositionLocalProvider(
            LocalIsDarkTheme provides isDark,
            LocalLiquidGlassEnabled provides glassOn,
            LocalContentColor provides scheme.onSurface,
        ) {
            val glass = rememberGlassState(refraction = refraction, refractionBackground = scheme.background)
            val radius = blurRadius.dp
            val chromeMode = glassRenderMode(glass, refraction = true)
            val bodyMode = glassRenderMode(glass, refraction = false)
            Column(
                modifier
                    .clip(GlassShapes.Large)
                    .background(scheme.background)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${if (isDark) "Dark" else "Light"} · top bar: ${chromeMode.label()} · cards: ${bodyMode.label()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onBackground,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                        .clip(GlassShapes.Large),
                ) {
                    // The source layer. Every GlassSurface below is its sibling, drawn on top.
                    LabArtwork(artworkUrl, Modifier.matchParentSize().glassSource(glass))

                    GlassSurface(
                        state = glass,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                                .fillMaxWidth()
                                .height(52.dp),
                        shape = GlassShapes.Pill,
                        blurRadius = radius,
                        refraction = true,
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(SimpIcons.ArrowBackIosNew, null, Modifier.padding(8.dp).size(20.dp))
                            Text(
                                "Now Playing",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(SimpIcons.MoreVert, null, Modifier.padding(8.dp).size(20.dp))
                        }
                    }

                    GlassSurface(
                        state = glass,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth(),
                        shape = GlassShapes.Medium,
                        blurRadius = radius,
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "GlassShapes.Medium · ${radius.value.roundToInt()} dp blur",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    GlassSurface(
                        state = glass,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .fillMaxWidth()
                                .height(64.dp),
                        shape = GlassShapes.Pill,
                        blurRadius = radius,
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LabArtwork(artworkUrl, Modifier.size(44.dp).clip(GlassShapes.Small))
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(SimpIcons.Favorite, null, Modifier.size(22.dp))
                            Icon(SimpIcons.PlayArrow, null, Modifier.size(26.dp))
                            Icon(SimpIcons.SkipNext, null, Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun GlassRenderMode.label(): String =
    when (this) {
        GlassRenderMode.Blur -> "blur"
        GlassRenderMode.Refraction -> "refraction"
        GlassRenderMode.Fallback -> "fallback tint"
    }

/** The now-playing artwork over a generated pattern (shown alone when nothing is playing). */
@Composable
private fun LabArtwork(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFF2EC4B6), Color(0xFF3A0CA3))))
            // Hard-edged stripes and dots: a blur is easiest to judge over sharp detail.
            val stripe = size.width / 14f
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = stripe / 3f,
                )
                x += stripe
            }
            drawCircle(Color(0xFF111111), radius = size.minDimension / 5f, center = Offset(size.width * 0.3f, size.height * 0.35f))
            drawCircle(Color.White, radius = size.minDimension / 8f, center = Offset(size.width * 0.72f, size.height * 0.62f))
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
