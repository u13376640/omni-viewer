package com.example.data.jellyfin

data class JellyfinServerInfo(
    val serverName: String,
    val version: String,
    val id: String
)

data class JellyfinAuthResult(
    val accessToken: String,
    val userId: String,
    val userName: String,
    val serverName: String
)

data class JellyfinLibraryItem(
    val id: String,
    val name: String,
    val collectionType: String, // movies, tvshows, books, photos, homevideos, music
    val primaryImageTag: String? = null
)

data class JellyfinMediaItem(
    val id: String,
    val name: String,
    val type: String, // Movie, Episode, Video, Photo, Book
    val overview: String? = null,
    val runTimeTicks: Long? = null, // 10,000,000 ticks = 1 second
    val productionYear: Int? = null,
    val container: String? = null,
    val primaryImageTag: String? = null,
    val parentId: String? = null,
    val isFolder: Boolean = false
) {
    val durationMs: Long
        get() = (runTimeTicks ?: 0L) / 10_000L
}
