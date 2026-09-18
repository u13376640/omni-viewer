package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String = "",
    val uriString: String,
    val mediaType: String, // MANGA, PHOTO, VIDEO, PDF
    val mediaFormat: String, // JPG, PNG, PDF, MP4, UNKNOWN
    val mediaSource: String, // LOCAL, JELLYFIN, CLOUD_URL, DEMO
    val thumbnailUrl: String? = null,
    val drawableResId: Int? = null,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val totalPages: Int = 1,
    val lastPageIndex: Int = 0,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val jellyfinItemId: String? = null,
    val jellyfinServerId: Long? = null,
    val fileSizeFormatted: String = "",
    val folderId: String? = null,
    val folderName: String? = null,
    val subfolderPath: String? = null,
    val immediateParentFolder: String? = null
) {
    fun toDomain(): MediaItem {
        return MediaItem(
            id = id,
            title = title,
            subtitle = subtitle,
            uriString = uriString,
            mediaType = try { MediaType.valueOf(mediaType) } catch (e: Exception) { MediaType.PHOTO },
            mediaFormat = try { MediaFormat.valueOf(mediaFormat) } catch (e: Exception) { MediaFormat.UNKNOWN },
            mediaSource = try { MediaSource.valueOf(mediaSource) } catch (e: Exception) { MediaSource.LOCAL },
            thumbnailUrl = thumbnailUrl,
            drawableResId = drawableResId,
            durationMs = durationMs,
            lastPositionMs = lastPositionMs,
            totalPages = totalPages,
            lastPageIndex = lastPageIndex,
            isFavorite = isFavorite,
            dateAdded = dateAdded,
            jellyfinItemId = jellyfinItemId,
            jellyfinServerId = jellyfinServerId,
            fileSizeFormatted = fileSizeFormatted,
            folderId = folderId,
            folderName = folderName,
            subfolderPath = subfolderPath,
            immediateParentFolder = immediateParentFolder
        )
    }

    companion object {
        fun fromDomain(item: MediaItem): MediaItemEntity {
            return MediaItemEntity(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle,
                uriString = item.uriString,
                mediaType = item.mediaType.name,
                mediaFormat = item.mediaFormat.name,
                mediaSource = item.mediaSource.name,
                thumbnailUrl = item.thumbnailUrl,
                drawableResId = item.drawableResId,
                durationMs = item.durationMs,
                lastPositionMs = item.lastPositionMs,
                totalPages = item.totalPages,
                lastPageIndex = item.lastPageIndex,
                isFavorite = item.isFavorite,
                dateAdded = item.dateAdded,
                jellyfinItemId = item.jellyfinItemId,
                jellyfinServerId = item.jellyfinServerId,
                fileSizeFormatted = item.fileSizeFormatted,
                folderId = item.folderId,
                folderName = item.folderName,
                subfolderPath = item.subfolderPath,
                immediateParentFolder = item.immediateParentFolder
            )
        }
    }
}
