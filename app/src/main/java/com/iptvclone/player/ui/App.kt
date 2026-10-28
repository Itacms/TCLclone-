package com.iptvclone.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.iptvclone.player.AppViewModel
import com.iptvclone.player.data.Channel

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home),
    CHANNELS("TV ao vivo", Icons.Filled.PlayArrow),
    MOVIES("Filmes e séries", Icons.Filled.Star),
    FAVORITES("Favoritos", Icons.Filled.Favorite),
    PLAYLISTS("Listas IPTV", Icons.Filled.List),
    SETTINGS("Configurações", Icons.Filled.Settings),
}

private val movieRegex = Regex("film|movie|vod|cine|s[ée]rie|series", RegexOption.IGNORE_CASE)
private const val heroImage = "https://aws-obg-image-lb-5.tcl.com/content/dam/brandsite/region/brazil/blog/naruto.jpg?t=1758250221939&w=1200"

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
            Tab.HOME -> HomeScreen(vm, onGoChannels = { tab = Tab.CHANNELS }, onGoPlaylists = { tab = Tab.PLAYLISTS }, onPlay = { playing = it })
            Tab.CHANNELS -> ChannelsScreen(
                "TV ao vivo", vm.channels.filterNot { movieRegex.containsMatchIn(it.group) }, vm.favorites,
                "Nenhum canal encontrado. Adicione sua lista na aba Listas IPTV.",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.MOVIES -> ChannelsScreen(
                "Filmes e séries", vm.channels.filter { movieRegex.containsMatchIn(it.group) }, vm.favorites,
                "Nenhum grupo de filmes/séries encontrado nas suas listas.",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.FAVORITES -> ChannelsScreen(
                "Favoritos", vm.channels.filter { it.url in vm.favorites }, vm.favorites,
                "Use o coração de um canal para adicioná-lo aos favoritos.",
                { playing = it }, vm::toggleFavorite,
            )
            Tab.PLAYLISTS -> PlaylistsScreen(vm)
            Tab.SETTINGS -> SettingsScreen(vm)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF080B12))) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.width(184.dp).fillMaxHeight()
                        .background(Color(0xFF0B101A))
                        .padding(horizontal = 12.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("TCL  •  PLAY", color = Color(0xFF53D8D1), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 12.dp, bottom = 22.dp))
                    Tab.values().forEach { t ->
                        val selected = t == tab
                        val bg = if (selected) Color(0xFF203A49) else Color.Transparent
                        Row(
                            Modifier.fillMaxWidth().tvClickable(RoundedCornerShape(14.dp)) { tab = t }
                                .background(bg, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(t.icon, contentDescription = null, tint = if (selected) Color(0xFF67E4DB) else Color(0xFFB7C0CE), modifier = Modifier.size(21.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(t.label, color = if (selected) Color.White else Color(0xFFB7C0CE), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                NavigationBar {
                    Tab.values().forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1, fontSize = 9.sp) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    vm: AppViewModel,
    onGoChannels: () -> Unit,
    onGoPlaylists: () -> Unit,
    onPlay: (Channel) -> Unit,
) {
    val channels = vm.channels
    val favorites = channels.filter { it.url in vm.favorites }
    val live = channels.filterNot { movieRegex.containsMatchIn(it.group) }
    val films = channels.filter { movieRegex.containsMatchIn(it.group) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF080B12)),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp).background(Color(0xFF121A29))) {
                AsyncImage(
                    model = heroImage,
                    contentDescription = "Destaque visual",
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = vm.backgroundOpacity / 100f),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF080B12), Color(0xE6080B12), Color(0x99080B12), Color.Transparent)
                        )
                    )
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA080B12)))
                    )
                )
                Column(
                    Modifier.align(Alignment.CenterStart).fillMaxWidth(0.72f).padding(start = 28.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("DESTAQUE", color = Color(0xFF63DED5), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Entre no seu mundo", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 33.sp)
                    Text(
                        if (channels.isEmpty()) "Adicione uma lista IPTV para preencher sua tela com seus canais."
                        else "Seus canais, favoritos e conteúdos em uma experiência feita para a TV.",
                        color = Color(0xFFE1E6EE), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { if (live.isNotEmpty()) onPlay(live.first()) else onGoChannels() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Assistir")
                        }
                        OutlinedButton(onClick = if (channels.isEmpty()) onGoPlaylists else onGoChannels) {
                            Text(if (channels.isEmpty()) "Adicionar lista" else "Ver canais")
                        }
                    }
                }
                Text(
                    "IPTV  •  SUA PROGRAMAÇÃO",
                    color = Color(0xFFD6DCE7),
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sua programação", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${vm.playlists.size} listas  •  ${channels.size} canais  •  ${vm.favorites.size} favoritos", color = Color(0xFF9AA6B8), fontSize = 12.sp)
                }
                Text("GERENCIAR LISTAS  ›", color = Color(0xFF65DCD4), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.tvClickable { onGoPlaylists() }.padding(8.dp))
            }
        }

        if (favorites.isNotEmpty()) {
            item { ChannelShelf("Continue com seus favoritos", favorites, onPlay) }
        }
        if (live.isNotEmpty()) {
            item { ChannelShelf("TV ao vivo", live, onPlay) }
        }
        if (films.isNotEmpty()) {
            item { ChannelShelf("Filmes e séries", films, onPlay) }
        }
        if (channels.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sua tela está esperando suas listas", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Importe um arquivo M3U/M3U8 ou cole o endereço da sua lista para começar.", color = Color(0xFF9AA6B8), fontSize = 13.sp)
                    OutlinedButton(onClick = onGoPlaylists) { Text("Abrir Listas IPTV") }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChannelShelf(title: String, channels: List<Channel>, onPlay: (Channel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        ) {
            items(channels.take(30)) { ch ->
                Column(
                    Modifier.width(156.dp).tvClickable(RoundedCornerShape(12.dp)) { onPlay(ch) }
                        .background(Color(0xFF151D2A).copy(alpha = vm.cardOpacity / 100f), RoundedCornerShape(12.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(82.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF202B3B)), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = ch.logo,
                            contentDescription = ch.name,
                            modifier = Modifier.fillMaxSize().padding(8.dp).graphicsLayer(alpha = vm.logoOpacity / 100f),
                            contentScale = ContentScale.Fit,
                        )
                        if (ch.logo.isNullOrBlank()) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF63DED5), modifier = Modifier.size(32.dp))
                        }
                    }
                    Text(ch.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(ch.group.ifBlank { "TV" }, color = Color(0xFF9AA6B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: AppViewModel) {
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restoreBackup(uri)
    }
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF080B12)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Configurações", style = MaterialTheme.typography.headlineSmall, color = Color.White) }
        item { Text("Backup e restauração", style = MaterialTheme.typography.titleMedium, color = Color.White) }
        item { Text("O backup inclui listas M3U armazenadas, favoritos e preferências visuais.", color = Color(0xFFB7C0CE), style = MaterialTheme.typography.bodyMedium) }
        item { Button(onClick = { createBackup.launch("iptv-clone-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Fazer backup") } }
        item { OutlinedButton(onClick = { restoreBackup.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar backup") } }
        item { Text("Atenção: restaurar substitui as listas e os favoritos atuais pelos dados do arquivo selecionado.", color = Color(0xFFFFC978), style = MaterialTheme.typography.bodySmall) }
        item { Text("Transparência", style = MaterialTheme.typography.titleMedium, color = Color.White) }
        item { OpacitySetting("Fundo da tela inicial", vm.backgroundOpacity) { vm.setOpacity("backgroundOpacity", it) } }
        item { OpacitySetting("Logotipos", vm.logoOpacity) { vm.setOpacity("logoOpacity", it) } }
        item { OpacitySetting("Cartões de canais", vm.cardOpacity) { vm.setOpacity("cardOpacity", it) } }
        item { OutlinedButton(onClick = { vm.clearFavorites() }) { Text("Limpar favoritos") } }
        item { Text("Este app é apenas um player. Use somente listas e conteúdos que você tem direito de acessar.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA6B8)) }
        item { Text("Versão 1.2", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA6B8)) }
        item { vm.message?.let { Text(it, color = Color(0xFF63DED5), style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
private fun OpacitySetting(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text("$value%", color = Color(0xFF63DED5), style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value.toFloat(), onValueChange = { onValueChange((it / 5f).toInt() * 5) }, valueRange = 0f..100f, steps = 19)
    }
}
