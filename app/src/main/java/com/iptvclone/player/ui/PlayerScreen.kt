package com.iptvclone.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptvclone.player.data.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    autoPlay: Boolean = true,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onBackToGuide: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember(channel.url) { mutableStateOf<String?>(null) }
    var buffering by remember(channel.url) { mutableStateOf(true) }
    val player = remember(channel.url) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(channel.url))
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "Não foi possível reproduzir este canal (${e.errorCodeName})."
                buffering = false
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) buffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    BackHandler { onBackToGuide() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xE6000000), Color.Transparent)))
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.tvClickable(RoundedCornerShape(22.dp)) { onBackToGuide() }
                    .background(Color(0xAA17191D), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Guia de canais", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f)) {
                Text(channel.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.group.ifBlank { "TV ao vivo" }, color = Color(0xFFCFD3DA), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.tvClickable(RoundedCornerShape(22.dp)) { onToggleFavorite() }
                    .background(if (isFavorite) Color(0xFFFF343D) else Color(0xAA17191D), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (isFavorite) "Favorito" else "Favoritar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000))))
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("REPRODUÇÃO AO VIVO", color = Color(0xFFFF454D), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                Text("Use os controles para pausar ou retomar a transmissão.", color = Color(0xFFD8DCE2), fontSize = 11.sp)
            }
            Row(
                Modifier.tvClickable(RoundedCornerShape(22.dp)) { onBackToGuide() }
                    .background(Color(0xEEFFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.List, contentDescription = null, tint = Color(0xFF101114), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Voltar ao guia", color = Color(0xFF101114), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (buffering && error == null) {
            Text("Carregando transmissão…", color = Color.White, modifier = Modifier.align(Alignment.Center)
                .background(Color(0xAA101114), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 13.sp)
        }
        error?.let {
            Column(Modifier.align(Alignment.Center).padding(24.dp).background(Color(0xEE17191D), RoundedCornerShape(14.dp)).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Não foi possível abrir o canal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(it, color = Color(0xFFFFA3A7), fontSize = 12.sp)
                Row(Modifier.tvClickable(RoundedCornerShape(20.dp)) { error = null; player.prepare(); player.playWhenReady = true }
                    .background(Color(0xFFFF343D), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Tentar novamente", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
