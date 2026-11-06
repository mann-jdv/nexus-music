package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.typo

@Composable
internal fun AudioQualityBadge(state: NowPlayingContentState) {
    state.audioQualityLabel?.takeIf { state.isLocalAudio }?.let { label ->
        Text(
            text = label,
            style = typo().labelSmall,
            color = state.secondaryTextColor(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}
