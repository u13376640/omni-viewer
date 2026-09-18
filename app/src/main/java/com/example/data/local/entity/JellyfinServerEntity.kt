package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jellyfin_servers")
data class JellyfinServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverName: String,
    val serverUrl: String,
    val username: String = "",
    val accessToken: String = "",
    val userId: String = "",
    val isActive: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
)
