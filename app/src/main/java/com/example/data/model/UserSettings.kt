package com.example.data.model

enum class ReadingDirection(val label: String, val shortLabel: String, val description: String) {
    LEFT_TO_RIGHT(
        label = "Left to Right (Horizontal)",
        shortLabel = "Left to Right",
        description = "Western horizontal page swipe (Left to Right) for all PDFs, comics, and manga"
    ),
    VERTICAL_CONTINUOUS(
        label = "Vertical Continuous (Webtoon)",
        shortLabel = "Vertical Webtoon",
        description = "Continuous vertical scrolling stream for all PDFs, webtoons, comics, and photos"
    )
}

enum class VideoAspectMode(val label: String) {
    FIT("Fit Screen"),
    FILL_CROP("Crop to Fill"),
    STRETCH("Stretch")
}

data class MediaFolder(
    val id: String,
    val name: String,
    val uriString: String,
    val fileCount: Int = 0,
    val lastScannedMs: Long = System.currentTimeMillis(),
    val coverUrl: String? = null
)

data class UserSettings(
    val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val videoAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val autoPlayVideos: Boolean = true,
    val configuredFolders: List<MediaFolder> = emptyList()
)

