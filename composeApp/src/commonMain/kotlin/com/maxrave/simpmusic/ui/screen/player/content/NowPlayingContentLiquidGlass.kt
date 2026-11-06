package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.rememberDeviceVolumeController
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.glass.GlassShapes
import com.maxrave.simpmusic.ui.component.glass.GlassSurface
import com.maxrave.simpmusic.ui.component.glass.glassSource
import com.maxrave.simpmusic.ui.component.glass.rememberGlassState
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.FastForward
import com.maxrave.simpmusic.ui.icon.FastRewind
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicLyricsView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicMesh
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicOutputSheet
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicQueueView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicThinSlider
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicTimesRow
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicView
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicVolumeRow
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicPressInflate
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.displayName
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicMesh
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.UIEvent
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stored value of the Liquid Glass Now Playing style. Lives here rather than beside the other three
 * in core's DataStoreManager so this fork does not have to change the core submodule: the setting
 * stores whatever string it is handed.
 */
const val NOW_PLAYING_STYLE_LIQUID_GLASS = "LIQUID_GLASS"

/**
 * The Liquid Glass Now Playing style: the cover over a slowly drifting mesh of its own colours,
 * with the controls on a [GlassSurface] panel and three glass buttons (Lyrics · Output · Queue).
 * Lyrics and Queue open as glass sheets over the same backdrop, reusing the Apple Music style's
 * lyrics and queue views as they are.
 *
 * Choosing this style IS choosing glass, so it turns [LocalLiquidGlassEnabled] on for its own
 * subtree whatever the app-wide Liquid Glass setting says. Where glass would not fit it hands the
 * whole page to the Classic layout instead: a Spotify canvas or a playing video (both fill the page
 * and auto-hide the controls), a landscape page and picture-in-picture.
 */
