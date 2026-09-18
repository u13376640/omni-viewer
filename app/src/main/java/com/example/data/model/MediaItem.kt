package com.example.data.model

enum class MediaType {
    MANGA,
    PHOTO,
    VIDEO,
    PDF;

    val displayName: String
        get() = when (this) {
            MANGA -> "Manga / Comic"
            PHOTO -> "Photo"
            VIDEO -> "Video"
            PDF -> "PDF Document"
        }
}

enum class MediaFormat {
    JPG,
    PNG,
    PDF,
    MP4,
    UNKNOWN;

    companion object {
        fun fromExtensionOrMime(filename: String, mimeType: String? = null): MediaFormat {
            val lower = filename.lowercase()
            val mime = mimeType?.lowercase() ?: ""
            return when {
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".3gp") ||
                lower.endsWith(".m4v") || lower.endsWith(".ts") || lower.endsWith(".flv") ||
                lower.endsWith(".wmv") || lower.endsWith(".m3u8") || mime.contains("video/") -> MP4
                lower.endsWith(".pdf") || mime.contains("application/pdf") -> PDF
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
                lower.endsWith(".bmp") || lower.endsWith(".heic") || lower.endsWith(".heif") ||
                lower.endsWith(".avif") || mime.contains("image/jpeg") || mime.contains("image/webp") -> JPG
                lower.endsWith(".png") || lower.endsWith(".gif") || mime.contains("image/png") ||
                mime.contains("image/gif") || mime.contains("image/") -> PNG
                else -> UNKNOWN
            }
        }
    }
}

enum class MediaSource {
    LOCAL,
    JELLYFIN,
    CLOUD_URL,
    DEMO
}

data class MediaItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val uriString: String,
    val mediaType: MediaType,
    val mediaFormat: MediaFormat,
    val mediaSource: MediaSource,
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
    val pages: List<String> = emptyList(),
    val pagesDrawableResIds: List<Int> = emptyList(),
    val fileSizeFormatted: String = "",
    val folderId: String? = null,
    val folderName: String? = null,
    val subfolderPath: String? = null,
    val immediateParentFolder: String? = null
)
