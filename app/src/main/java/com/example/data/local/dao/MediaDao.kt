package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateAdded DESC")
    fun getMediaByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE lastPositionMs > 0 OR lastPageIndex > 0 ORDER BY dateAdded DESC LIMIT 10")
    fun getRecentMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    fun getMediaById(id: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaByIdDirect(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(items: List<MediaItemEntity>)

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Query("UPDATE media_items SET lastPageIndex = :page, totalPages = :totalPages WHERE id = :id")
    suspend fun updateReadingProgress(id: String, page: Int, totalPages: Int)

    @Query("UPDATE media_items SET lastPositionMs = :positionMs, durationMs = :durationMs WHERE id = :id")
    suspend fun updatePlaybackProgress(id: String, positionMs: Long, durationMs: Long)

    @Query("UPDATE media_items SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFav: Boolean)

    @Query("UPDATE media_items SET thumbnailUrl = :thumbnailUrl WHERE id = :id")
    suspend fun updateThumbnailUrl(id: String, thumbnailUrl: String?)

    @Query("UPDATE media_items SET thumbnailUrl = :thumbnailUrl WHERE folderName = :folderName AND (thumbnailUrl IS NULL OR thumbnailUrl = '')")
    suspend fun updateFolderThumbnailIfEmpty(folderName: String, thumbnailUrl: String)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: String)

    @Query("DELETE FROM media_items WHERE id LIKE :idPrefix || '%'")
    suspend fun deleteMediaByIdPrefix(idPrefix: String)

    @Query("DELETE FROM media_items WHERE folderId = :folderId OR id LIKE 'file_' || :folderId || '%' OR id LIKE :folderId || '%'")
    suspend fun deleteMediaByFolderId(folderId: String)

    @Query("DELETE FROM media_items WHERE folderName = :folderName")
    suspend fun deleteMediaByFolderName(folderName: String)

    @Query("DELETE FROM media_items WHERE folderId = :folderId OR folderName = :folderName")
    suspend fun deleteMediaByFolderIdOrName(folderId: String, folderName: String)

    @Query("DELETE FROM media_items WHERE uriString LIKE :uriPrefix || '%'")
    suspend fun deleteMediaByUriPrefix(uriPrefix: String)

    @Query("DELETE FROM media_items WHERE uriString LIKE '%' || :containsStr || '%'")
    suspend fun deleteMediaByUriContains(containsStr: String)

    @Query("DELETE FROM media_items WHERE mediaSource = 'DEMO' OR drawableResId IS NOT NULL")
    suspend fun clearDemoMedia()

    @Query("SELECT * FROM media_items WHERE mediaSource = 'CLOUD_URL' ORDER BY dateAdded DESC")
    suspend fun getCloudStreamsDirect(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaDirect(): List<MediaItemEntity>
}
