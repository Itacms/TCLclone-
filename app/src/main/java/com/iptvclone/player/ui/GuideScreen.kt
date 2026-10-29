package com.iptvclone.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptvclone.player.data.Channel
import com.iptvclone.player.data.EpgProgram
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val bg = Color(0xFF08090C)
private val panel = Color(0xFF17191D)
private val muted = Color(0xFF9DA3AD)
private val liveRed = Color(0xFFFF343D)

@Composable
fun GuideScreen(channels: List<Channel>, programs: List<EpgProgram>, onPlay: (Channel) -> Unit) {
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000L); now = System.currentTimeMillis() } }
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val liveChannels = remember(channels) { channels.filterNot { Regex("filme|s[ée]rie|vod|movie", RegexOption.IGNORE_CASE).containsMatchIn(it.group) } }
    val featured = liveChannels.firstOrNull()
    val featuredPrograms = remember(programs, featured) { programs.filter { featured != null && it.channelId.equals(featured.tvgId, true) }.sortedBy { it.startMillis } }
    val current = featuredPrograms.firstOrNull { now >= it.startMillis && now < it.endMillis }
    val next = featuredPrograms.firstOrNull { it.startMillis >= now }

    Column(Modifier.fillMaxSize().background(bg)) {
        Row(Modifier.fillMaxWidth().height(230.dp)) {
            Column(Modifier.weight(0.9f).fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black, Color(0xFF15161A)))).padding(start = 26.dp, end = 18.dp, top = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.Center) {
                Text(featured?.name ?: "TV ao vivo", color = Color(0xFFD1D3D8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(current?.title ?: "Programação ao vivo", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AO VIVO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(liveRed, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                    Text(if (current != null) "${clock.format(Date(current.startMillis))} – ${clock.format(Date(current.endMillis))}" else clock.format(Date(now)), color = Color(0xFFE4E5E8), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(next?.let { "A seguir: ${clock.format(Date(it.startMillis))} • ${it.title}" } ?: "Selecione um canal para assistir à transmissão.", color = muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFF292A2D)).tvClickable(RoundedCornerShape(7.dp)) { featured?.let(onPlay) }.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                    Text("  Assistir canal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.weight(1.1f).fillMaxSize().background(Color(0xFF202832))) {
                if (!featured?.logo.isNullOrBlank()) AsyncImage(model = featured?.logo, contentDescription = featured?.name, modifier = Modifier.fillMaxSize().padding(28.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF16374C), Color(0xFF0B111B)))))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x5506090D), Color.Transparent))))
                Text("PRÉVIA DO CANAL", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Destaques", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            (0..3).forEach { Text(clock.format(Date(now + it * 30 * 60 * 1000L)), color = muted, fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            Text("GUIA DE PROGRAMAÇÃO", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (programs.isEmpty()) Text("A grade detalhada depende do EPG/XMLTV declarado na lista M3U (url-tvg ou x-tvg-url).", color = muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
        if (liveChannels.isEmpty()) Text("Importe sua lista M3U/M3U8 para ver os canais ao vivo.", color = Color.White, modifier = Modifier.padding(24.dp))
        else LazyColumn(Modifier.fillMaxSize().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(liveChannels, key = { it.url }) { channel ->
                val related = programs.filter { it.channelId.equals(channel.tvgId, true) }.sortedBy { it.startMillis }
                val airing = related.firstOrNull { now >= it.startMillis && now < it.endMillis }
                val upcoming = related.filter { it.startMillis >= now }.take(3)
                val progress = if (airing != null && airing.endMillis > airing.startMillis) ((now - airing.startMillis).toFloat() / (airing.endMillis - airing.startMillis).toFloat()).coerceIn(0f, 1f) else 0f
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).background(Color(0xFF111215), RoundedCornerShape(6.dp)).tvClickable(RoundedCornerShape(6.dp)) { onPlay(channel) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(105.dp).height(56.dp).clip(RoundedCornerShape(5.dp)).background(panel), contentAlignment = Alignment.Center) {
                        if (!channel.logo.isNullOrBlank()) AsyncImage(model = channel.logo, contentDescription = channel.name, modifier = Modifier.fillMaxSize().padding(7.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                        else Text(channel.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(Modifier.width(165.dp).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(channel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (airing != null) "AO VIVO" else "▶ Assistir", color = if (airing != null) liveRed else muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (airing != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = liveRed, trackColor = Color(0xFF3B3D42))
                    }
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val visible = (listOfNotNull(airing) + upcoming).distinctBy { it.startMillis }
                        if (visible.isEmpty()) item { Box(Modifier.width(250.dp).height(56.dp).background(panel, RoundedCornerShape(5.dp)).padding(10.dp)) { Text("Programação indisponível", color = muted, fontSize = 11.sp) } }
                        else items(visible) { program ->
                            val selected = program == airing
                            Column(Modifier.width(if (selected) 245.dp else 210.dp).height(56.dp).background(if (selected) Color(0xFF292A2D) else panel, RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${clock.format(Date(program.startMillis))} • ${program.title}", color = Color.White, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (selected) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = liveRed, trackColor = Color(0xFF45464A))
                                else Text(clock.format(Date(program.endMillis)), color = muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}