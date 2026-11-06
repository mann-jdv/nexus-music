package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.simpmusic.ui.component.playRadio
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Notifications
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.NexusColors
import com.maxrave.simpmusic.ui.theme.nexusGlass
import com.maxrave.simpmusic.ui.theme.nexusGlow
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.good_afternoon
import simpmusic.composeapp.generated.resources.good_evening
import simpmusic.composeapp.generated.resources.good_morning
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.guest
import simpmusic.composeapp.generated.resources.nexus_start_listening

/*
 * Home in the Nexus Violet layout, ported from the Expo prototype's `app/(tabs)/home.tsx`: a greeting
 * header with glass buttons, glowing category pills, and a "new collection" card picked from the real
 * feed. Every shelf below that is the feed YouTube Music sends, drawn by Home's existing shelf code.
 * HomeScreen switches these in only while [com.maxrave.simpmusic.ui.theme.LocalNexusTheme] is on.
 */

private val glowTitle =
    TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(color = NexusColors.AccentGlow.copy(alpha = 0.85f), offset = Offset.Zero, blurRadius = 14f),
    )

/** Avatar (with the account menu) · greeting and name · search · notifications · Create. */
@Composable
internal fun NexusHomeHeaderRow(
    accountName: String?,
    thumbnailUrl: String?,
    navController: NavController,
    createActions: @Composable () -> Unit,
) {
    val signedIn = !accountName.isNullOrBlank()
    val hour = remember { now().time.hour }
    val greeting =
        stringResource(
            when (hour) {
                in 6..12 -> Res.string.good_morning
                in 13..17 -> Res.string.good_afternoon
                in 18..23 -> Res.string.good_evening
                else -> Res.string.good_night
            },
        )
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.nexusGlow(CircleShape, radius = 14.dp, intensity = 0.7f)) {
            HomeAccountButton(accountName = accountName, thumbnailUrl = thumbnailUrl, navController = navController)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = "$greeting!",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NexusColors.TextMuted),
                maxLines = 1,
            )
            Text(
                text = if (signedIn) accountName.orEmpty() else stringResource(Res.string.guest),
                style = glowTitle.copy(fontSize = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        NexusHomeIconButton(SimpIcons.Search) { navController.navigate(SearchDestination) }
        Spacer(Modifier.width(8.dp))
        NexusHomeIconButton(SimpIcons.Notifications) { navController.navigate(NotificationDestination) }
        Spacer(Modifier.width(8.dp))
        createActions()
    }
}

@Composable
private fun NexusHomeIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "nexusHomeIconPress")
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.nexusGlass(CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/**
 * "All" plus every mood as its own pill, instead of the All + expandable Mood chip pair. The selected
 * pill is the violet gradient with a glow; the rest are glass.
 */
@Composable
internal fun NexusMoodPills(
    moods: List<Pair<String, String>>,
    selectedParams: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NexusPill(text = stringResource(Res.string.all), selected = selectedParams == null) { onSelect(null) }
        moods.forEach { (label, params) ->
            NexusPill(text = label, selected = selectedParams == params) { onSelect(params) }
        }
    }
}

@Composable
private fun NexusPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "nexusPill")
    Box(
        modifier =
            Modifier
                .height(38.dp)
                .graphicsLayer {
                    val s = 1f + 0.04f * lift
                    scaleX = s
                    scaleY = s
                }.then(
                    if (selected) {
                        Modifier
                            .nexusGlow(shape, radius = 16.dp, intensity = 0.75f)
                            .clip(shape)
                            .background(Brush.linearGradient(NexusColors.accentGradient))
                    } else {
                        Modifier.nexusGlass(shape)
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else NexusColors.TextMuted,
                ),
            maxLines = 1,
        )
    }
}

/**
 * The prototype's "New Collections" card, filled from the feed: the first playlist or album any shelf
 * carries (a song when there is none), with that shelf's title as the kicker.
 */
internal fun List<HomeItem>.nexusFeatured(): Pair<HomeItem, Content>? {
    val collections =
        asSequence().flatMap { shelf -> shelf.contents.asSequence().filterNotNull().map { shelf to it } }
    return collections.firstOrNull { (_, c) ->
        c.videoId.isNullOrEmpty() &&
            (
                (!c.playlistId.isNullOrEmpty() && !c.playlistId!!.startsWith("UC")) ||
                    (!c.browseId.isNullOrEmpty() && !c.browseId!!.startsWith("UC"))
            )
    } ?: collections.firstOrNull { (_, c) -> !c.videoId.isNullOrEmpty() }
}

/** Opens a feed item the way Home's own cards do: a song or video starts its radio, anything else its page. */
internal fun Content.openFromHome(
    navController: NavController,
    viewModel: BaseViewModel,
) {
    val playlist = playlistId
    val browse = browseId
    when {
        videoId.isNullOrEmpty() && !playlist.isNullOrEmpty() ->
            if (playlist.startsWith("UC")) {
                navController.navigate(ArtistDestination(channelId = playlist))
            } else {
                navController.navigate(PlaylistDestination(playlistId = playlist))
            }

        videoId.isNullOrEmpty() && !browse.isNullOrEmpty() ->
            when {
                browse.startsWith("UC") -> navController.navigate(ArtistDestination(channelId = browse))
                browse.startsWith("MPSP") -> navController.navigate(PodcastDestination(podcastId = browse))
                else -> navController.navigate(AlbumDestination(browseId = browse))
            }

        else -> playRadio(viewModel)
    }
}

@Composable
internal fun NexusFeaturedCard(
    shelf: HomeItem,
    content: Content,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .nexusGlow(shape, NexusColors.AccentPurple, radius = 26.dp, intensity = 0.45f)
                .nexusGlass(shape)
                .background(
                    Brush.linearGradient(
                        listOf(NexusColors.AccentPurple.copy(alpha = 0.55f), NexusColors.AccentGlow.copy(alpha = 0.22f), Color.Transparent),
                    ),
                    shape,
                ).clickable(onClick = onOpen)
                .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = shelf.title.uppercase(),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp, color = NexusColors.GlowSoft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = content.title, style = glowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val subtitle =
                    content.description?.takeIf { it.isNotBlank() }
                        ?: content.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(fontSize = 13.sp, color = NexusColors.TextMuted, lineHeight = 18.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier =
                        Modifier
                            .nexusGlow(RoundedCornerShape(50), radius = 16.dp, intensity = 0.6f)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(SimpIcons.PlayArrow, contentDescription = null, tint = NexusColors.AccentPurple, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(Res.string.nexus_start_listening),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NexusColors.AccentPurple),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            val art = content.thumbnails.maxByOrNull { it.width * it.height }?.url
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(art)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(art)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberHolderPainter(square = true),
                error = rememberHolderPainter(square = true),
                modifier =
                    Modifier
                        .size(112.dp)
                        .nexusGlow(CircleShape, NexusColors.AccentGlow, radius = 24.dp, intensity = 0.8f)
                        .clip(CircleShape),
            )
        }
    }
}
