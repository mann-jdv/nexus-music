package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.rememberDeviceVolumeController
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.lengthLabel
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.icon.VolumeDown
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.theme.NexusBackground
import com.maxrave.simpmusic.ui.theme.NexusColors
import com.maxrave.simpmusic.ui.theme.nexusGlass
import com.maxrave.simpmusic.ui.theme.nexusGlow
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.now_playing
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stored value of the Nexus Violet Now Playing style. Defined here, like Liquid Glass's, so the fork
 * does not change core's DataStoreManager: the setting stores whatever string it is handed.
 */
const val NOW_PLAYING_STYLE_NEXUS_VIOLET = "NEXUS_VIOLET"

/**
 * The Nexus Violet Now Playing style, ported from the Expo design prototype (`My Music APP/nexus-music`,
 * `app/player.tsx`): a circular "vinyl" cover that turns while the track plays inside a glowing violet
 * ring, the title with a soft neon glow, a gradient seek bar whose knob glows, a glass play button
 * that pulses, and a glass action bar.
 *
 * Hands the whole page to Classic in the same cases Liquid Glass does — a Spotify canvas or a
 * playing video (both want the full page), a landscape page and picture-in-picture — crossfaded,
 * since a canvas tends to arrive a few seconds into the track.
 */
@Composable
fun NowPlayingContentNexusViolet(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val useClassic =
            landscape ||
                state.isInPipMode ||
                state.screenData.fullscreenCanvas() != null ||
                (state.screenData.isVideo && state.shouldShowVideo)
        Crossfade(targetState = useClassic, animationSpec = tween(350), label = "nexusVioletFallback") { classic ->
            if (classic) {
                NowPlayingContentSpotify(state = state, actions = actions)
            } else {
                NexusVioletPlayer(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun NexusVioletPlayer(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // No scroll on this page, so the shell's toolbar flag stays parked (as in the Apple Music style).
    LaunchedEffect(Unit) { actions.onToolbarVisibilityChange(false) }

    val density = LocalDensity.current
    val isDesktop = getPlatform() == Platform.Desktop
    val deviceVolumeController = rememberDeviceVolumeController()
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val systemBottom = with(density) { WindowInsets.systemBars.getBottom(density).toDp() }

    // Entrance: the disc springs in and the panel rises, once per opening of the player.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) }

    NexusBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop + 8.dp, bottom = systemBottom + 16.dp)
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NexusHeader(state = state, actions = actions)

            // The disc takes whatever height the controls leave.
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // The disc plus its ring and halo is disc + 72dp, so the height bound leaves room for both.
                val disc = minOf(maxWidth * 0.74f, maxHeight - 80.dp, 340.dp).coerceAtLeast(120.dp)
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            val s = 0.82f + 0.18f * entrance.value
                            scaleX = s
                            scaleY = s
                            alpha = entrance.value
                        },
                ) {
                    NexusDisc(state = state, actions = actions, size = disc)
                }
            }

            Column(
                modifier =
                    Modifier.graphicsLayer {
                        translationY = (1f - entrance.value) * 40.dp.toPx()
                        alpha = entrance.value
                    },
            ) {
                NexusTitleRow(state = state, actions = actions)
                Spacer(Modifier.height(6.dp))
                NexusLyricLine(state = state)
                if (!isDesktop) {
                    Spacer(Modifier.height(10.dp))
                    NexusProgress(state = state, actions = actions)
                    Spacer(Modifier.height(10.dp))
                    NexusControls(state = state, actions = actions)
                    deviceVolumeController?.let { controller ->
                        Spacer(Modifier.height(14.dp))
                        NexusVolume(controller)
                    }
                }
                Spacer(Modifier.height(14.dp))
                NexusActionBar(state = state, actions = actions)
            }
        }
    }
}

// ---------------------------------------------------------------- header

@Composable
private fun NexusHeader(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NexusRoundButton(icon = state.dismissIcon, onClick = actions.onDismiss)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.now_playing),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                maxLines = 1,
            )
            val source = state.screenData.playlistName
            if (source.isNotBlank()) {
                Text(
                    text = source.uppercase(),
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp, color = NexusColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        NexusRoundButton(icon = SimpIcons.QueueMusic, onClick = actions.onShowQueue)
    }
}

