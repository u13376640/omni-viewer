package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.JellyfinServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JellyfinDao {
    @Query("SELECT * FROM jellyfin_servers WHERE isActive = 1 LIMIT 1")
    fun getActiveServer(): Flow<JellyfinServerEntity?>

    @Query("SELECT * FROM jellyfin_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServerDirect(): JellyfinServerEntity?

    @Query("SELECT * FROM jellyfin_servers ORDER BY lastConnected DESC")
    fun getAllServers(): Flow<List<JellyfinServerEntity>>

    @Query("SELECT * FROM jellyfin_servers ORDER BY lastConnected DESC")
    suspend fun getAllServersDirect(): List<JellyfinServerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: JellyfinServerEntity): Long

    @Update
    suspend fun updateServer(server: JellyfinServerEntity)

    @Query("UPDATE jellyfin_servers SET isActive = (CASE WHEN id = :serverId THEN 1 ELSE 0 END)")
    suspend fun setActiveServer(serverId: Long)

    @Query("DELETE FROM jellyfin_servers WHERE id = :serverId")
    suspend fun deleteServer(serverId: Long)

    @Query("DELETE FROM jellyfin_servers WHERE accessToken = 'demo_token_authenticated' OR serverUrl LIKE '%192.168.1.120%'")
    suspend fun clearDemoServers()
}
