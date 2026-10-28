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

    private val prefs = app.getSharedPreferences("iptv_prefs", android.content.Context.MODE_PRIVATE)
    var backgroundOpacity by mutableStateOf(prefs.getInt("backgroundOpacity", 100)); private set
    var logoOpacity by mutableStateOf(prefs.getInt("logoOpacity", 100)); private set
    var cardOpacity by mutableStateOf(prefs.getInt("cardOpacity", 100)); private set

    fun setOpacity(key: String, value: Int) {
        val safeKey = when (key) {
            "backgroundOpacity", "logoOpacity", "cardOpacity" -> key
            else -> return
        }
        val safeValue = value.coerceIn(0, 100)
        prefs.edit().putInt(safeKey, safeValue).apply()
        when (safeKey) {
            "backgroundOpacity" -> backgroundOpacity = safeValue
            "logoOpacity" -> logoOpacity = safeValue
            "cardOpacity" -> cardOpacity = safeValue
        }
    }

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

    fun exportBackup(uri: Uri) = work("Backup exportado com sucesso.") { repo.exportBackup(uri) }

    fun restoreBackup(uri: Uri) = work("Backup restaurado com sucesso.") {
        repo.restoreBackup(uri)
        favorites = repo.favorites()
        backgroundOpacity = prefs.getInt("backgroundOpacity", 100)
        logoOpacity = prefs.getInt("logoOpacity", 100)
        cardOpacity = prefs.getInt("cardOpacity", 100)
    }

    fun clearFavorites() {
        favorites = emptySet()
        repo.setFavorites(favorites)
    }
}