@Composable
private fun NexusRoundButton(
    icon: ImageVector,
    size: Dp = 44.dp,
    tint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "nexusRoundPress")
    Box(
        modifier =
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.nexusGlass(CircleShape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = if (enabled) tint.alpha else 0.4f), modifier = Modifier.size(size * 0.45f))
    }
}

// ---------------------------------------------------------------- disc

/**
 * The cover cut to a circle, turning once per [DISC_ROTATION_MS] while playing and holding its angle
 * on pause, inside a violet gradient ring that glows. The halo behind it breathes while playing.
 * Feeds the shell's palette exactly as the other styles' artwork does.
 */
@Composable
private fun NexusDisc(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    size: Dp,
) {
    val isPlaying = state.controllerState.isPlaying
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            val from = rotation.value % 360f
            rotation.snapTo(from)
            rotation.animateTo(from + 360f, tween(DISC_ROTATION_MS, easing = LinearEasing))
        }
    }
    val playing by animateFloatAsState(if (isPlaying) 1f else 0f, tween(500), label = "nexusDiscPlaying")
    val breathe by rememberInfiniteTransition(label = "nexusDiscBreathe").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = SineInOut), RepeatMode.Reverse),
        label = "nexusDiscBreathePhase",
    )

    val ring = size + 16.dp
    Box(modifier = Modifier.size(ring + 56.dp), contentAlignment = Alignment.Center) {
        // Soft halos, drawn as radial light so they work without any blur support.
        Canvas(
            modifier =
                Modifier.size(ring + 56.dp).graphicsLayer {
                    val s = 1f + 0.05f * breathe * playing
                    scaleX = s
                    scaleY = s
                    alpha = 0.55f + 0.35f * breathe * playing
                },
        ) {
            drawCircle(
                Brush.radialGradient(
                    0.55f to NexusColors.AccentGlow.copy(alpha = 0.45f),
                    1f to Color.Transparent,
                ),
            )
        }
        // Glowing gradient ring.
        Box(
            modifier =
                Modifier
                    .size(ring)
                    .nexusGlow(CircleShape, NexusColors.AccentGlow, radius = 30.dp, intensity = 0.75f + 0.25f * playing)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                NexusColors.AccentPurple,
                                NexusColors.GlowSoft,
                                NexusColors.AccentGlow,
                                Color(0xFF4C1D95),
                                NexusColors.AccentPurple,
                            ),
                        ),
                    ),
        )
        // The turning disc.
        Box(
            modifier =
                Modifier
                    .size(size)
                    .graphicsLayer { rotationZ = rotation.value }
                    .clip(CircleShape)
                    .background(NexusColors.Gradient0),
            contentAlignment = Alignment.Center,
        ) {
            val url = state.screenData.thumbnailURL
            var artworkUrl by remember(url) { mutableStateOf(url) }
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(artworkUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artworkUrl + "BIGGER")
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberHolderPainter(square = true),
                error = rememberHolderPainter(square = true),
                onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                onError = {
                    val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                },
                modifier = Modifier.size(size - 8.dp).clip(CircleShape),
            )
            // Grooves, a specular sweep and the spindle, so the cover reads as a record.
            Canvas(modifier = Modifier.size(size)) {
                val r = this.size.minDimension / 2f
                listOf(0.78f, 0.6f, 0.42f).forEach { f ->
                    drawCircle(Color.White.copy(alpha = 0.14f), radius = r * f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                }
                drawCircle(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.5f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset(this.size.width, this.size.height),
                    ),
                )
                drawCircle(NexusColors.Gradient0.copy(alpha = 0.88f), radius = 17.dp.toPx())
                drawCircle(NexusColors.GlassBorder, radius = 17.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                drawCircle(NexusColors.TextMuted, radius = 4.dp.toPx())
            }
        }
    }
}

// ---------------------------------------------------------------- title, lyric line

private fun glowText(
    size: Int,
    weight: FontWeight,
    color: Color = Color.White,
    glow: Float = 16f,
) = TextStyle(
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    shadow = androidx.compose.ui.graphics.Shadow(color = NexusColors.AccentGlow.copy(alpha = 0.85f), offset = Offset.Zero, blurRadius = glow),
)