@Composable
fun NowPlayingContentLiquidGlass(
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
        // Crossfaded rather than swapped: a canvas usually arrives a few seconds into the track, and a
        // hard cut from glass to Classic under the user's eyes reads as a glitch.
        Crossfade(targetState = useClassic, animationSpec = tween(350), label = "liquidGlassFallback") { classic ->
            if (classic) {
                NowPlayingContentSpotify(state = state, actions = actions)
            } else {
                CompositionLocalProvider(LocalLiquidGlassEnabled provides true) {
                    LiquidGlassPlayer(state = state, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassPlayer(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // Same toolbar rule as the Apple Music style: no scroll, so the shared flag stays parked.
    LaunchedEffect(Unit) { actions.onToolbarVisibilityChange(false) }

    val density = LocalDensity.current
    val isDesktop = getPlatform() == Platform.Desktop
    val typography = rememberAppleMusicTypography()
    val deviceVolumeController = rememberDeviceVolumeController()

    var sheet by rememberSaveable { mutableStateOf(AppleMusicView.MAIN) }
    // Lyrics can vanish mid-session (provider offline, a track without any); the sheet closes rather
    // than standing empty. Waits out the normal gap while the next track's lyrics are fetched, as the
    // Apple Music style does, so a skip does not throw the user out of the sheet.
    LaunchedEffect(state.screenData.lyricsData, sheet) {
        if (sheet != AppleMusicView.LYRICS || state.screenData.lyricsData != null) return@LaunchedEffect
        kotlinx.coroutines.delay(LYRICS_ABSENCE_GRACE_MS)
        sheet = AppleMusicView.MAIN
    }
    PlatformBackHandler(enabled = sheet != AppleMusicView.MAIN) { sheet = AppleMusicView.MAIN }

    val paletteColor = state.startColor.value
    val seedColor = if (paletteColor == Color.Black) seed else paletteColor
    val activePillContainer = remember(seedColor) { lerp(seedColor, Color.White, 0.75f) }
    val activePillContent = remember(seedColor) { lerp(seedColor, Color.Black, 0.6f) }

    val player = koinInject<MediaPlayerHandler>().player
    val audioOutputs by player.audioOutputs.collectAsStateWithLifecycle()
    val activeOutputName = audioOutputs.firstOrNull { it.isActive }?.displayName()
    val outputName = if (state.castState.isRemote) state.castState.deviceName else activeOutputName
    var showOutputSheet by rememberSaveable { mutableStateOf(false) }

    // The refraction layer is only worth recording because the buttons below ask for it.
    val glass = rememberGlassState(refraction = true, refractionBackground = Color.Black)
    val mesh =
        rememberAppleMusicMesh(
            artwork = state.screenData.bitmap,
            seed = state.screenData.thumbnailURL.hashCode(),
        )

    // Mini player → full player: the sheet slides the page up, and the cover and panel settle into
    // place on top of that slide rather than arriving already still, so the expansion reads as one
    // motion. Runs once per opening (the page is disposed when the player closes).
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) }

    // A sheet open pushes MAIN back a little, the way iOS recedes the page under a sheet.
    val recede by animateFloatAsState(
        targetValue = if (sheet == AppleMusicView.MAIN) 0f else 1f,
        animationSpec = tween(320),
        label = "liquidGlassRecede",
    )

    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val systemBottom = with(density) { WindowInsets.systemBars.getBottom(density).toDp() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // The source every glass surface on this page blurs. Everything else is its sibling.
        Box(modifier = Modifier.matchParentSize().glassSource(glass)) {
            LiquidGlassMeshBackdrop(mesh = mesh, modifier = Modifier.matchParentSize())
        }

        // MAIN
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = lerp(1f, 0.94f, recede)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(1f, 0.45f, recede)
                    }.padding(top = statusBarTop + TOP_CHROME, bottom = systemBottom + 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The cover takes whatever height the panel leaves, as a square.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                val coverScale = lerp(0.86f, 1f, entrance.value)
                // A paused cover sits back a step, as Apple Music's does.
                val playScale by animateFloatAsState(
                    targetValue = if (state.controllerState.isPlaying) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                    label = "liquidGlassCoverPlayScale",
                )
                LiquidGlassArtworkPager(
                    state = state,
                    actions = actions,
                    modifier =
                        Modifier
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .graphicsLayer {
                                scaleX = coverScale * playScale
                                scaleY = coverScale * playScale
                                alpha = entrance.value
                            },
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier =
                    Modifier.graphicsLayer {
                        translationY = (1f - entrance.value) * 48.dp.toPx()
                        alpha = entrance.value
                    },
            ) {
                GlassSurface(
                    state = glass,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = GlassShapes.Large,
                    blurRadius = 40.dp,
                ) {
                    Column(modifier = Modifier.padding(vertical = 18.dp)) {
                        AppleMusicMainTitleRow(state = state, actions = actions, typography = typography)
                        // The other styles leave playback to the Desktop shell's own player bar.
                        if (!isDesktop) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
                                    AppleMusicThinSlider(
                                        value = state.sliderValue / 100f,
                                        activeColor = Color.White,
                                        onValueChange = { actions.onSliderChange(it * 100f) },
                                        onValueChangeFinished = actions.onSliderChangeFinished,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                AppleMusicTimesRow(state = state, typography = typography, modifier = Modifier.padding(top = 8.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                LiquidGlassTransportRow(state = state, actions = actions)
                                deviceVolumeController?.let { controller ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    AppleMusicVolumeRow(controller = controller)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LiquidGlassActionButton(
                        glass = glass,
                        icon = SimpIcons.Lyrics,
                        enabled = state.screenData.lyricsData != null,
                        modifier = Modifier.weight(1f),
                        onClick = { sheet = AppleMusicView.LYRICS },
                    )
                    LiquidGlassActionButton(
                        glass = glass,
                        icon = SimpIcons.Headphones,
                        modifier = Modifier.weight(1f),
                        onClick = { showOutputSheet = true },
                    )
                    LiquidGlassActionButton(
                        glass = glass,
                        icon = SimpIcons.QueueMusic,
                        modifier = Modifier.weight(1f),
                        onClick = { sheet = AppleMusicView.QUEUE },
                    )
                }
            }
        }

        // Grabber (Android, where the page is a sheet) or a glass close button (Desktop side panel).
        if (isDesktop) {
            GlassSurface(
                state = glass,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { actions.onDismiss() },
                shape = CircleShape,
                shadow = false,
                contentAlignment = Alignment.Center,
            ) {
                Icon(state.dismissIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = statusBarTop)
                        .size(width = 64.dp, height = TOP_CHROME)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            actions.onDismiss()
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 36.dp, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.4f)),
                )
            }
        }

        // Lyrics / Queue as glass sheets over the backdrop. The Apple Music views are used as they
        // are, header, list and bottom dock included; their dock toggles back to MAIN.
        AnimatedVisibility(
            visible = sheet != AppleMusicView.MAIN,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Remembered past the close, so the sheet animates out with what it showed. A plain
            // holder, not state: it is written during composition and must not trigger another one.
            val lastSheet = remember { arrayOf(AppleMusicView.LYRICS) }
            if (sheet != AppleMusicView.MAIN) lastSheet[0] = sheet
            val shown = lastSheet[0]
            GlassSurface(
                state = glass,
                modifier = Modifier.fillMaxSize().padding(top = statusBarTop + SHEET_TOP_GAP),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                blurRadius = 48.dp,
                shadow = false,
            ) {
                val selectView: (AppleMusicView) -> Unit = { sheet = it }
                when (shown) {
                    AppleMusicView.QUEUE ->
                        AppleMusicQueueView(
                            state = state,
                            actions = actions,
                            typography = typography,
                            viewState = AppleMusicView.QUEUE,
                            onSelectView = selectView,
                            activePillContainer = activePillContainer,
                            activePillContent = activePillContent,
                            deviceVolumeController = deviceVolumeController,
                            outputName = outputName,
                            onOpenOutput = { showOutputSheet = true },
                        )

                    else ->
                        AppleMusicLyricsView(
                            state = state,
                            actions = actions,
                            typography = typography,
                            viewState = AppleMusicView.LYRICS,
                            onSelectView = selectView,
                            activePillContainer = activePillContainer,
                            activePillContent = activePillContent,
                            deviceVolumeController = deviceVolumeController,
                            outputName = outputName,
                            onOpenOutput = { showOutputSheet = true },
                        )
                }
                // Tap target over the sheet's top edge, so it closes like the page's own grabber.
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 64.dp, height = 20.dp)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                sheet = AppleMusicView.MAIN
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(width = 36.dp, height = 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.4f)),
                    )
                }
            }
        }

        if (showOutputSheet) {
            AppleMusicOutputSheet(
                outputs = audioOutputs,
                castState = state.castState,
                deviceVolumeController = deviceVolumeController,
                onSelectOutput = { output -> player.selectAudioOutput(output.id) },
                onRefresh = { player.refreshAudioOutputs() },
                onDismiss = { showOutputSheet = false },
            )
        }
    }
}

