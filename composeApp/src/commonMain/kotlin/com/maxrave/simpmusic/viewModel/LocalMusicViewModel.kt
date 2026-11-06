package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.LocalAlbum
import com.maxrave.domain.data.entities.LocalArtist
import com.maxrave.domain.data.entities.LocalFolderEntity
import com.maxrave.domain.data.entities.LocalScanResult
import com.maxrave.domain.data.entities.LocalTrackEntity
import com.maxrave.domain.data.entities.toTrack
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.repository.LocalMusicRepository
import com.maxrave.domain.usecase.localmusic.AddLocalFolderUseCase
import com.maxrave.domain.usecase.localmusic.GetLocalAlbumsUseCase
import com.maxrave.domain.usecase.localmusic.GetLocalArtistsUseCase
import com.maxrave.domain.usecase.localmusic.GetLocalFoldersUseCase
import com.maxrave.domain.usecase.localmusic.GetLocalTracksUseCase
import com.maxrave.domain.usecase.localmusic.RemoveLocalFolderUseCase
import com.maxrave.domain.usecase.localmusic.ScanLocalMusicUseCase
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * Library › Local and Settings › Local music. Everything goes through the domain use cases; playback
 * goes through the same queue as YouTube tracks, so local and streamed songs mix in one queue.
 */
class LocalMusicViewModel(
    getFolders: GetLocalFoldersUseCase,
    private val getTracks: GetLocalTracksUseCase,
    getAlbums: GetLocalAlbumsUseCase,
    getArtists: GetLocalArtistsUseCase,
    private val addFolder: AddLocalFolderUseCase,
    private val removeFolder: RemoveLocalFolderUseCase,
    private val scan: ScanLocalMusicUseCase,
) : BaseViewModel() {
    private val repository: LocalMusicRepository by inject()

    val folders: StateFlow<List<LocalFolderEntity>> = getFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tracks: StateFlow<List<LocalTrackEntity>> = getTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val albums: StateFlow<List<LocalAlbum>> = getAlbums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val artists: StateFlow<List<LocalArtist>> = getArtists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val isScanning: StateFlow<Boolean> = repository.isScanning

    fun tracksOfAlbum(album: LocalAlbum): Flow<List<LocalTrackEntity>> = getTracks.ofAlbum(album.name, album.artist)

    fun tracksOfArtist(artist: LocalArtist): Flow<List<LocalTrackEntity>> = getTracks.ofArtist(artist.name)

    fun tracksInFolder(folder: LocalFolderEntity): Flow<List<LocalTrackEntity>> = getTracks.inFolder(folder.uri)

    fun addFolder(
        uri: String,
        onResult: (LocalScanResult?) -> Unit = {},
    ) {
        viewModelScope.launch { onResult(addFolder.invoke(uri)) }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch { removeFolder.invoke(uri) }
    }

    fun rescan(
        folderUri: String? = null,
        onResult: (LocalScanResult) -> Unit = {},
    ) {
        viewModelScope.launch { onResult(scan(folderUri)) }
    }

    /**
     * Plays [list] from [index]. A plain playlist queue, not a radio: no continuation and no related
     * tracks, so a local queue never reaches for the network when it runs out.
     */
    fun play(
        list: List<LocalTrackEntity>,
        index: Int,
        name: String,
    ) {
        val first = list.getOrNull(index) ?: return
        setQueueData(
            QueueData.Data(
                listTracks = list.map { it.toTrack() }.toCollection(ArrayList()),
                firstPlayedTrack = first.toTrack(),
                playlistId = null,
                playlistName = name,
                playlistType = PlaylistType.PLAYLIST,
                continuation = null,
            ),
        )
        loadMediaItem(first.toTrack(), Config.PLAYLIST_CLICK, index)
    }

    fun shuffle(
        list: List<LocalTrackEntity>,
        name: String,
    ) {
        if (list.isEmpty()) return
        play(list.shuffled(), 0, name)
    }
}