@Composable
private fun NexusTitleRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = glowText(26, FontWeight.ExtraBold),
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp).clickable(onClick = actions.onNavigateToArtist),
            ) {
                NexusEqualizer(playing = state.controllerState.isPlaying, height = 12.dp, barWidth = 2.5.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.screenData.artistName,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NexusColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        NexusLikeButton(liked = state.controllerState.isLiked) { actions.onUIEvent(UIEvent.ToggleLike) }
    }
}

/** The line being sung, when the lyrics are synced. Fixed height so nothing below it jumps. */
@Composable
private fun NexusLyricLine(state: NowPlayingContentState) {
    val lyrics = state.screenData.lyricsData?.lyrics
    val synced = lyrics?.lines != null && lyrics.syncType != null && lyrics.syncType != "UNSYNCED"
    val line =
        if (!synced || state.currentLyricLineIndex < 0) {
            ""
        } else {
            lyrics.lines
                ?.getOrNull(state.currentLyricLineIndex)
                ?.words
                ?.stripRichSyncTimestamps()
                .orEmpty()
        }
    Box(modifier = Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.CenterStart) {
        Crossfade(targetState = line, animationSpec = tween(300), label = "nexusLyricLine") { text ->
            Text(
                text = text,
                style = glowText(14, FontWeight.SemiBold, NexusColors.GlowSoft, glow = 10f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Four bars bouncing out of phase while playing; settling low on pause. */
@Composable
private fun NexusEqualizer(
    playing: Boolean,
    height: Dp,
    barWidth: Dp,
    bars: Int = 4,
) {
    val level by animateFloatAsState(if (playing) 1f else 0f, tween(300), label = "nexusEqLevel")
    val t by rememberInfiniteTransition(label = "nexusEq").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "nexusEqPhase",
    )
    Canvas(modifier = Modifier.size(width = barWidth * bars + barWidth * 0.9f * (bars - 1), height = height)) {
        val w = barWidth.toPx()
        val gap = w * 0.9f
        repeat(bars) { i ->
            val phase = (t + i * 0.23f) * 2f * PI.toFloat()
            val bounce = 0.55f + 0.45f * sin(phase * (1f + i * 0.37f)) * cos(phase * 0.5f)
            val frac = 0.2f + (bounce.coerceIn(0.1f, 1f) - 0.2f) * level
            val h = this.size.height * frac.coerceIn(0.15f, 1f)
            drawRoundRect(
                color = NexusColors.AccentGlow,
                topLeft = Offset(i * (w + gap), this.size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w, w),
            )
        }
    }
}

/** Heart in a glass circle that pops and throws a small burst of violet sparks when liked. */
@Composable
private fun NexusLikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
) {
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var lastLiked by remember { mutableStateOf(liked) }
    LaunchedEffect(liked) {
        if (liked == lastLiked) return@LaunchedEffect
        lastLiked = liked
        pop.snapTo(0.7f)
        if (liked) {
            burst.snapTo(0f)
            coroutineScope {
                launch { burst.animateTo(1f, tween(520)) }
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
            }
        } else {
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
    }
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val p = burst.value
            if (p >= 1f) return@Canvas
            val alpha = if (p < 0.15f) p / 0.15f else 1f - (p - 0.15f) / 0.85f
            repeat(8) { i ->
                val a = i / 8f * 2f * PI.toFloat()
                val d = this.size.minDimension * 0.42f * p
                drawCircle(
                    color = (if (i % 2 == 0) NexusColors.AccentGlow else NexusColors.GlowSoft).copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = 3.dp.toPx() * (1f - 0.5f * p),
                    center = center + Offset(cos(a) * d, sin(a) * d),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .then(if (liked) Modifier.nexusGlow(CircleShape, radius = 14.dp, intensity = 0.6f) else Modifier)
                    .nexusGlass(CircleShape, strong = liked)
                    .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (liked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = null,
                tint = if (liked) NexusColors.AccentGlow else Color.White.copy(alpha = 0.7f),
                modifier =
                    Modifier.size(22.dp).graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            )
        }
    }
}

// ---------------------------------------------------------------- progress

@Composable
private fun NexusProgress(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val total = state.timelineState.total
    Column {
        NexusSlider(
            value = (state.sliderValue / 100f).coerceIn(0f, 1f),
            enabled = total > 0L && !state.timelineState.isLive,
            onValueChange = { actions.onSliderChange(it * 100f) },
            onValueChangeFinished = actions.onSliderChangeFinished,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shownMs = if (total > 0L) (state.sliderValue / 100f * total).toLong() else state.timelineState.current.coerceAtLeast(0L)
            Text(
                text = if (state.timelineState.isLive) "" else formatDuration(shownMs),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NexusColors.TextMuted),
                modifier = Modifier.width(48.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    SimpIcons.Replay5,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp).clip(CircleShape).clickable { actions.onUIEvent(UIEvent.Backward) },
                )
                Icon(
                    SimpIcons.Forward5,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp).clip(CircleShape).clickable { actions.onUIEvent(UIEvent.Forward) },
                )
            }
            Text(
                text = state.timelineState.lengthLabel(),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NexusColors.TextMuted, textAlign = TextAlign.End),
                modifier = Modifier.width(48.dp),
            )
        }
    }
}

