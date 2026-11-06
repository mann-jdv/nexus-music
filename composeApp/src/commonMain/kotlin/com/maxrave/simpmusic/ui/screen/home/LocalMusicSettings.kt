package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.entities.LocalFolderEntity
import com.maxrave.domain.data.entities.LocalScanResult
import com.maxrave.simpmusic.expect.ui.rememberFolderPicker
import com.maxrave.simpmusic.ui.component.SettingGroup
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.component.glass.GlassAlertDialog
import com.maxrave.simpmusic.viewModel.LocalMusicViewModel
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.local_add_folder
import simpmusic.composeapp.generated.resources.local_add_folder_description
import simpmusic.composeapp.generated.resources.local_folder_unreadable
import simpmusic.composeapp.generated.resources.local_music_settings
import simpmusic.composeapp.generated.resources.local_remove_folder
import simpmusic.composeapp.generated.resources.local_remove_folder_message
import simpmusic.composeapp.generated.resources.local_rescan
import simpmusic.composeapp.generated.resources.local_rescan_description
import simpmusic.composeapp.generated.resources.local_scan_result
import simpmusic.composeapp.generated.resources.local_scanning

/**
 * Settings › Local music: add a folder, rescan everything, and one row per folder (tap to remove).
 * The same folders show under Library › Local › Folders; both go through [LocalMusicViewModel].
 */
@Composable
fun LocalMusicSettingsGroup(viewModel: LocalMusicViewModel = koinViewModel()) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<LocalFolderEntity?>(null) }
    val scope = rememberCoroutineScope()

    val report: (LocalScanResult?) -> Unit = { result ->
        scope.launch {
            showToast(
                if (result == null) {
                    getString(Res.string.local_folder_unreadable)
                } else {
                    getString(Res.string.local_scan_result, (result.added + result.updated).toString(), result.removed.toString())
                },
                ToastGravity.Bottom,
            )
        }
    }
    val pickFolder = rememberFolderPicker { uri -> viewModel.addFolder(uri, report) }

    SettingGroup(title = stringResource(Res.string.local_music_settings)) {
        SettingItem(
            title = stringResource(Res.string.local_add_folder),
            subtitle = stringResource(Res.string.local_add_folder_description),
            smallSubtitle = true,
            onClick = pickFolder,
        )
        SettingItem(
            title = stringResource(if (isScanning) Res.string.local_scanning else Res.string.local_rescan),
            subtitle = stringResource(Res.string.local_rescan_description),
            smallSubtitle = true,
            isEnable = !isScanning && folders.isNotEmpty(),
            onClick = { viewModel.rescan(onResult = report) },
        )
        folders.forEach { folder ->
            SettingItem(
                title = folder.name,
                subtitle = folder.uri,
                smallSubtitle = true,
                onClick = { removing = folder },
            )
        }
    }

    removing?.let { folder ->
        GlassAlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(Res.string.local_remove_folder)) },
            text = { Text(stringResource(Res.string.local_remove_folder_message, folder.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFolder(folder.uri)
                    removing = null
                }) { Text(stringResource(Res.string.local_remove_folder)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}
