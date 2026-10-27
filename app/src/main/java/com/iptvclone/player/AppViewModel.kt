package com.iptvclone.player

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptvclone.player.data.Channel
import com.iptvclone.player.data.Playlist
import com.iptvclone.player.data.PlaylistRepository
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlaylistRepository(app)

    var playlists by mutableStateOf<List<Playlist>>(emptyList()); private set
    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var favorites by mutableStateOf<Set<String>>(emptySet()); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    init {
        favorites = repo.favorites()
        work { }
    }

    private fun work(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                block()
                playlists = repo.playlists()
                channels = repo.allChannels()
                if (successMessage != null) message = successMessage
            } catch (e: Exception) {
                message = e.message ?: "Erro inesperado."
            } finally {
                busy = false
            }
        }
    }

    fun addUrl(name: String, url: String) =
        work("Lista adicionada com sucesso.") { repo.addFromUrl(name, url) }

    fun addLocal(name: String, uri: Uri) =
        work("Arquivo importado com sucesso.") { repo.addFromUri(name, uri) }

    fun refresh(id: String) = work("Lista atualizada.") { repo.refresh(id) }
    fun remove(id: String) = work("Lista removida.") { repo.remove(id) }
    fun setEnabled(id: String, enabled: Boolean) = work { repo.setEnabled(id, enabled) }

    fun toggleFavorite(channel: Channel) {
        favorites = if (channel.url in favorites) favorites - channel.url else favorites + channel.url
        repo.setFavorites(favorites)
    }

    fun clearFavorites() {
        favorites = emptySet()
        repo.setFavorites(favorites)
    }
}
