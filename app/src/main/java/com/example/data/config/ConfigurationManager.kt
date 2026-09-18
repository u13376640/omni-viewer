package com.example.data.config

import android.content.Context
import android.net.Uri
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.JellyfinDao
import com.example.data.local.dao.MediaDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.JellyfinServerEntity
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.MediaFolder
import com.example.data.model.MediaFormat
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.ReadingDirection
import com.example.data.model.VideoAspectMode
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

data class ConfigFileStats(
    val fileName: String = "omniviewer_config.json",
    val exists: Boolean = false,
    val sizeBytes: Long = 0L,
    val lastModifiedMs: Long = 0L,
    val folderCount: Int = 0,
    val cloudStreamCount: Int = 0,
    val hasJellyfin: Boolean = false,
    val jellyfinServerUrl: String = "",
    val lastExportStatus: String? = null
) {
    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            return if (sizeBytes < 1024) "$sizeBytes B"
            else String.format(Locale.getDefault(), "%.1f KB", sizeBytes / 1024.0)
        }
}

data class ConfigImportSummary(
    val success: Boolean,
    val message: String,
    val folderCount: Int = 0,
    val cloudStreamCount: Int = 0,
    val hasJellyfin: Boolean = false,
    val jellyfinServerUrl: String = "",
    val bookmarksCount: Int = 0,
    val mediaItemsCount: Int = 0
)

class ConfigurationManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val mediaDao: MediaDao,
    private val jellyfinDao: JellyfinDao,
    private val bookmarkDao: BookmarkDao
) {
    private val configFileName = "omniviewer_config.json"
    private val localConfigFile: File
        get() = File(context.filesDir, configFileName)

    private val _configFileStats = MutableStateFlow(ConfigFileStats())
    val configFileStats: StateFlow<ConfigFileStats> = _configFileStats.asStateFlow()

    suspend fun refreshStats() = withContext(Dispatchers.IO) {
        val file = localConfigFile
        val exists = file.exists()
        val size = if (exists) file.length() else 0L
        val lastMod = if (exists) file.lastModified() else 0L
        val folders = settingsRepository.settings.value.configuredFolders
        val cloudStreams = mediaDao.getCloudStreamsDirect()
        val activeServer = jellyfinDao.getActiveServerDirect()

        _configFileStats.value = _configFileStats.value.copy(
            fileName = configFileName,
            exists = exists,
            sizeBytes = size,
            lastModifiedMs = lastMod,
            folderCount = folders.size,
            cloudStreamCount = cloudStreams.size,
            hasJellyfin = activeServer != null && activeServer.serverUrl.isNotBlank(),
            jellyfinServerUrl = activeServer?.serverUrl ?: settingsRepository.getJellyfinServerUrl()
        )
    }

    suspend fun generateFullConfigJsonString(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "OmniViewer")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Settings (Reading & Playback)
        val settings = settingsRepository.settings.value
        val settingsObj = JSONObject().apply {
            put("readingDirection", settings.readingDirection.name)
            put("videoAspectMode", settings.videoAspectMode.name)
            put("autoPlayVideos", settings.autoPlayVideos)
        }
        root.put("settings", settingsObj)

        // 2. Configured Folders
        val foldersArray = JSONArray()
        settings.configuredFolders.forEach { f ->
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("uriString", f.uriString)
                put("fileCount", f.fileCount)
                put("lastScannedMs", f.lastScannedMs)
                put("coverUrl", f.coverUrl ?: "")
            }
            foldersArray.put(fObj)
        }
        root.put("configuredFolders", foldersArray)

        // 3. Jellyfin Configuration (IP, login, password, token)
        val activeServer = jellyfinDao.getActiveServerDirect()
        val storedUrl = settingsRepository.getJellyfinServerUrl()
        val storedUser = settingsRepository.getJellyfinUsername()
        val storedPass = settingsRepository.getJellyfinPassword()

        if (activeServer != null || storedUrl.isNotBlank()) {
            val jfObj = JSONObject().apply {
                put("serverName", activeServer?.serverName ?: "Jellyfin Server")
                put("serverUrl", activeServer?.serverUrl ?: storedUrl)
                put("username", activeServer?.username?.ifBlank { storedUser } ?: storedUser)
                put("password", storedPass)
                put("accessToken", activeServer?.accessToken ?: "")
                put("userId", activeServer?.userId ?: "")
                put("isActive", activeServer?.isActive ?: true)
            }
            root.put("jellyfin", jfObj)
        } else {
            root.put("jellyfin", JSONObject.NULL)
        }

        // 4. Saved Cloud Streams
        val cloudStreams = mediaDao.getCloudStreamsDirect()
        val cloudArray = JSONArray()
        cloudStreams.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("subtitle", c.subtitle)
                put("url", c.uriString)
                put("mediaType", c.mediaType)
                put("mediaFormat", c.mediaFormat)
                put("thumbnailUrl", c.thumbnailUrl ?: "")
                put("isFavorite", c.isFavorite)
                put("durationMs", c.durationMs)
                put("lastPositionMs", c.lastPositionMs)
                put("dateAdded", c.dateAdded)
                put("folderId", c.folderId ?: "")
                put("folderName", c.folderName ?: "")
            }
            cloudArray.put(cObj)
        }
        root.put("savedCloudStreams", cloudArray)

        // 5. Bookmarks
        val bookmarks = bookmarkDao.getAllBookmarksDirect()
        val bookmarksArray = JSONArray()
        bookmarks.forEach { b ->
            val bObj = JSONObject().apply {
                put("mediaId", b.mediaId)
                put("pageIndex", b.pageIndex)
                put("title", b.title)
            }
            bookmarksArray.put(bObj)
        }
        root.put("bookmarks", bookmarksArray)

        // 6. Media Items (Local & Indexed)
        val allMediaEntities = mediaDao.getAllMediaDirect()
        val mediaArray = JSONArray()
        allMediaEntities.forEach { m ->
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("subtitle", m.subtitle)
                put("uriString", m.uriString)
                put("mediaType", m.mediaType)
                put("mediaFormat", m.mediaFormat)
                put("mediaSource", m.mediaSource)
                put("thumbnailUrl", m.thumbnailUrl ?: "")
                put("fileSizeFormatted", m.fileSizeFormatted)
                put("folderId", m.folderId ?: "")
                put("folderName", m.folderName ?: "")
                put("subfolderPath", m.subfolderPath ?: "")
                put("immediateParentFolder", m.immediateParentFolder ?: "")
                put("durationMs", m.durationMs)
                put("lastPositionMs", m.lastPositionMs)
                put("totalPages", m.totalPages)
                put("lastPageIndex", m.lastPageIndex)
                put("isFavorite", m.isFavorite)
                put("dateAdded", m.dateAdded)
            }
            mediaArray.put(mObj)
        }
        root.put("mediaItems", mediaArray)

        root.toString(2)
    }

    suspend fun autoSaveToFile(): File = withContext(Dispatchers.IO) {
        val jsonString = generateFullConfigJsonString()
        val file = localConfigFile
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }
        }
        refreshStats()
        file
    }

    suspend fun exportToFileUri(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = generateFullConfigJsonString()
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                os.write(bytes)
                os.flush()
            } ?: throw IllegalStateException("Cannot open output stream for $targetUri")

            // Also keep local auto-save updated
            autoSaveToFile()

            _configFileStats.value = _configFileStats.value.copy(
                lastExportStatus = "Exported ${bytes.size} bytes to file"
            )
            bytes.size
        }
    }

    suspend fun importFromUri(sourceUri: Uri): Result<ConfigImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val buffer = CharArray(1024)
                    var read: Int
                    while (reader.read(buffer).also { read = it } != -1) {
                        stringBuilder.append(buffer, 0, read)
                    }
                }
            } ?: throw IllegalStateException("Cannot read file from $sourceUri")

            importFromJsonString(stringBuilder.toString()).getOrThrow()
        }
    }

    suspend fun importFromJsonString(jsonString: String): Result<ConfigImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonString)

            // 1. Settings
            val currentSettings = settingsRepository.settings.value
            var newDirection = currentSettings.readingDirection
            var newAspectMode = currentSettings.videoAspectMode
            var newAutoPlay = currentSettings.autoPlayVideos

            if (root.has("settings") && !root.isNull("settings")) {
                val sObj = root.getJSONObject("settings")
                val dirStr = sObj.optString("readingDirection", "")
                if (dirStr.isNotBlank()) {
                    newDirection = runCatching { ReadingDirection.valueOf(dirStr) }.getOrDefault(newDirection)
                }
                val aspectStr = sObj.optString("videoAspectMode", "")
                if (aspectStr.isNotBlank()) {
                    newAspectMode = runCatching { VideoAspectMode.valueOf(aspectStr) }.getOrDefault(newAspectMode)
                }
                if (sObj.has("autoPlayVideos")) {
                    newAutoPlay = sObj.optBoolean("autoPlayVideos", newAutoPlay)
                }
            }

            // 2. Configured Folders
            val restoredFolders = mutableListOf<MediaFolder>()
            val foldersArrayKey = when {
                root.has("configuredFolders") && !root.isNull("configuredFolders") -> "configuredFolders"
                root.has("folders") && !root.isNull("folders") -> "folders"
                root.has("mediaFolders") && !root.isNull("mediaFolders") -> "mediaFolders"
                else -> null
            }
            if (foldersArrayKey != null) {
                val fArray = root.getJSONArray(foldersArrayKey)
                for (i in 0 until fArray.length()) {
                    val fObj = fArray.getJSONObject(i)
                    val id = fObj.optString("id", "folder_${System.currentTimeMillis()}_$i")
                    val name = fObj.optString("name", fObj.optString("folderName", fObj.optString("title", fObj.optString("displayName", "Media Folder"))))
                    val uri = fObj.optString("uriString", fObj.optString("uri", fObj.optString("path", fObj.optString("folderPath", fObj.optString("directory", "")))))
                    val count = fObj.optInt("fileCount", fObj.optInt("count", 0))
                    val lastScanned = fObj.optLong("lastScannedMs", System.currentTimeMillis())
                    val coverUrl = fObj.optString("coverUrl").ifBlank { null }
                    restoredFolders.add(
                        MediaFolder(
                            id = id,
                            name = name,
                            uriString = uri,
                            fileCount = count,
                            lastScannedMs = lastScanned,
                            coverUrl = coverUrl
                        )
                    )
                }
            }

            // Apply settings & folders
            settingsRepository.restoreFullSettings(
                direction = newDirection,
                aspectMode = newAspectMode,
                autoPlay = newAutoPlay,
                folders = restoredFolders
            )

            // 3. Jellyfin Configuration (IP, login, password)
            var hasJellyfin = false
            var jfServerUrl = ""
            if (root.has("jellyfin") && !root.isNull("jellyfin")) {
                val jObj = root.getJSONObject("jellyfin")
                val serverName = jObj.optString("serverName", "Home Server")
                val serverUrl = jObj.optString("serverUrl", "")
                val username = jObj.optString("username", "")
                val password = jObj.optString("password", "")
                val token = jObj.optString("accessToken", "")
                val userId = jObj.optString("userId", "")
                val isActive = jObj.optBoolean("isActive", true)

                if (serverUrl.isNotBlank()) {
                    settingsRepository.saveJellyfinCredentials(serverUrl, username, password)
                    val serverEntity = JellyfinServerEntity(
                        serverName = serverName,
                        serverUrl = serverUrl.trim().removeSuffix("/"),
                        username = username,
                        accessToken = token,
                        userId = userId,
                        isActive = isActive,
                        lastConnected = System.currentTimeMillis()
                    )
                    val id = jellyfinDao.insertServer(serverEntity)
                    if (isActive) {
                        jellyfinDao.setActiveServer(id)
                    }
                    hasJellyfin = true
                    jfServerUrl = serverUrl
                }
            }

            // 4. Saved Cloud Streams
            var streamCount = 0
            val cloudStreamsKey = when {
                root.has("savedCloudStreams") && !root.isNull("savedCloudStreams") -> "savedCloudStreams"
                root.has("cloudStreams") && !root.isNull("cloudStreams") -> "cloudStreams"
                root.has("streams") && !root.isNull("streams") -> "streams"
                root.has("saved_cloud_streams") && !root.isNull("saved_cloud_streams") -> "saved_cloud_streams"
                else -> null
            }
            if (cloudStreamsKey != null) {
                val sArray = root.getJSONArray(cloudStreamsKey)
                val entities = mutableListOf<MediaItemEntity>()
                for (i in 0 until sArray.length()) {
                    val sObj = sArray.getJSONObject(i)
                    val url = sObj.optString("url", "").ifBlank {
                        sObj.optString("uriString", "").ifBlank {
                            sObj.optString("uri", "").ifBlank {
                                sObj.optString("streamUrl", "")
                            }
                        }
                    }
                    if (url.isNotBlank()) {
                        val mediaTypeStr = sObj.optString("mediaType", "VIDEO")
                        val mediaFormatStr = sObj.optString("mediaFormat", "MP4")
                        val title = sObj.optString("title", "Cloud Stream")
                        val subtitle = sObj.optString("subtitle", "Cloud $mediaFormatStr Stream")
                        val id = sObj.optString("id", "cloud_${java.util.UUID.randomUUID()}")
                        val thumbUrl = sObj.optString("thumbnailUrl", "").ifBlank {
                            sObj.optString("thumbnail", "").ifBlank {
                                sObj.optString("thumb", "").ifBlank {
                                    sObj.optString("thumbnail_url", "").ifBlank { null }
                                }
                            }
                        }

                        val fId = sObj.optString("folderId", "").ifBlank { null }
                        val fName = sObj.optString("folderName", "").ifBlank { null }

                        entities.add(
                            MediaItemEntity(
                                id = id,
                                title = title,
                                subtitle = subtitle,
                                uriString = url,
                                mediaType = mediaTypeStr,
                                mediaFormat = mediaFormatStr,
                                mediaSource = MediaSource.CLOUD_URL.name,
                                thumbnailUrl = thumbUrl,
                                folderId = fId,
                                folderName = fName,
                                isFavorite = sObj.optBoolean("isFavorite", false),
                                durationMs = sObj.optLong("durationMs", 0L),
                                lastPositionMs = sObj.optLong("lastPositionMs", 0L),
                                dateAdded = sObj.optLong("dateAdded", System.currentTimeMillis()),
                                fileSizeFormatted = "Streaming"
                            )
                        )
                    }
                }
                if (entities.isNotEmpty()) {
                    mediaDao.insertMediaList(entities)
                    streamCount = entities.size
                }
            }

            // 5. Bookmarks
            var bookmarksCount = 0
            if (root.has("bookmarks") && !root.isNull("bookmarks")) {
                val bArray = root.getJSONArray("bookmarks")
                val bEntities = mutableListOf<BookmarkEntity>()
                for (i in 0 until bArray.length()) {
                    val bObj = bArray.getJSONObject(i)
                    val mediaId = bObj.optString("mediaId", "")
                    if (mediaId.isNotBlank()) {
                        bEntities.add(
                            BookmarkEntity(
                                mediaId = mediaId,
                                pageIndex = bObj.optInt("pageIndex", 0),
                                title = bObj.optString("title", "Bookmark")
                            )
                        )
                    }
                }
                if (bEntities.isNotEmpty()) {
                    bookmarkDao.insertBookmarks(bEntities)
                    bookmarksCount = bEntities.size
                }
            }

            // 6. Media Items (Local & Indexed items from previous export)
            var mediaItemsCount = 0
            val mediaArrayKey = when {
                root.has("mediaItems") && !root.isNull("mediaItems") -> "mediaItems"
                root.has("items") && !root.isNull("items") -> "items"
                root.has("media") && !root.isNull("media") -> "media"
                root.has("localMedia") && !root.isNull("localMedia") -> "localMedia"
                else -> null
            }
            if (mediaArrayKey != null) {
                val mArray = root.getJSONArray(mediaArrayKey)
                val mEntities = mutableListOf<MediaItemEntity>()
                for (i in 0 until mArray.length()) {
                    val mObj = mArray.getJSONObject(i)
                    val id = mObj.optString("id", "imported_${java.util.UUID.randomUUID()}")
                    val uri = mObj.optString("uriString", mObj.optString("uri", mObj.optString("url", "")))
                    if (uri.isNotBlank()) {
                        val title = mObj.optString("title", "Media Item")
                        val subtitle = mObj.optString("subtitle", "")
                        val mType = mObj.optString("mediaType", "PHOTO")
                        val mFormat = mObj.optString("mediaFormat", "JPG")
                        val mSource = mObj.optString("mediaSource", "LOCAL")
                        val thumb = mObj.optString("thumbnailUrl", null)
                        val sizeStr = mObj.optString("fileSizeFormatted", "")
                        val fId = mObj.optString("folderId", null)
                        val fName = mObj.optString("folderName", null)
                        val subPath = mObj.optString("subfolderPath", null)
                        val immParent = mObj.optString("immediateParentFolder", null)
                        val duration = mObj.optLong("durationMs", 0L)
                        val lastPos = mObj.optLong("lastPositionMs", 0L)
                        val pages = mObj.optInt("totalPages", 1)
                        val lastPage = mObj.optInt("lastPageIndex", 0)
                        val fav = mObj.optBoolean("isFavorite", false)
                        val date = mObj.optLong("dateAdded", System.currentTimeMillis())

                        mEntities.add(
                            MediaItemEntity(
                                id = id,
                                title = title,
                                subtitle = subtitle,
                                uriString = uri,
                                mediaType = mType,
                                mediaFormat = mFormat,
                                mediaSource = mSource,
                                thumbnailUrl = thumb,
                                fileSizeFormatted = sizeStr,
                                folderId = fId,
                                folderName = fName,
                                subfolderPath = subPath,
                                immediateParentFolder = immParent,
                                durationMs = duration,
                                lastPositionMs = lastPos,
                                totalPages = pages,
                                lastPageIndex = lastPage,
                                isFavorite = fav,
                                dateAdded = date
                            )
                        )
                    }
                }
                if (mEntities.isNotEmpty()) {
                    mediaDao.insertMediaList(mEntities)
                    mediaItemsCount = mEntities.size
                }
            }

            // Save the imported state to local omniviewer_config.json
            autoSaveToFile()

            ConfigImportSummary(
                success = true,
                message = "Configuration successfully imported!",
                folderCount = restoredFolders.size,
                cloudStreamCount = streamCount,
                hasJellyfin = hasJellyfin,
                jellyfinServerUrl = jfServerUrl,
                bookmarksCount = bookmarksCount,
                mediaItemsCount = mediaItemsCount
            )
        }
    }
}
