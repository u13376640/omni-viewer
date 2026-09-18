package com.example.ui.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoThumbnailFetcher(
    private val context: Context,
    private val data: Any
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val uri: Uri = when (data) {
            is Uri -> data
            is String -> Uri.parse(data)
            is File -> Uri.fromFile(data)
            else -> throw IllegalArgumentException("Unsupported data type for VideoThumbnailFetcher: $data")
        }

        var bitmap: Bitmap? = null

        // 1. Android Q+ ContentResolver.loadThumbnail for content URIs (instant, OS-cached)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            } catch (_: Throwable) {}
        }

        // 2. MediaMetadataRetriever fallback for local files, SAF content URIs, or pre-Q
        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    retriever.setDataSource(context, uri)
                } else {
                    val path = uri.path ?: uri.toString()
                    retriever.setDataSource(path)
                }
                // Try 1.0 second in to avoid initial black frames common in videos
                bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.frameAtTime
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            }
        }

        if (bitmap != null) {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } else {
            throw IllegalStateException("Failed to load video thumbnail for $uri")
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Any> {
        override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
            val str = when (data) {
                is Uri -> data.toString().lowercase()
                is String -> data.lowercase()
                is File -> data.name.lowercase()
                else -> return null
            }
            val isVideo = str.endsWith(".mp4") || str.endsWith(".mkv") || str.endsWith(".webm") ||
                    str.endsWith(".avi") || str.endsWith(".mov") || str.endsWith(".3gp") ||
                    str.endsWith(".m4v") || str.endsWith(".ts") || str.endsWith(".flv") ||
                    str.endsWith(".wmv") ||
                    (data is Uri && data.scheme == ContentResolver.SCHEME_CONTENT &&
                            runCatching { context.contentResolver.getType(data)?.startsWith("video/") }.getOrNull() == true)

            return if (isVideo) VideoThumbnailFetcher(context, data) else null
        }
    }
}
