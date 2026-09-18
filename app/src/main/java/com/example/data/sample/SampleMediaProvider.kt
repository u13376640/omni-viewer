package com.example.data.sample

import android.content.Context
import com.example.R
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.pdf.PdfEngine
import java.io.File

object SampleMediaProvider {

    suspend fun getInitialMedia(context: Context): List<MediaItem> {
        val pdfFile = try {
            PdfEngine(context).createSampleComicPdf()
        } catch (e: Exception) {
            null
        }

        return listOf(
            MediaItem(
                id = "manga_aether",
                title = "Chronicles of Aether: Ch. 1",
                subtitle = "Cel-shaded fantasy adventure comic",
                uriString = "res://${R.drawable.img_manga_cover}",
                mediaType = MediaType.MANGA,
                mediaFormat = MediaFormat.JPG,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_manga_cover,
                totalPages = 5,
                lastPageIndex = 0,
                isFavorite = true,
                pagesDrawableResIds = listOf(
                    R.drawable.img_manga_cover,
                    R.drawable.img_hero_media,
                    R.drawable.img_photo_scenic,
                    R.drawable.img_manga_cover,
                    R.drawable.img_hero_media
                ),
                fileSizeFormatted = "18.4 MB"
            ),
            MediaItem(
                id = "manga_pdf_comic",
                title = "Aether Chronicles (Comic PDF)",
                subtitle = "Full PDF comic with vector pages",
                uriString = pdfFile?.absolutePath ?: "res://${R.drawable.img_manga_cover}",
                mediaType = MediaType.PDF,
                mediaFormat = MediaFormat.PDF,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_manga_cover,
                totalPages = 4,
                lastPageIndex = 0,
                isFavorite = false,
                fileSizeFormatted = "4.2 MB"
            ),
            MediaItem(
                id = "manga_cyber",
                title = "Cyber Odyssey 2099",
                subtitle = "Futuristic vertical webtoon strip",
                uriString = "res://${R.drawable.img_hero_media}",
                mediaType = MediaType.MANGA,
                mediaFormat = MediaFormat.PNG,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_hero_media,
                totalPages = 3,
                lastPageIndex = 0,
                isFavorite = false,
                pagesDrawableResIds = listOf(
                    R.drawable.img_hero_media,
                    R.drawable.img_photo_scenic,
                    R.drawable.img_manga_cover
                ),
                fileSizeFormatted = "12.1 MB"
            ),
            MediaItem(
                id = "photo_alpine",
                title = "Alpine Aurora Borealis",
                subtitle = "3840 x 2160 Ultra HD Landscape",
                uriString = "res://${R.drawable.img_photo_scenic}",
                mediaType = MediaType.PHOTO,
                mediaFormat = MediaFormat.JPG,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_photo_scenic,
                isFavorite = true,
                fileSizeFormatted = "8.6 MB"
            ),
            MediaItem(
                id = "photo_cyber",
                title = "Neo Tokyo Neon Rain",
                subtitle = "3840 x 2160 Night Photography",
                uriString = "res://${R.drawable.img_hero_media}",
                mediaType = MediaType.PHOTO,
                mediaFormat = MediaFormat.JPG,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_hero_media,
                isFavorite = false,
                fileSizeFormatted = "7.9 MB"
            ),
            MediaItem(
                id = "photo_character",
                title = "Rune Wanderer Portrait",
                subtitle = "High Resolution Character Illustration",
                uriString = "res://${R.drawable.img_manga_cover}",
                mediaType = MediaType.PHOTO,
                mediaFormat = MediaFormat.PNG,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_manga_cover,
                isFavorite = false,
                fileSizeFormatted = "6.3 MB"
            ),
            MediaItem(
                id = "video_bunny",
                title = "Big Buck Bunny (MP4)",
                subtitle = "Open source cinema showcase",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                mediaType = MediaType.VIDEO,
                mediaFormat = MediaFormat.MP4,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_hero_media,
                durationMs = 596_000L, // ~9 min 56 sec
                lastPositionMs = 0L,
                isFavorite = true,
                fileSizeFormatted = "158 MB"
            ),
            MediaItem(
                id = "video_tears",
                title = "Tears of Steel (MP4)",
                subtitle = "Sci-Fi cinematic video stream",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                mediaType = MediaType.VIDEO,
                mediaFormat = MediaFormat.MP4,
                mediaSource = MediaSource.DEMO,
                thumbnailUrl = null,
                drawableResId = R.drawable.img_photo_scenic,
                durationMs = 734_000L,
                lastPositionMs = 0L,
                isFavorite = false,
                fileSizeFormatted = "182 MB"
            )
        )
    }
}
