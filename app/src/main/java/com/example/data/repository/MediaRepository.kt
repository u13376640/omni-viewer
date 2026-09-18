package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.jellyfin.JellyfinAuthResult
import com.example.data.jellyfin.JellyfinClient
import com.example.data.jellyfin.JellyfinLibraryItem
import com.example.data.jellyfin.JellyfinMediaItem
import com.example.data.jellyfin.JellyfinServerInfo
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.JellyfinServerEntity
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.pdf.PdfEngine
import com.example.data.sample.SampleMediaProvider
import com.example.data.util.GooglePhotosHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val jellyfinClient: JellyfinClient = JellyfinClient(),
    private val pdfEngine: PdfEngine = PdfEngine(context)
) {
    private val mediaDao = database.mediaDao()
    private val jellyfinDao = database.jellyfinDao()
    private val bookmarkDao = database.bookmarkDao()

    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllMedia().map { entities ->
        entities.map { it.toDomain() }
    }

    val favorites: Flow<List<MediaItem>> = mediaDao.getFavorites().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentMedia: Flow<List<MediaItem>> = mediaDao.getRecentMedia().map { entities ->
        entities.map { it.toDomain() }
    }

    val activeJellyfinServer: Flow<JellyfinServerEntity?> = jellyfinDao.getActiveServer()

    suspend fun clearPlaceholderMedia() = withContext(Dispatchers.IO) {
        mediaDao.clearDemoMedia()
        jellyfinDao.clearDemoServers()
    }

    suspend fun removeMediaFromFolder(folderId: String, folderUriString: String, folderName: String? = null) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaByIdPrefix(folderId)
        mediaDao.deleteMediaByFolderId(folderId)
        if (!folderName.isNullOrBlank()) {
            mediaDao.deleteMediaByFolderName(folderName)
            mediaDao.deleteMediaByFolderIdOrName(folderId, folderName)
        }
        if (folderUriString.isNotBlank()) {
            mediaDao.deleteMediaByUriPrefix(folderUriString)
            val docSegment = runCatching { Uri.parse(folderUriString).lastPathSegment }.getOrNull()
            if (!docSegment.isNullOrBlank()) {
                mediaDao.deleteMediaByUriContains(docSegment)
            }
            val decoded = runCatching { Uri.decode(folderUriString) }.getOrNull()
            if (!decoded.isNullOrBlank()) {
                val lastPart = decoded.substringAfterLast("/").substringAfterLast(":")
                if (lastPart.isNotBlank()) {
                    mediaDao.deleteMediaByUriContains(lastPart)
                }
            }
        }
    }

    fun getUriDisplayName(uri: Uri): String {
        var fileName = "Local Media"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fileName
    }

    suspend fun addLocalMediaUri(uri: Uri, mimeType: String? = null): MediaItem = withContext(Dispatchers.IO) {
        val uriString = uri.toString()
        var fileName = "Local Media"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val resolvedMime = mimeType ?: context.contentResolver.getType(uri)
        val format = MediaFormat.fromExtensionOrMime(fileName, resolvedMime)
        val type = when (format) {
            MediaFormat.MP4 -> MediaType.VIDEO
            MediaFormat.PDF -> MediaType.PDF
            MediaFormat.JPG, MediaFormat.PNG -> {
                if (fileName.lowercase().contains("manga") || fileName.lowercase().contains("comic") || fileName.lowercase().contains("ch")) {
                    MediaType.MANGA
                } else {
                    MediaType.PHOTO
                }
            }
            MediaFormat.UNKNOWN -> MediaType.PHOTO
        }

        val pagesCount = if (format == MediaFormat.PDF) {
            pdfEngine.getPageCount(uriString).coerceAtLeast(1)
        } else 1

        val formattedSize = if (fileSize > 0) {
            val mb = fileSize / (1024.0 * 1024.0)
            if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", fileSize / 1024)
        } else ""

        val mediaItem = MediaItem(
            id = "local_${UUID.randomUUID()}",
            title = fileName.substringBeforeLast("."),
            subtitle = "Local ${format.name} File",
            uriString = uriString,
            mediaType = type,
            mediaFormat = format,
            mediaSource = MediaSource.LOCAL,
            totalPages = pagesCount,
            fileSizeFormatted = formattedSize
        )

        mediaDao.insertMedia(MediaItemEntity.fromDomain(mediaItem))
        mediaItem
    }

    suspend fun scanFolderUri(
        treeUri: Uri,
        onProgress: ((status: String, fileName: String, count: Int) -> Unit)? = null
    ): Pair<String, List<MediaItem>> {
        val folderId = "folder_${treeUri.hashCode()}"
        val displayName = getUriDisplayName(treeUri).ifBlank { "Media Folder" }
        return scanFolder(folderId, displayName, treeUri.toString(), onProgress)
    }

    suspend fun scanFolder(
        folderId: String,
        folderName: String,
        uriString: String,
        onProgress: ((status: String, fileName: String, count: Int) -> Unit)? = null
    ): Pair<String, List<MediaItem>> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        var resolvedFolderName = folderName.ifBlank { "Media Folder" }

        // 1. Check if demo / local sample vault
        if (uriString.startsWith("content://local/") || folderId == "folder_demo_vault") {
            onProgress?.invoke("Loading sample vault...", resolvedFolderName, 0)
            val demoMedia = SampleMediaProvider.getInitialMedia(context).map { item ->
                item.copy(
                    folderId = folderId,
                    folderName = resolvedFolderName,
                    subfolderPath = ""
                )
            }
            if (demoMedia.isNotEmpty()) {
                mediaDao.insertMediaList(demoMedia.map { MediaItemEntity.fromDomain(it) })
                items.addAll(demoMedia)
            }
            return@withContext (resolvedFolderName to items)
        }

        // 1b. Check if Cloud / Google Photos Album folder
        if (folderId.startsWith("cloud_folder_") || GooglePhotosHelper.isGooglePhotosUrl(uriString) || GooglePhotosHelper.isWebPhotoAlbumUrl(uriString)) {
            val existing = mediaDao.getAllMediaDirect()
                .filter { it.folderId == folderId || (it.folderName != null && it.folderName.equals(folderName, ignoreCase = true)) }
                .map { it.toDomain() }
            if (existing.isNotEmpty()) {
                return@withContext (resolvedFolderName to existing)
            }
        }

        // 2. SAF Content Tree URI
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
        if (uri != null && uri.scheme == "content") {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            try {
                val root = DocumentFile.fromTreeUri(context, uri)
                if (root != null && root.canRead()) {
                    val rootName = root.name
                    if (!rootName.isNullOrBlank()) {
                        resolvedFolderName = rootName
                    }
                    onProgress?.invoke("Scanning directory...", resolvedFolderName, 0)
                    scanDirectoryRecursively(
                        directory = root,
                        folderId = folderId,
                        rootFolderName = resolvedFolderName,
                        relativeSubfolderPath = "",
                        items = items,
                        maxDepth = 6,
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Filesystem fallback if SAF produced 0 items or wasn't a valid content URI
        if (items.isEmpty()) {
            val candidateFile = resolveToFile(uriString, uri)
            if (candidateFile != null && candidateFile.exists() && candidateFile.isDirectory && candidateFile.canRead()) {
                val fname = candidateFile.name
                if (fname.isNotBlank()) resolvedFolderName = fname
                onProgress?.invoke("Scanning local storage...", resolvedFolderName, 0)
                scanFileDirectoryRecursively(
                    directory = candidateFile,
                    folderId = folderId,
                    rootFolderName = resolvedFolderName,
                    relativeSubfolderPath = "",
                    items = items,
                    maxDepth = 6,
                    onProgress = onProgress
                )
            }
        }

        // 4. MediaStore fallback if still empty
        if (items.isEmpty()) {
            onProgress?.invoke("Searching MediaStore...", resolvedFolderName, 0)
            val msItems = queryMediaStoreForFolder(
                folderId = folderId,
                folderName = resolvedFolderName,
                uriString = uriString
            )
            items.addAll(msItems)
        }

        if (items.isNotEmpty()) {
            onProgress?.invoke("Saving media to database...", "Indexed ${items.size} files", items.size)
            mediaDao.insertMediaList(items.map { MediaItemEntity.fromDomain(it) })
        }

        resolvedFolderName to items
    }

    private fun resolveToFile(uriString: String, uri: Uri?): File? {
        if (uriString.startsWith("/") && File(uriString).exists()) {
            return File(uriString)
        }
        if (uri?.scheme == "file" && uri.path != null && File(uri.path!!).exists()) {
            return File(uri.path!!)
        }
        val decoded = Uri.decode(uriString)
        if (decoded.contains("primary:")) {
            val rel = decoded.substringAfter("primary:").substringBefore("?").trim('/')
            val extDir = Environment.getExternalStorageDirectory()
            val candidate = if (rel.isEmpty()) extDir else File(extDir, rel)
            if (candidate.exists()) return candidate
        }
        if (decoded.contains("raw:")) {
            val rawPath = decoded.substringAfter("raw:").substringBefore("?").trim()
            val candidate = File(rawPath)
            if (candidate.exists()) return candidate
        }
        val standardDirs = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOCUMENTS
        )
        for (std in standardDirs) {
            val f = Environment.getExternalStoragePublicDirectory(std)
            if (f.exists() && f.name.equals(uriString.substringAfterLast("/"), ignoreCase = true)) {
                return f
            }
        }
        return null
    }

    private fun queryMediaStoreForFolder(
        folderId: String,
        folderName: String,
        uriString: String
    ): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val bucketTarget = folderName.trim()

        // 1. Images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        try {
            contentResolver.query(
                imageUri,
                imageProjection,
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?",
                arrayOf(bucketTarget),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Image_$id"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else "image/jpeg"
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val itemUri = ContentUris.withAppendedId(imageUri, id)

                    val format = MediaFormat.fromExtensionOrMime(name, mime)
                    val lower = name.lowercase()
                    val type = if (lower.contains("manga") || lower.contains("comic") || lower.contains("ch")) {
                        MediaType.MANGA
                    } else {
                        MediaType.PHOTO
                    }

                    val formattedSize = if (size > 0) {
                        val mb = size / (1024.0 * 1024.0)
                        if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", size / 1024)
                    } else ""

                    result.add(
                        MediaItem(
                            id = "mediastore_img_${id}",
                            title = name.substringBeforeLast("."),
                            subtitle = "$folderName • ${format.name}",
                            uriString = itemUri.toString(),
                            mediaType = type,
                            mediaFormat = format,
                            mediaSource = MediaSource.LOCAL,
                            fileSizeFormatted = formattedSize,
                            folderId = folderId,
                            folderName = folderName,
                            subfolderPath = "",
                            immediateParentFolder = folderName,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Videos
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )
        try {
            contentResolver.query(
                videoUri,
                videoProjection,
                "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} = ?",
                arrayOf(bucketTarget),
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else "video/mp4"
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val itemUri = ContentUris.withAppendedId(videoUri, id)

                    val format = MediaFormat.fromExtensionOrMime(name, mime)
                    val formattedSize = if (size > 0) {
                        val mb = size / (1024.0 * 1024.0)
                        if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", size / 1024)
                    } else ""

                    result.add(
                        MediaItem(
                            id = "mediastore_vid_${id}",
                            title = name.substringBeforeLast("."),
                            subtitle = "$folderName • ${format.name}",
                            uriString = itemUri.toString(),
                            mediaType = MediaType.VIDEO,
                            mediaFormat = format,
                            mediaSource = MediaSource.LOCAL,
                            durationMs = duration,
                            fileSizeFormatted = formattedSize,
                            folderId = folderId,
                            folderName = folderName,
                            subfolderPath = "",
                            immediateParentFolder = folderName,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun scanFileDirectoryRecursively(
        directory: File,
        folderId: String,
        rootFolderName: String,
        relativeSubfolderPath: String,
        items: MutableList<MediaItem>,
        maxDepth: Int,
        onProgress: ((status: String, fileName: String, count: Int) -> Unit)? = null
    ) {
        if (maxDepth <= 0) return
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                val subDirName = file.name
                val nextSubfolderPath = if (relativeSubfolderPath.isEmpty()) subDirName else "$relativeSubfolderPath/$subDirName"
                onProgress?.invoke("Traversing $subDirName", subDirName, items.size)
                scanFileDirectoryRecursively(
                    directory = file,
                    folderId = folderId,
                    rootFolderName = rootFolderName,
                    relativeSubfolderPath = nextSubfolderPath,
                    items = items,
                    maxDepth = maxDepth - 1,
                    onProgress = onProgress
                )
            } else {
                val fileName = file.name
                val format = MediaFormat.fromExtensionOrMime(fileName, null)
                if (format == MediaFormat.UNKNOWN) continue

                val lower = fileName.lowercase()
                val pathLower = relativeSubfolderPath.lowercase()
                val type = when (format) {
                    MediaFormat.MP4 -> MediaType.VIDEO
                    MediaFormat.PDF -> MediaType.PDF
                    MediaFormat.JPG, MediaFormat.PNG -> {
                        if (lower.contains("manga") || lower.contains("comic") || lower.contains("ch") ||
                            pathLower.contains("manga") || pathLower.contains("comic") || pathLower.contains("chapter")) {
                            MediaType.MANGA
                        } else {
                            MediaType.PHOTO
                        }
                    }
                    MediaFormat.UNKNOWN -> MediaType.PHOTO
                }

                val size = file.length()
                val formattedSize = if (size > 0) {
                    val mb = size / (1024.0 * 1024.0)
                    if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", size / 1024)
                } else ""

                val subtitle = if (relativeSubfolderPath.isNotEmpty()) {
                    "$rootFolderName / $relativeSubfolderPath • ${format.name}"
                } else {
                    "$rootFolderName • ${format.name}"
                }

                val immediateParent = if (relativeSubfolderPath.isNotEmpty()) {
                    relativeSubfolderPath.substringAfterLast("/")
                } else {
                    rootFolderName
                }

                val item = MediaItem(
                    id = "file_${folderId}_${file.absolutePath.hashCode()}",
                    title = fileName.substringBeforeLast("."),
                    subtitle = subtitle,
                    uriString = Uri.fromFile(file).toString(),
                    mediaType = type,
                    mediaFormat = format,
                    mediaSource = MediaSource.LOCAL,
                    totalPages = 1,
                    fileSizeFormatted = formattedSize,
                    folderId = folderId,
                    folderName = rootFolderName,
                    subfolderPath = relativeSubfolderPath,
                    immediateParentFolder = immediateParent
                )
                items.add(item)
                onProgress?.invoke("Indexed $fileName", fileName, items.size)
            }
        }
    }

    private fun scanDirectoryRecursively(
        directory: DocumentFile,
        folderId: String,
        rootFolderName: String,
        relativeSubfolderPath: String,
        items: MutableList<MediaItem>,
        maxDepth: Int,
        onProgress: ((status: String, fileName: String, count: Int) -> Unit)? = null
    ) {
        if (maxDepth <= 0) return
        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                val subDirName = file.name ?: "Subfolder"
                val nextSubfolderPath = if (relativeSubfolderPath.isEmpty()) subDirName else "$relativeSubfolderPath/$subDirName"
                onProgress?.invoke("Traversing $subDirName", subDirName, items.size)
                scanDirectoryRecursively(
                    directory = file,
                    folderId = folderId,
                    rootFolderName = rootFolderName,
                    relativeSubfolderPath = nextSubfolderPath,
                    items = items,
                    maxDepth = maxDepth - 1,
                    onProgress = onProgress
                )
            } else {
                val fileName = file.name ?: continue
                val mime = file.type
                val format = MediaFormat.fromExtensionOrMime(fileName, mime)
                if (format == MediaFormat.UNKNOWN) continue

                val lower = fileName.lowercase()
                val pathLower = relativeSubfolderPath.lowercase()
                val type = when (format) {
                    MediaFormat.MP4 -> MediaType.VIDEO
                    MediaFormat.PDF -> MediaType.PDF
                    MediaFormat.JPG, MediaFormat.PNG -> {
                        if (lower.contains("manga") || lower.contains("comic") || lower.contains("ch") ||
                            pathLower.contains("manga") || pathLower.contains("comic") || pathLower.contains("chapter")) {
                            MediaType.MANGA
                        } else {
                            MediaType.PHOTO
                        }
                    }
                    MediaFormat.UNKNOWN -> MediaType.PHOTO
                }

                val size = file.length()
                val formattedSize = if (size > 0) {
                    val mb = size / (1024.0 * 1024.0)
                    if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", size / 1024)
                } else ""

                val subtitle = if (relativeSubfolderPath.isNotEmpty()) {
                    "$rootFolderName / $relativeSubfolderPath • ${format.name}"
                } else {
                    "$rootFolderName • ${format.name}"
                }

                val immediateParent = if (relativeSubfolderPath.isNotEmpty()) {
                    relativeSubfolderPath.substringAfterLast("/")
                } else {
                    rootFolderName
                }

                val item = MediaItem(
                    id = "file_${folderId}_${file.uri.toString().hashCode()}",
                    title = fileName.substringBeforeLast("."),
                    subtitle = subtitle,
                    uriString = file.uri.toString(),
                    mediaType = type,
                    mediaFormat = format,
                    mediaSource = MediaSource.LOCAL,
                    totalPages = 1,
                    fileSizeFormatted = formattedSize,
                    folderId = folderId,
                    folderName = rootFolderName,
                    subfolderPath = relativeSubfolderPath,
                    immediateParentFolder = immediateParent
                )
                items.add(item)
                onProgress?.invoke("Indexed $fileName", fileName, items.size)
            }
        }
    }

    suspend fun addCloudMedia(
        title: String,
        url: String,
        mediaType: MediaType,
        mediaFormat: MediaFormat,
        thumbnailUrl: String? = null
    ): MediaItem = withContext(Dispatchers.IO) {
        val cleanThumb = thumbnailUrl?.trim()?.ifBlank { null }
        val isGooglePhotos = GooglePhotosHelper.isGooglePhotosUrl(url)
        val isWebAlbum = isGooglePhotos || (mediaType == MediaType.PHOTO && GooglePhotosHelper.isWebPhotoAlbumUrl(url))

        if (isWebAlbum) {
            val albumInfo = if (cleanThumb == null || title.isBlank() || title.startsWith("Cloud ")) {
                GooglePhotosHelper.extractAlbumInfo(url)
            } else {
                null
            }

            val resolvedTitle = if (title.isNotBlank() && !title.startsWith("Cloud ")) {
                title
            } else {
                albumInfo?.title?.ifBlank { null } ?: title.ifBlank { "Google Photos Album" }
            }

            val resolvedCover = cleanThumb
                ?: albumInfo?.coverUrl?.ifBlank { null }
                ?: albumInfo?.photoUrls?.firstOrNull()?.ifBlank { null }
            val albumFolderId = "cloud_folder_${resolvedTitle.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")}"

            val primaryAlbumItem = MediaItem(
                id = "cloud_${UUID.randomUUID()}",
                title = resolvedTitle,
                subtitle = if (isGooglePhotos) "Google Photos Album" else "Cloud Photo Album",
                uriString = url,
                mediaType = MediaType.PHOTO,
                mediaFormat = MediaFormat.JPG,
                mediaSource = MediaSource.CLOUD_URL,
                thumbnailUrl = resolvedCover,
                folderName = resolvedTitle,
                folderId = albumFolderId,
                fileSizeFormatted = if (albumInfo != null && albumInfo.photoUrls.isNotEmpty()) "${albumInfo.photoUrls.size} Photos" else "Web Album"
            )
            mediaDao.insertMedia(MediaItemEntity.fromDomain(primaryAlbumItem))

            // If individual photos were extracted from the album, add them under the same album folder
            if (albumInfo != null && albumInfo.photoUrls.isNotEmpty()) {
                val photoEntities = albumInfo.photoUrls.take(150).mapIndexed { index, pUrl ->
                    MediaItemEntity(
                        id = "cloud_photo_${UUID.randomUUID()}",
                        title = "$resolvedTitle - Photo ${index + 1}",
                        subtitle = resolvedTitle,
                        uriString = pUrl,
                        mediaType = MediaType.PHOTO.name,
                        mediaFormat = MediaFormat.JPG.name,
                        mediaSource = MediaSource.CLOUD_URL.name,
                        thumbnailUrl = pUrl,
                        folderName = resolvedTitle,
                        folderId = albumFolderId,
                        fileSizeFormatted = "Cloud Photo"
                    )
                }
                mediaDao.insertMediaList(photoEntities)
            }

            primaryAlbumItem
        } else {
            val mediaItem = MediaItem(
                id = "cloud_${UUID.randomUUID()}",
                title = title.ifBlank { "Cloud Stream" },
                subtitle = "Cloud ${mediaFormat.name} Stream",
                uriString = url,
                mediaType = mediaType,
                mediaFormat = mediaFormat,
                mediaSource = MediaSource.CLOUD_URL,
                thumbnailUrl = cleanThumb,
                fileSizeFormatted = "Streaming"
            )
            mediaDao.insertMedia(MediaItemEntity.fromDomain(mediaItem))
            mediaItem
        }
    }

    suspend fun refreshAlbumCover(mediaItem: MediaItem): String? = withContext(Dispatchers.IO) {
        if (!GooglePhotosHelper.isGooglePhotosUrl(mediaItem.uriString) && !GooglePhotosHelper.isWebPhotoAlbumUrl(mediaItem.uriString)) {
            return@withContext null
        }
        val info = GooglePhotosHelper.extractAlbumInfo(mediaItem.uriString)
        val cover = info?.coverUrl?.ifBlank { null } ?: info?.photoUrls?.firstOrNull()?.ifBlank { null }
        if (cover != null) {
            mediaDao.updateThumbnailUrl(mediaItem.id, cover)
            if (!mediaItem.folderName.isNullOrBlank()) {
                mediaDao.updateFolderThumbnailIfEmpty(mediaItem.folderName, cover)
            }
        }
        cover
    }

    suspend fun updateThumbnailUrl(id: String, thumbnailUrl: String?) = withContext(Dispatchers.IO) {
        mediaDao.updateThumbnailUrl(id, thumbnailUrl?.trim()?.ifBlank { null })
    }

    suspend fun updateReadingProgress(id: String, page: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        mediaDao.updateReadingProgress(id, page, totalPages)
    }

    suspend fun updatePlaybackProgress(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        mediaDao.updatePlaybackProgress(id, positionMs, durationMs)
    }

    suspend fun toggleFavorite(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val existing = mediaDao.getMediaByIdDirect(item.id)
        val newFav = if (existing != null) !existing.isFavorite else !item.isFavorite
        if (existing != null) {
            mediaDao.toggleFavorite(item.id, newFav)
        } else {
            mediaDao.insertMedia(MediaItemEntity.fromDomain(item.copy(isFavorite = newFav)))
        }
        newFav
    }

    suspend fun toggleFavorite(id: String, currentFav: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.toggleFavorite(id, !currentFav)
    }

    suspend fun deleteMedia(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteMedia(id)
    }

    fun getBookmarks(mediaId: String): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarks(mediaId)

    suspend fun addBookmark(mediaId: String, pageIndex: Int, title: String = "Page ${pageIndex + 1}") = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(BookmarkEntity(mediaId = mediaId, pageIndex = pageIndex, title = title))
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(id)
    }

    // --- Jellyfin Operations ---

    suspend fun testJellyfinConnection(serverUrl: String): Result<JellyfinServerInfo> {
        return jellyfinClient.testServerConnection(serverUrl)
    }

    suspend fun authenticateJellyfin(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinServerEntity> = withContext(Dispatchers.IO) {
        val authResult = jellyfinClient.authenticate(serverUrl, username, password)
        if (authResult.isSuccess) {
            val res = authResult.getOrThrow()
            val serverEntity = JellyfinServerEntity(
                serverName = res.serverName.ifBlank { "Home Server" },
                serverUrl = serverUrl.trim().removeSuffix("/"),
                username = res.userName,
                accessToken = res.accessToken,
                userId = res.userId,
                isActive = true,
                lastConnected = System.currentTimeMillis()
            )
            val id = jellyfinDao.insertServer(serverEntity)
            jellyfinDao.setActiveServer(id)
            Result.success(serverEntity.copy(id = id))
        } else {
            Result.failure(authResult.exceptionOrNull() ?: Exception("Authentication failed"))
        }
    }

    suspend fun getJellyfinLibraries(server: JellyfinServerEntity): Result<List<JellyfinLibraryItem>> {
        return jellyfinClient.getLibraries(server.serverUrl, server.userId, server.accessToken)
    }

    suspend fun getJellyfinItems(
        server: JellyfinServerEntity,
        parentId: String?
    ): Result<List<JellyfinMediaItem>> {
        return jellyfinClient.getItemsInLibrary(server.serverUrl, server.userId, server.accessToken, parentId)
    }

    fun convertJellyfinItemToMediaItem(
        jellyfinItem: JellyfinMediaItem,
        server: JellyfinServerEntity
    ): MediaItem {
        val (mediaType, mediaFormat, streamUri, thumb) = when (jellyfinItem.type.lowercase()) {
            "book" -> {
                val stream = jellyfinClient.getImageUrl(server.serverUrl, jellyfinItem.id, width = 1200)
                val format = if (jellyfinItem.container.equals("pdf", ignoreCase = true)) MediaFormat.PDF else MediaFormat.JPG
                val type = if (format == MediaFormat.PDF) MediaType.PDF else MediaType.MANGA
                Quadruple(type, format, stream, stream)
            }
            "photo" -> {
                val stream = jellyfinClient.getImageUrl(server.serverUrl, jellyfinItem.id, width = 1920)
                Quadruple(MediaType.PHOTO, MediaFormat.JPG, stream, stream)
            }
            else -> { // Movie, Video, Episode
                val stream = jellyfinClient.getVideoDirectStreamUrl(server.serverUrl, jellyfinItem.id, server.accessToken)
                val thumbUrl = jellyfinClient.getImageUrl(server.serverUrl, jellyfinItem.id, width = 400)
                Quadruple(MediaType.VIDEO, MediaFormat.MP4, stream, thumbUrl)
            }
        }

        return MediaItem(
            id = "jf_${jellyfinItem.id}",
            title = jellyfinItem.name,
            subtitle = "Jellyfin • ${jellyfinItem.type}",
            uriString = streamUri,
            mediaType = mediaType,
            mediaFormat = mediaFormat,
            mediaSource = MediaSource.JELLYFIN,
            thumbnailUrl = thumb,
            drawableResId = null,
            durationMs = jellyfinItem.durationMs,
            jellyfinItemId = jellyfinItem.id,
            jellyfinServerId = server.id,
            totalPages = if (mediaType == MediaType.MANGA || mediaType == MediaType.PDF) 1 else 1,
            pagesDrawableResIds = emptyList()
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