/**
 * The cover, swipeable through the queue. Bound to the shell's pager state, so a swipe seeks
 * exactly as it does in the other styles (the shell owns that logic). The current page loads the
 * full-size cover and feeds [NowPlayingContentActions.onArtworkBitmap], which drives the palette and
 * the mesh behind the page; other pages show the queue's own thumbnail.
 */
@Composable
private fun LiquidGlassArtworkPager(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = state.artworkPagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
    ) { page ->
        val isCurrentPage = page == state.currentOrderIndex
        val url =
            if (isCurrentPage) {
                state.screenData.thumbnailURL
            } else {
                state.artworkQueue
                    .getOrNull(page)
                    ?.thumbnails
                    ?.maxByOrNull { it.width * it.height }
                    ?.url
            }
        var artworkUrl by remember(url) { mutableStateOf(url) }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .dropShadow(
                        GlassShapes.Large,
                        Shadow(radius = 32.dp, color = Color.Black.copy(alpha = 0.45f), offset = DpOffset(0.dp, 14.dp)),
                    ).clip(GlassShapes.Large),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(artworkUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artworkUrl + if (isCurrentPage) "BIGGER" else "")
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberHolderPainter(square = true),
                error = rememberHolderPainter(square = true),
                onSuccess = { if (isCurrentPage) actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                onError = {
                    val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The artwork's own colours (the Apple Music style's mesh texture) drifting slowly behind the page:
 * two copies of the texture, scaled past the edges and turning against each other on a long loop,
 * under a heavy blur. The drift is read in the draw phase, so it only ever redraws, never recomposes.
 * A new track's mesh crossfades in over the old one.
 */
@Composable
private fun LiquidGlassMeshBackdrop(
    mesh: AppleMusicMesh?,
    modifier: Modifier = Modifier,
) {
    val drift by rememberInfiniteTransition(label = "liquidGlassMeshDrift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(MESH_DRIFT_PERIOD_MS, easing = LinearEasing)),
        label = "liquidGlassMeshPhase",
    )
    Crossfade(targetState = mesh, animationSpec = tween(900), label = "liquidGlassMesh", modifier = modifier) { shown ->
        Canvas(modifier = Modifier.fillMaxSize().background(MeshFallback).blur(MESH_BLUR)) {
            val texture = shown?.image ?: return@Canvas
            val angle = drift * 2f * PI.toFloat()
            val dst = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            val src = IntSize(texture.width, texture.height)
            drawWithLayerTransform(
                translate = Offset(cos(angle) * size.width * 0.06f, sin(angle) * size.height * 0.04f),
                scale = 1.3f + 0.08f * sin(angle),
                rotation = 8f * sin(angle),
            ) {
                drawImage(texture, IntOffset.Zero, src, IntOffset.Zero, dst, filterQuality = FilterQuality.Low)
            }
            drawWithLayerTransform(
                translate = Offset(-sin(angle) * size.width * 0.08f, cos(angle) * size.height * 0.05f),
                scale = 1.5f + 0.1f * cos(angle),
                rotation = 180f + 12f * cos(angle),
            ) {
                drawImage(texture, IntOffset.Zero, src, IntOffset.Zero, dst, alpha = 0.55f, filterQuality = FilterQuality.Low)
            }
            // A floor of darkness for the white text on the glass, more of it towards the bottom
            // where the panel sits. The panel's own tint carries the rest of the contrast.
            drawRect(Color.Black.copy(alpha = 0.18f))
        }
    }
}

private fun DrawScope.drawWithLayerTransform(
    translate: Offset,
    scale: Float,
    rotation: Float,
    block: DrawScope.() -> Unit,
) {
    val pivot = Offset(size.width / 2f, size.height / 2f)
    withTransform(
        {
            translate(translate.x, translate.y)
            rotate(rotation, pivot)
            scale(scale, scale, pivot)
        },
        block,
    )
}

/** Previous · play/pause (morphing) · next. */
@Composable
private fun LiquidGlassTransportRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val controls = state.controllerState
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportGlyph(
            icon = SimpIcons.FastRewind,
            enabled = controls.isPreviousAvailable,
            onClick = { actions.onUIEvent(UIEvent.Previous) },
        )
        Box(
            modifier =
                Modifier
                    .appleMusicPressInflate()
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { actions.onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            MorphingPlayPauseIcon(isPlaying = controls.isPlaying, color = Color.White, modifier = Modifier.size(44.dp))
        }
        TransportGlyph(
            icon = SimpIcons.FastForward,
            enabled = controls.isNextAvailable,
            onClick = { actions.onUIEvent(UIEvent.Next) },
        )
    }
}

@Composable
private fun TransportGlyph(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .appleMusicPressInflate()
                .size(52.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(40.dp))
    }
}

