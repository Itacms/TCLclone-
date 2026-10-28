package com.iptvclone.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptvclone.player.data.Channel
import com.iptvclone.player.data.EpgProgram
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuideScreen(channels: List<Channel>, programs: List<EpgProgram>, onPlay: (Channel) -> Unit) {
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val liveChannels = remember(channels) { channels.filterNot { Regex("filme|s[ée]rie|vod", RegexOption.IGNORE_CASE).containsMatchIn(it.group) } }

    Column(Modifier.fillMaxSize().background(Color(0xFF080B12)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Guia de programação", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("AO VIVO  •  ${clock.format(Date(now))}", color = Color(0xFF63DED5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (programs.isEmpty()) {
            Text(
                "Sua lista ainda não disponibiliza EPG/XMLTV. Para preencher esta grade com programas, horários e progresso reais, a lista M3U precisa declarar url-tvg ou x-tvg-url no cabeçalho.",
                color = Color(0xFFB7C0CE),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (liveChannels.isEmpty()) {
            Text("Importe uma lista M3U/M3U8 para ver os canais ao vivo.", color = Color(0xFFB7C0CE))
        } else {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("CANAL", color = Color(0xFF9AA6B8), fontSize = 10.sp, modifier = Modifier.width(180.dp))
                Text("PROGRAMAÇÃO", color = Color(0xFF9AA6B8), fontSize = 10.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(liveChannels, key = { it.url }) { channel ->
                    val related = programs.filter { it.channelId == channel.tvgId || it.channelId.equals(channel.tvgId, true) }
                        .sortedBy { it.startMillis }
                    val current = related.firstOrNull { now >= it.startMillis && now < it.endMillis }
                    val next = related.firstOrNull { it.startMillis >= now }
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF121A29), RoundedCornerShape(10.dp))
                            .tvClickable(RoundedCornerShape(10.dp)) { onPlay(channel) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(168.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("▶ Assistir", color = Color(0xFF63DED5), fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (current != null) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(clock.format(Date(current.startMillis)), color = Color(0xFF9AA6B8), fontSize = 10.sp)
                                    Text("  ${current.title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                val progress = ((now - current.startMillis).toFloat() / (current.endMillis - current.startMillis).toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Color(0xFF63DED5), trackColor = Color(0xFF334152))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Agora", color = Color(0xFF63DED5), fontSize = 10.sp)
                                    Text(clock.format(Date(current.endMillis)), color = Color(0xFF9AA6B8), fontSize = 10.sp)
                                }
                            } else {
                                Text("Programação atual indisponível", color = Color(0xFFB7C0CE), fontSize = 12.sp)
                            }
                            if (next != null) {
                                Text("A seguir • ${clock.format(Date(next.startMillis))}  ${next.title}", color = Color(0xFF9AA6B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
