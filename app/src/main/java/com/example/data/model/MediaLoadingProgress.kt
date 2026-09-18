package com.example.data.model

data class MediaLoadingProgress(
    val isVisible: Boolean = false,
    val title: String = "Importing Media...",
    val subtitle: String = "Processing files...",
    val currentFileName: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val isIndeterminate: Boolean = false,
    val isComplete: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (processedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
}
