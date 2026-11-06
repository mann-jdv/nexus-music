package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that asks the user for a music folder and hands back its location: a
 * `content://` tree URI on Android (ACTION_OPEN_DOCUMENT_TREE; access is persisted by the data
 * layer when the folder is added), an absolute directory path on Desktop. Nothing comes back when
 * the user cancels.
 */
@Composable
expect fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit
