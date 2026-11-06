package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser

@Composable
actual fun rememberFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPicked)
    val scope = rememberCoroutineScope()
    return {
        // Swing dialogs belong on the EDT, which is Compose Desktop's Main dispatcher.
        scope.launch(Dispatchers.Main) {
            val chooser =
                JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                    dialogTitle = "Choose a music folder"
                }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.absolutePath?.let(callback)
            }
        }
    }
}
