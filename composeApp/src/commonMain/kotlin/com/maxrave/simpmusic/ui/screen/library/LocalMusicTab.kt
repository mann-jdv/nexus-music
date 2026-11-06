package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.domain.data.entities.LocalAlbum
import com.maxrave.domain.data.entities.LocalArtist
import com.maxrave.domain.data.entities.LocalFolderEntity
import com.maxrave.domain.data.entities.LocalScanResult
import com.maxrave.domain.data.entities.LocalTrackEntity
import com.maxrave.domain.data.entities.qualityLabel
import com.maxrave.domain.data.entities.toSongEntity
import com.maxrave.simpmusic.expect.ui.rememberFolderPicker
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.glass.glassTile
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.RestartAlt
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LocalMusicViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.local_add_folder
import simpmusic.composeapp.generated.resources.local_empty
import simpmusic.composeapp.generated.resources.local_folder_unreadable
import simpmusic.composeapp.generated.resources.local_folders
import simpmusic.composeapp.generated.resources.local_music
import simpmusic.composeapp.generated.resources.local_rescan
import simpmusic.composeapp.generated.resources.local_scan_result
import simpmusic.composeapp.generated.resources.local_scanning
import simpmusic.composeapp.generated.resources.local_track_count
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.songs

private enum class LocalSection { SONGS, ALBUMS, ARTISTS, FOLDERS }

/** A drill-down into one album, artist or folder; null shows the section itself. */
private sealed interface LocalDetail {
    data class Album(
        val album: LocalAlbum,
    ) : LocalDetail

    data class Artist(
        val artist: LocalArtist,
    ) : LocalDetail

    data class Folder(
        val folder: LocalFolderEntity,
    ) : LocalDetail
}

/**
 * Library › Local: the user's own files, as Songs / Albums / Artists / Folders. Tapping a song plays
 * the list it is in through the shared queue, so local songs and YouTube songs mix freely (the song
 * menu adds a local song to whatever is already queued).
 */
