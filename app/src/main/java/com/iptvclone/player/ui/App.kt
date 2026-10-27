package com.iptvclone.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptvclone.player.AppViewModel
import com.iptvclone.player.data.Channel

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home),
    CHANNELS("Canais", Icons.Filled.PlayArrow),
    MOVIES("Filmes", Icons.Filled.Star),
    FAVORITES("Favoritos", Icons.Filled.Favorite),
    PLAYLISTS("Listas IPTV", Icons.Filled.List), // aba nova
    SETTINGS("Config.", Icons.Filled.Settings),
}

private val movieRegex = Regex("film|movie|vod|cine|s[ée]rie|series", RegexOption.IGNORE_CASE)

@Composable
fun App(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var playing by remember { mutableStateOf<Channel?>(null) }

    val current = playing
    if (current != null) {
        PlayerScreen(current) { playing = null }
        return
    }

    val content: @Composable () -> Unit = {
        when (tab) {
            Tab.HOME -> HomeScreen(vm, onGoPlaylists = { tab = Tab.PLAYLISTS }, onPlay = { playing = it })
            Tab.CHANNELS -> ChannelsScreen(
                "Canais", vm.channels.filterNot { movieRegex.containsMatchIn(it.group) }, vm.favorites,
                "Nenhum canal. Adicione uma lista na aba \"Listas IPTV\".",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.MOVIES -> ChannelsScreen(
                "Filmes e séries", vm.channels.filter { movieRegex.containsMatchIn(it.group) }, vm.favorites,
                "Nenhum grupo de filmes/séries encontrado nas suas listas.",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.FAVORITES -> ChannelsScreen(
                "Favoritos", vm.channels.filter { it.url in vm.favorites }, vm.favorites,
                "Toque no coração de um canal para favoritá-lo.",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.PLAYLISTS -> PlaylistsScreen(vm)
            Tab.SETTINGS -> SettingsScreen(vm)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            // TV / tablet: sidebar à esquerda (como o drawer do app original)
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.width(220.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Tab.values().forEach { t ->
                        val bg = if (t == tab) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                        Row(
                            Modifier.fillMaxWidth().tvClickable { tab = t }.background(bg).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(t.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(t.label)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            // Celular: barra inferior
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                NavigationBar {
                    Tab.values().forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1, fontSize = 10.sp) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel, onGoPlaylists: () -> Unit, onPlay: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Início", style = MaterialTheme.typography.headlineSmall)
        if (vm.playlists.isEmpty()) {
            Text("Você ainda não tem listas. Adicione uma lista IPTV por URL ou arquivo local.")
            Button(onClick = onGoPlaylists) { Text("Adicionar lista IPTV") }
        } else {
            Text("${vm.playlists.size} listas • ${vm.channels.size} canais • ${vm.favorites.size} favoritos")
            val favs = vm.channels.filter { it.url in vm.favorites }
            if (favs.isNotEmpty()) {
                Text("Favoritos", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favs) { ch ->
                        Column(
                            Modifier.width(110.dp).tvClickable { onPlay(ch) }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(64.dp))
                            Text(ch.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                    }
                }
            }
            OutlinedButton(onClick = onGoPlaylists) { Text("Gerenciar listas") }
        }
    }
}

@Composable
private fun SettingsScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configurações", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = { vm.clearFavorites() }) { Text("Limpar favoritos") }
        Text(
            "Este app é apenas um player. Use somente listas e conteúdos que você tem direito de acessar.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Versão 1.0", style = MaterialTheme.typography.bodySmall)
    }
}