/**
 * Gradient-filled track with a glowing knob. Tap or drag anywhere on it; the knob grows and its glow
 * brightens while the finger is down.
 */
@Composable
private fun NexusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackHeight: Dp = 4.dp,
    knobSize: Dp = 14.dp,
) {
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    var active by remember { mutableStateOf(false) }
    val activeLevel by animateFloatAsState(if (active) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "nexusSliderActive")
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(maxOf(28.dp, knobSize * 2.4f))
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            fun fraction(x: Float) = (x / size.width.toFloat()).coerceIn(0f, 1f)
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                active = true
                                change(fraction(down.position.x))
                                down.consume()
                                horizontalDrag(down.id) { drag ->
                                    change(fraction(drag.position.x))
                                    drag.consume()
                                }
                                active = false
                                finished()
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        val cy = size.height / 2f
        val th = trackHeight.toPx()
        val x = size.width * value
        drawRoundRect(NexusColors.Track, Offset(0f, cy - th / 2f), Size(size.width, th), CornerRadius(th, th))
        if (x > 0f) {
            // Glow under the fill, then the fill itself.
            drawRoundRect(
                NexusColors.AccentGlow.copy(alpha = 0.35f),
                Offset(0f, cy - th * 1.5f),
                Size(x, th * 3f),
                CornerRadius(th * 1.5f, th * 1.5f),
            )
            drawRoundRect(
                Brush.horizontalGradient(NexusColors.accentGradient, startX = 0f, endX = x.coerceAtLeast(1f)),
                Offset(0f, cy - th / 2f),
                Size(x, th),
                CornerRadius(th, th),
            )
        }
        val knob = knobSize.toPx() / 2f * (1f + 0.35f * activeLevel)
        val haloR = knobSize.toPx() * (1.1f + 0.6f * activeLevel)
        drawCircle(
            Brush.radialGradient(
                0f to NexusColors.AccentGlow.copy(alpha = 0.55f + 0.35f * activeLevel),
                1f to Color.Transparent,
                center = Offset(x, cy),
                radius = haloR,
            ),
            radius = haloR,
            center = Offset(x, cy),
        )
        drawCircle(Color.White, radius = knob, center = Offset(x, cy))
    }
}

// ---------------------------------------------------------------- controls

@Composable
private fun NexusControls(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val controls = state.controllerState
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NexusSmallControl(SimpIcons.Shuffle, active = controls.isShuffle, size = 22.dp) { actions.onUIEvent(UIEvent.Shuffle) }
        NexusSmallControl(SimpIcons.SkipPrevious, enabled = controls.isPreviousAvailable, size = 34.dp) { actions.onUIEvent(UIEvent.Previous) }
        NexusPlayButton(isPlaying = controls.isPlaying) { actions.onUIEvent(UIEvent.PlayPause) }
        NexusSmallControl(SimpIcons.SkipNext, enabled = controls.isNextAvailable, size = 34.dp) { actions.onUIEvent(UIEvent.Next) }
        NexusSmallControl(
            icon = if (controls.repeatState == RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
            active = controls.repeatState != RepeatState.None,
            size = 22.dp,
        ) { actions.onUIEvent(UIEvent.Repeat) }
    }
}