@Composable
fun LocalMusicTab(
    navController: NavController,
    contentPadding: PaddingValues,
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: LocalMusicViewModel = koinViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var section by rememberSaveable { mutableStateOf(LocalSection.SONGS) }
    var detail by remember { mutableStateOf<LocalDetail?>(null) }
    var menuFor by remember { mutableStateOf<LocalTrackEntity?>(null) }
    val scope = rememberCoroutineScope()
    val localName = stringResource(Res.string.local_music)

    val reportScan: (LocalScanResult?) -> Unit = { result ->
        scope.launch {
            val text =
                if (result == null) {
                    getString(Res.string.local_folder_unreadable)
                } else {
                    getString(Res.string.local_scan_result, (result.added + result.updated).toString(), result.removed.toString())
                }
            showToast(text, ToastGravity.Bottom)
        }
    }
    val pickFolder = rememberFolderPicker { uri -> viewModel.addFolder(uri, reportScan) }

    Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (folders.isEmpty()) {
            EmptyLocal(onAddFolder = pickFolder)
            return@Column
        }

        val current = detail
        if (current == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LocalSection.entries.forEach { s ->
                    FilterChip(
                        selected = section == s,
                        onClick = { section = s },
                        label = {
                            Text(
                                when (s) {
                                    LocalSection.SONGS -> stringResource(Res.string.songs)
                                    LocalSection.ALBUMS -> stringResource(Res.string.albums)
                                    LocalSection.ARTISTS -> stringResource(Res.string.artists)
                                    LocalSection.FOLDERS -> stringResource(Res.string.local_folders)
                                },
                            )
                        },
                    )
                }
            }
        } else {
            val title =
                when (current) {
                    is LocalDetail.Album -> current.album.name
                    is LocalDetail.Artist -> current.artist.name
                    is LocalDetail.Folder -> current.folder.name
                }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RippleIconButton(SimpIcons.ArrowBackIosNew, Modifier.size(32.dp), true, tint = MaterialTheme.colorScheme.onSurface) { detail = null }
                Text(title, style = typo().titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }

        val bottom = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)
        when {
            current != null -> {
                val flow =
                    remember(current) {
                        when (current) {
                            is LocalDetail.Album -> viewModel.tracksOfAlbum(current.album)
                            is LocalDetail.Artist -> viewModel.tracksOfArtist(current.artist)
                            is LocalDetail.Folder -> viewModel.tracksInFolder(current.folder)
                        }
                    }
                val list by (flow ?: emptyFlow()).collectAsStateWithLifecycle(emptyList())
                LocalSongList(
                    list = list,
                    name = localName,
                    viewModel = viewModel,
                    padding = bottom,
                    onScrolling = onScrolling,
                    onMore = { menuFor = it },
                )
            }

            section == LocalSection.SONGS ->
                LocalSongList(tracks, localName, viewModel, bottom, onScrolling, onMore = { menuFor = it })

            section == LocalSection.ALBUMS -> {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    state = gridState,
                    contentPadding = bottom,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                ) {
                    items(albums, key = { it.name + "\u0000" + it.artist }) { album ->
                        Column(
                            modifier =
                                Modifier
                                    .glassTile()
                                    .clickable { detail = LocalDetail.Album(album) }
                                    .padding(8.dp),
                        ) {
                            Artwork(album.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f))
                            Spacer(Modifier.height(6.dp))
                            Text(album.name, style = typo().titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(album.artist, album.year?.toString()).joinToString(" · "),
                                style = typo().bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            section == LocalSection.ARTISTS -> {
                val state = rememberLazyListState()
                ReportTop(state, onScrolling)
                LazyColumn(state = state, contentPadding = bottom, modifier = Modifier.fillMaxSize()) {
                    items(artists, key = { it.name }) { artist ->
                        LocalRow(
                            artwork = artist.artworkUri,
                            title = artist.name,
                            subtitle = stringResource(Res.string.local_track_count, artist.trackCount.toString()),
                            onClick = { detail = LocalDetail.Artist(artist) },
                        )
                    }
                }
            }

            else -> {
                val state = rememberLazyListState()
                ReportTop(state, onScrolling)
                LazyColumn(state = state, contentPadding = bottom, modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(onClick = pickFolder) {
                                Icon(SimpIcons.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.local_add_folder))
                            }
                            OutlinedButton(onClick = { viewModel.rescan(onResult = reportScan) }, enabled = !isScanning) {
                                Icon(SimpIcons.RestartAlt, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(if (isScanning) Res.string.local_scanning else Res.string.local_rescan))
                            }
                        }
                    }
                    items(folders, key = { it.uri }) { folder ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { detail = LocalDetail.Folder(folder) }
                                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(folder.name, style = typo().titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(folder.uri, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            RippleIconButton(SimpIcons.RestartAlt, tint = MaterialTheme.colorScheme.onSurface) { viewModel.rescan(folder.uri, reportScan) }
                            RippleIconButton(SimpIcons.Delete, tint = MaterialTheme.colorScheme.onSurface) { viewModel.removeFolder(folder.uri) }
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { menuFor = null },
            navController = navController,
            song = track.toSongEntity(),
        )
    }
}

@Composable
private fun LocalSongList(
    list: List<LocalTrackEntity>,
    name: String,
    viewModel: LocalMusicViewModel,
    padding: PaddingValues,
    onScrolling: (Boolean) -> Unit,
    onMore: (LocalTrackEntity) -> Unit,
) {
    val state = rememberLazyListState()
    ReportTop(state, onScrolling)
    LazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.local_track_count, list.size.toString()),
                    style = typo().bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.shuffle(list, name) }, enabled = list.isNotEmpty()) {
                    Text(stringResource(Res.string.shuffle))
                }
                FilledTonalButton(onClick = { viewModel.play(list, 0, name) }, enabled = list.isNotEmpty()) {
                    Text(stringResource(Res.string.play_all))
                }
            }
        }
        itemsIndexed(list, key = { _, track -> track.id }) { index, track ->
            LocalRow(
                artwork = track.artworkUri,
                title = track.title,
                subtitle = listOfNotNull(track.artist ?: track.albumArtist, track.qualityLabel().ifBlank { null }).joinToString(" · "),
                onClick = { viewModel.play(list, index, name) },
                onMore = { onMore(track) },
            )
        }
    }
}

@Composable
private fun LocalRow(
    artwork: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(artwork, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onMore != null) {
            RippleIconButton(SimpIcons.MoreVert, tint = MaterialTheme.colorScheme.onSurface, onClick = onMore)
        }
    }
}

@Composable
private fun Artwork(
    uri: String?,
    modifier: Modifier,
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberHolderPainter(square = true),
        error = rememberHolderPainter(square = true),
        fallback = rememberHolderPainter(square = true),
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun EmptyLocal(onAddFolder: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.local_empty),
                style = typo().bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAddFolder) {
                Icon(SimpIcons.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.local_add_folder))
            }
        }
    }
}

/** Tells Library whether the list is at its top, which is what decides the bar's frost. */
@Composable
private fun ReportTop(
    state: LazyListState,
    onScrolling: (Boolean) -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
            .distinctUntilChanged()
            .collect(onScrolling)
    }
}
