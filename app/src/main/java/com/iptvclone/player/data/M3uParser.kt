package com.iptvclone.player.data

object M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String, playlistId: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name: String? = null
        var logo: String? = null
        var group: String? = null
        var tvgId: String? = null
        var extGroup: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val lastQuote = line.lastIndexOf('"')
                    val comma = line.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                    val header = if (comma >= 0) line.substring(0, comma) else line
                    val title = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    val attrs = attrRegex.findAll(header)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    name = title.ifBlank { attrs["tvg-name"].orEmpty() }
                    logo = attrs["tvg-logo"]
                    group = attrs["group-title"]
                    tvgId = attrs["tvg-id"]
                }
                line.startsWith("#EXTGRP", ignoreCase = true) ->
                    extGroup = line.substringAfter(':', "").trim()
                line.startsWith("#") -> Unit
                else -> {
                    if (line.contains("://")) {
                        val finalName = name?.takeIf { it.isNotBlank() }
                            ?: line.substringAfterLast('/').substringBefore('?').ifBlank { line }
                        val finalGroup = (group ?: extGroup).orEmpty().ifBlank { "Sem grupo" }
                        out += Channel(
                            name = finalName,
                            url = line,
                            logo = logo?.ifBlank { null },
                            group = finalGroup,
                            tvgId = tvgId?.ifBlank { null },
                            playlistId = playlistId,
                        )
                    }
                    name = null; logo = null; group = null; tvgId = null
                }
            }
        }
        return out
    }
}
