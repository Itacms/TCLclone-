package com.iptvclone.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptvclone.player.data.Channel

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .tvClickable(RoundedCornerShape(20.dp), onClick)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun ChannelsScreen(
    title: String,
    channels: List<Channel>,
    favorites: Set<String>,
    emptyText: String,
    onPlay: (Channel) -> Unit,
    onFav: (Channel) -> Unit,
) {
    var query by rememberSaveable(title) { mutableStateOf("") }
    var group by rememberSaveable(title) { mutableStateOf<String?>(null) }
    val groups = remember(channels) { channels.map { it.group }.distinct().sorted() }
    val filtered = remember(channels, query, group) {
        channels.filter {
            (group == null || it.group == group) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (channels.isEmpty()) {
            Text(emptyText)
            return@Column
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar canal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (groups.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Pill("Todos", group == null) { group = null } }
                items(groups) { g -> Pill(g, group == g) { group = g } }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filtered) { ch ->
                Row(
                    Modifier.fillMaxWidth().tvClickable { onPlay(ch) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ch.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            ch.group,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { onFav(ch) }) {
                        Icon(
                            if (ch.url in favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favoritar",
                        )
                    }
                }
            }
        }
    }
}
