package com.iptvclone.player.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val dir = File(context.filesDir, "playlists").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)

    private fun fileOf(id: String) = File(dir, "$id.m3u")

    // ---------- índice de listas ----------

    suspend fun playlists(): List<Playlist> = withContext(Dispatchers.IO) { readIndex() }

    private fun readIndex(): List<Playlist> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Playlist(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    type = SourceType.valueOf(o.getString("type")),
                    location = o.getString("location"),
                    channelCount = o.optInt("count", 0),
                    updatedAt = o.optLong("updatedAt", 0L),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeIndex(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("type", it.type.name)
                put("location", it.location); put("count", it.channelCount)
                put("updatedAt", it.updatedAt); put("enabled", it.enabled)
            })
        }
        indexFile.writeText(arr.toString())
    }

    // ---------- adicionar / atualizar / remover ----------

    /** Baixa uma lista M3U de uma URL (http/https). */
    suspend fun addFromUrl(name: String, url: String): Playlist = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            "A URL precisa começar com http:// ou https://"
        }
        val id = UUID.randomUUID().toString()
        download(cleanUrl, fileOf(id))
        val count = countChannels(id)
        if (count == 0) {
            fileOf(id).delete()
            error("Nenhum canal encontrado. Verifique se a URL é uma lista M3U válida.")
        }
        val displayName = name.trim().ifBlank { Uri.parse(cleanUrl).host ?: "Lista IPTV" }
        val pl = Playlist(id, displayName, SourceType.URL, cleanUrl, count, System.currentTimeMillis())
        writeIndex(readIndex() + pl)
        pl
    }

    /** Importa uma lista M3U de um arquivo local escolhido pelo seletor de arquivos (SAF). */
    suspend fun addFromUri(name: String, uri: Uri): Playlist = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val target = fileOf(id)
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Não foi possível abrir o arquivo selecionado.")
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        val count = countChannels(id)
        if (count == 0) {
            target.delete()
            error("Nenhum canal encontrado. Verifique se o arquivo é uma lista M3U/M3U8 válida.")
        }
        val displayName = name.trim().ifBlank { queryDisplayName(uri) ?: "Lista local" }
        val pl = Playlist(id, displayName, SourceType.LOCAL, uri.toString(), count, System.currentTimeMillis())
        writeIndex(readIndex() + pl)
        pl
    }

    /** Atualiza uma lista vinda de URL. Listas locais precisam ser reimportadas. */
    suspend fun refresh(id: String) = withContext(Dispatchers.IO) {
        val list = readIndex()
        val pl = list.firstOrNull { it.id == id } ?: return@withContext
        require(pl.type == SourceType.URL) {
            "Listas locais não podem ser atualizadas. Importe o arquivo novamente."
        }
        download(pl.location, fileOf(id))
        val count = countChannels(id)
        writeIndex(list.map {
            if (it.id == id) it.copy(channelCount = count, updatedAt = System.currentTimeMillis()) else it
        })
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        fileOf(id).delete()
        writeIndex(readIndex().filterNot { it.id == id })
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        writeIndex(readIndex().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    // ---------- canais ----------

    suspend fun allChannels(): List<Channel> = withContext(Dispatchers.IO) {
        readIndex().filter { it.enabled }.flatMap { pl ->
            val f = fileOf(pl.id)
            if (f.exists()) M3uParser.parse(f.readText(), pl.id) else emptyList()
        }
    }

    private fun countChannels(id: String): Int =
        M3uParser.parse(fileOf(id).readText(), id).size

    // ---------- favoritos (por URL do canal) ----------

    fun favorites(): Set<String> = prefs.getStringSet("favs", emptySet())?.toSet() ?: emptySet()

    fun setFavorites(set: Set<String>) {
        prefs.edit().putStringSet("favs", HashSet(set)).apply()
    }

    // ---------- util ----------

    private fun download(url: String, target: File) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "VLC/3.0.18 LibVLC/3.0.18")
            .build()
        client.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "Servidor respondeu HTTP ${resp.code}" }
            val body = resp.body ?: error("Resposta vazia do servidor.")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
        }
    } catch (e: Exception) {
        null
    }
}
