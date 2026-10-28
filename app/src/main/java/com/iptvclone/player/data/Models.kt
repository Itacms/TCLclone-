package com.iptvclone.player.data

enum class SourceType { URL, LOCAL }

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val tvgId: String?,
    val playlistId: String,
)

data class Playlist(
    val id: String,
    val name: String,
    val type: SourceType,
    val location: String,
    val channelCount: Int,
    val updatedAt: Long,
    val enabled: Boolean = true,
)


data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long,
)
