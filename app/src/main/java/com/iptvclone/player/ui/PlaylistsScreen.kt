package com.iptvclone.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.iptvclone.player.AppViewModel
import com.iptvclone.player.data.SourceType
import java.text.DateFormat
import java.util.Date

/** NOVA ABA: gerenciamento de listas IPTV (URL + arquivo local). */
@Composable
fun PlaylistsScreen(vm: AppViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.addLocal(name, uri)
            name = ""
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Listas IPTV", style = MaterialTheme.typography.headlineSmall) }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Adicionar lista", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nome (opcional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("URL da lista (.m3u / .m3u8)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.addUrl(name, url); url = ""; name = "" },
                            enabled = !vm.busy && url.isNotBlank(),
                        ) { Text("Adicionar por URL") }
                        OutlinedButton(onClick = {
                            clipboard.getText()?.text?.let { url = it.trim() }
                        }) { Text("Colar") }
                    }
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        enabled = !vm.busy,
                    ) { Text("Escolher arquivo local (.m3u / .m3u8)") }
                    if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    vm.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        item { Text("Suas listas (${vm.playlists.size})", style = MaterialTheme.typography.titleMedium) }

        items(vm.playlists) { pl ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleSmall)
                        val origem = if (pl.type == SourceType.URL) "URL" else "Arquivo local"
                        val data = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(pl.updatedAt))
                        Text("$origem • ${pl.channelCount} canais • $data", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = pl.enabled, onCheckedChange = { vm.setEnabled(pl.id, it) })
                    if (pl.type == SourceType.URL) {
                        IconButton(onClick = { vm.refresh(pl.id) }, enabled = !vm.busy) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Atualizar")
                        }
                    }
                    IconButton(onClick = { vm.remove(pl.id) }, enabled = !vm.busy) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remover")
                    }
                }
            }
        }
    }
}