@Composable
private fun NexusSmallControl(
    icon: ImageVector,
    size: Dp,
    active: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.82f else 1f, spring(dampingRatio = 0.45f, stiffness = 420f), label = "nexusSmallPress")
    val tint =
        when {
            !enabled -> Color.White.copy(alpha = 0.35f)
            active == true -> NexusColors.GlowSoft
            active == false -> Color.White.copy(alpha = 0.7f)
            else -> Color.White
        }
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier =
                Modifier.size(size).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        if (active == true) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .nexusGlow(CircleShape, radius = 6.dp, intensity = 1f)
                    .background(NexusColors.AccentGlow, CircleShape),
            )
        }
    }
}

/** Glass circle with a pulsing violet glow, a tap ripple and the morphing play/pause glyph. */
@Composable
private fun NexusPlayButton(
    isPlaying: Boolean,
    size: Dp = 78.dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.4f, stiffness = 380f), label = "nexusPlayPress")
    val playing by animateFloatAsState(if (isPlaying) 1f else 0f, tween(400), label = "nexusPlayLevel")
    val breathe by rememberInfiniteTransition(label = "nexusPlayBreathe").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = SineInOut), RepeatMode.Reverse),
        label = "nexusPlayBreathePhase",
    )
    val ripple = remember { Animatable(1f) }
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        ripple.snapTo(0f)
        ripple.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    Box(
        modifier =
            Modifier
                .size(size + 24.dp)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }.clickable(interactionSource = interaction, indication = null) {
                    taps++
                    onClick()
                },
        contentAlignment = Alignment.Center,
    ) {
        // Ripple ring.
        Canvas(modifier = Modifier.size(size)) {
            val p = ripple.value
            if (p < 1f) {
                drawCircle(
                    Color.White.copy(alpha = (0.55f * (1f - p)).coerceIn(0f, 1f)),
                    radius = this.size.minDimension / 2f * (1f + 0.7f * p),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .size(size)
                    .nexusGlow(CircleShape, NexusColors.AccentGlow, radius = 30.dp, intensity = 0.65f + 0.35f * breathe * playing + 0.1f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexusColors.AccentPurple.copy(alpha = 0.75f), NexusColors.AccentGlow.copy(alpha = 0.55f))))
                    .border(1.dp, NexusColors.GlassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MorphingPlayPauseIcon(isPlaying = isPlaying, color = Color.White, modifier = Modifier.size(size * 0.36f))
        }
    }
}

// ---------------------------------------------------------------- volume, action bar

@Composable
private fun NexusVolume(controller: DeviceVolumeController) {
    Row(
        modifier = Modifier.fillMaxWidth().nexusGlass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(SimpIcons.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        NexusSlider(
            value = controller.volumeFraction,
            onValueChange = { controller.setVolumeFraction(it) },
            onValueChangeFinished = {},
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            trackHeight = 3.dp,
            knobSize = 12.dp,
        )
        Icon(SimpIcons.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun NexusActionBar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .nexusGlass(RoundedCornerShape(26.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NexusBarIcon(SimpIcons.Lyrics, enabled = state.screenData.lyricsData != null, onClick = actions.onShowFullscreenLyrics)
        NexusBarIcon(SimpIcons.Info, onClick = actions.onShowInfo)
        NexusBarIcon(SimpIcons.PlaylistAdd, onClick = actions.onShowAddToPlaylist)
        NexusBarIcon(SimpIcons.MoreVert, onClick = actions.onShowMoreSheet)
    }
}

@Composable
private fun NexusBarIcon(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxHeight().width(56.dp).clip(RoundedCornerShape(20.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = if (enabled) 0.8f else 0.3f), modifier = Modifier.size(22.dp))
    }
}

private val SineInOut = Easing { x -> -(cos(PI.toFloat() * x) - 1f) / 2f }

private const val DISC_ROTATION_MS = 24_000
