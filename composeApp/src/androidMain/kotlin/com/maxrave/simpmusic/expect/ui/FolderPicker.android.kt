package com.maxrave.simpmusic.expect.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPicked)
    // OpenDocumentTree's result intent carries FLAG_GRANT_PERSISTABLE_URI_PERMISSION, which is what
    // lets the data layer call takePersistableUriPermission on it.
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { callback(it.toString()) }
        }
    return { launcher.launch(null) }
}