/**
 * Play ▶ morphing into pause ❚❚ and back. Each half of the triangle is a quad whose corners slide to
 * the corners of one pause bar, so the glyph reshapes instead of crossfading between two pictures.
 * Overshoots a touch on the way, as a spring does.
 */
@Composable
internal fun MorphingPlayPauseIcon(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "playPauseMorph",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Play: a triangle pointing right, split at its vertical midline into two quads.
        // The first quad's right edge and the second quad's left edge meet at x = mid.
        val left = w * 0.18f
        val tip = w * 0.92f
        val mid = (left + tip) / 2f
        val midTop = h * 0.5f - (h * 0.42f) * (tip - mid) / (tip - left)
        val midBottom = h - midTop
        val play1 = listOf(Offset(left, h * 0.08f), Offset(mid, midTop), Offset(mid, midBottom), Offset(left, h * 0.92f))
        val play2 = listOf(Offset(mid, midTop), Offset(tip, h * 0.5f), Offset(tip, h * 0.5f), Offset(mid, midBottom))
        // Pause: two bars.
        val barW = w * 0.26f
        val gap = w * 0.16f
        val x1 = (w - barW * 2 - gap) / 2f
        val x2 = x1 + barW + gap
        val top = h * 0.1f
        val bottom = h * 0.9f
        val pause1 = listOf(Offset(x1, top), Offset(x1 + barW, top), Offset(x1 + barW, bottom), Offset(x1, bottom))
        val pause2 = listOf(Offset(x2, top), Offset(x2 + barW, top), Offset(x2 + barW, bottom), Offset(x2, bottom))
        // Turns a quarter as it morphs, so the motion has a direction.
        rotate(degrees = 90f * (1f - progress)) {
            listOf(play1 to pause1, play2 to pause2).forEach { (from, to) ->
                val path = Path()
                from.indices.forEach { i ->
                    val p = androidx.compose.ui.geometry.lerp(from[i], to[i], progress)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, color)
            }
        }
    }
}

/** One of the three glass buttons under the panel. Refracting where the platform allows it. */
@Composable
private fun LiquidGlassActionButton(
    glass: com.maxrave.simpmusic.ui.component.glass.GlassState,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    GlassSurface(
        state = glass,
        modifier =
            modifier
                .height(52.dp)
                .appleMusicPressInflate(pressedScale = 1.08f)
                .clip(GlassShapes.Pill)
                .clickable(enabled = enabled, onClick = onClick),
        shape = GlassShapes.Pill,
        refraction = true,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(24.dp))
    }
}

/** Grabber strip on Android / room for the close button on Desktop, above the cover. */
private val TOP_CHROME: Dp = 28.dp

/** Gap between the status bar and a lyrics/queue sheet's top edge, where the page shows through. */
private val SHEET_TOP_GAP: Dp = 24.dp

private const val LYRICS_ABSENCE_GRACE_MS = 3_000L
private const val MESH_DRIFT_PERIOD_MS = 28_000
private val MESH_BLUR: Dp = 48.dp
private val MeshFallback = Color(0xFF16161A)
