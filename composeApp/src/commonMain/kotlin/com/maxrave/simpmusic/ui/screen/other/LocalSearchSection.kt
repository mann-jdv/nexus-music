package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.toSongEntity
import com.maxrave.domain.data.entities.toTrack
import com.maxrave.domain.usecase.localmusic.SearchLocalTracksUseCase
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LocalMusicViewModel
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.local_search_section

/**
 * "On this device": the user's own files matching [query], above the online results — and on their
 * own when offline, where the online search has nothing to show. Draws nothing when there is no
 * match, so the screen around it is unchanged for anyone without local music.
 */
@Composable
fun LocalSearchSection(
    query: String,
    currentVideoId: String,
    onMore: (SongEntity) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 8,
    search: SearchLocalTracksUseCase = koinInject(),
    viewModel: LocalMusicViewModel = koinViewModel(),
) {
    val q = query.trim()
    val results by remember(q) { if (q.length < 2) flowOf(emptyList()) else search(q) }.collectAsStateWithLifecycle(emptyList())
    if (results.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.local_search_section),
            style = typo().titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        results.take(maxItems).forEachIndexed { index, track ->
            SongFullWidthItems(
                modifier = Modifier,
                track = track.toTrack(),
                isPlaying = track.id == currentVideoId,
                onMoreClickListener = { onMore(track.toSongEntity()) },
                onClickListener = { viewModel.play(results, index, "\"$q\"") },
            )
        }
    }
}
