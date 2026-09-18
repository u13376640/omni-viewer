package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.jellyfin.JellyfinLibraryItem
import com.example.data.jellyfin.JellyfinMediaItem
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.JellyfinServerEntity
import com.example.data.model.MediaFolder
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaLoadingProgress
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.ReadingDirection
import com.example.data.model.UserSettings
import com.example.data.model.VideoAspectMode
import com.example.data.repository.MediaRepository
import com.example.data.repository.SettingsRepository
import com.example.data.util.GooglePhotosHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BottomNavTab {
    HOME,
    OFFLINE,
    CLOUD,
    SETTINGS
}

enum class CloudOption {
    JELLYFIN,
    STREAM_URL
}

enum class HomeCategory {
    ALL,
    COMICS,
    PHOTOS,
    VIDEOS,
    FAVORITES
}

enum class OfflineCategory {
    ALL,
    COMICS,
    PHOTOS,
    VIDEOS,
    DOCUMENTS
}

enum class SortOrder(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_DESC("Newest"),
    DATE_ASC("Oldest")
}

data class FolderNavNode(
    val folderId: String? = null,
    val folderName: String,
    val subfolderPath: String = "" // "" for root of folder, "Season 1", "Season 1/Special"
)

fun naturalCompare(s1: String, s2: String): Int {
    val regex = Regex("(\\d+)|(\\D+)")
    val tokens1 = regex.findAll(s1).map { it.value }.toList()
    val tokens2 = regex.findAll(s2).map { it.value }.toList()
    val minSize = minOf(tokens1.size, tokens2.size)
    for (i in 0 until minSize) {
        val t1 = tokens1[i]
        val t2 = tokens2[i]
        val num1 = t1.toLongOrNull()
        val num2 = t2.toLongOrNull()
        if (num1 != null && num2 != null) {
            val cmp = num1.compareTo(num2)
            if (cmp != 0) return cmp
        } else {
            val cmp = t1.compareTo(t2, ignoreCase = true)
            if (cmp != 0) return cmp
        }
    }
    return tokens1.size.compareTo(tokens2.size)
}

// For backwards compatibility
enum class MainTab {
    ALL,
    MANGA,
    PHOTOS,
    VIDEOS,
    JELLYFIN,
    FAVORITES
}

sealed interface ActiveViewer {
    object None : ActiveViewer
    data class Manga(val item: MediaItem) : ActiveViewer
    data class Photo(val item: MediaItem, val allPhotos: List<MediaItem>) : ActiveViewer
    data class GooglePhotosAlbum(val item: MediaItem, val photos: List<MediaItem> = emptyList()) : ActiveViewer
    data class Video(val item: MediaItem) : ActiveViewer
    data class Pdf(val item: MediaItem) : ActiveViewer
}

data class JellyfinUiState(
    val isConnecting: Boolean = false,
    val connectedServer: JellyfinServerEntity? = null,
    val errorMessage: String? = null,
    val libraries: List<JellyfinLibraryItem> = emptyList(),
    val selectedLibrary: JellyfinLibraryItem? = null,
    val libraryItems: List<JellyfinMediaItem> = emptyList(),
    val isLoadingItems: Boolean = false
)

class OmniViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val database = com.example.data.local.AppDatabase.getDatabase(application)

    val configManager = com.example.data.config.ConfigurationManager(
        context = application,
        settingsRepository = settingsRepository,
        mediaDao = database.mediaDao(),
        jellyfinDao = database.jellyfinDao(),
        bookmarkDao = database.bookmarkDao()
    )

    val configFileStats: StateFlow<com.example.data.config.ConfigFileStats> = configManager.configFileStats
    val configImportExportMessage = MutableStateFlow<String?>(null)

    val userSettings: StateFlow<UserSettings> = settingsRepository.settings

    private val _currentNavTab = MutableStateFlow(BottomNavTab.HOME)
    val currentNavTab: StateFlow<BottomNavTab> = _currentNavTab.asStateFlow()

    private val _cloudOption = MutableStateFlow(CloudOption.JELLYFIN)
    val cloudOption: StateFlow<CloudOption> = _cloudOption.asStateFlow()

    private val _homeCategory = MutableStateFlow(HomeCategory.ALL)
    val homeCategory: StateFlow<HomeCategory> = _homeCategory.asStateFlow()

    private val _offlineCategory = MutableStateFlow(OfflineCategory.ALL)
    val offlineCategory: StateFlow<OfflineCategory> = _offlineCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _folderNavStack = MutableStateFlow<List<FolderNavNode>>(emptyList())
    val folderNavStack: StateFlow<List<FolderNavNode>> = _folderNavStack.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.ALL)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeViewer = MutableStateFlow<ActiveViewer>(ActiveViewer.None)
    val activeViewer: StateFlow<ActiveViewer> = _activeViewer.asStateFlow()

    private val _selectedDetailItem = MutableStateFlow<MediaItem?>(null)
    val selectedDetailItem: StateFlow<MediaItem?> = _selectedDetailItem.asStateFlow()

    private val _showConnectDialog = MutableStateFlow(false)
    val showConnectDialog: StateFlow<Boolean> = _showConnectDialog.asStateFlow()

    private val _showAddUrlDialog = MutableStateFlow(false)
    val showAddUrlDialog: StateFlow<Boolean> = _showAddUrlDialog.asStateFlow()

    private val _mediaLoadingProgress = MutableStateFlow(MediaLoadingProgress())
    val mediaLoadingProgress: StateFlow<MediaLoadingProgress> = _mediaLoadingProgress.asStateFlow()

    fun dismissLoadingProgress() {
        _mediaLoadingProgress.value = MediaLoadingProgress(isVisible = false)
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun enterFolder(folderId: String?, folderName: String, subfolderPath: String = "") {
        _folderNavStack.value = _folderNavStack.value + FolderNavNode(
            folderId = folderId,
            folderName = folderName,
            subfolderPath = subfolderPath
        )
    }

    fun navigateUpFolder() {
        val current = _folderNavStack.value
        if (current.isNotEmpty()) {
            _folderNavStack.value = current.dropLast(1)
        }
    }

    fun navigateToFolderCrumb(index: Int) {
        val current = _folderNavStack.value
        if (index >= 0 && index < current.size) {
            _folderNavStack.value = current.take(index + 1)
        }
    }

    fun navigateToHomeRoot() {
        _folderNavStack.value = emptyList()
    }

    private val _jellyfinState = MutableStateFlow(JellyfinUiState())
    val jellyfinState: StateFlow<JellyfinUiState> = _jellyfinState.asStateFlow()

    val allMedia: StateFlow<List<MediaItem>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMedia: StateFlow<List<MediaItem>> = repository.recentMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered media for HOME dashboard, sorted according to sortOrder (Name A-Z by default)
    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        allMedia,
        _homeCategory,
        _searchQuery,
        _sortOrder
    ) { media, category, query, sort ->
        val catFiltered = when (category) {
            HomeCategory.ALL -> media
            HomeCategory.COMICS -> media.filter { it.mediaType == MediaType.MANGA || it.mediaType == MediaType.PDF }
            HomeCategory.PHOTOS -> media.filter { it.mediaType == MediaType.PHOTO }
            HomeCategory.VIDEOS -> media.filter { it.mediaType == MediaType.VIDEO }
            HomeCategory.FAVORITES -> media.filter { it.isFavorite }
        }
        val searched = if (query.isBlank()) {
            catFiltered
        } else {
            catFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
            }
        }
        when (sort) {
            SortOrder.NAME_ASC -> searched.sortedWith { a, b -> naturalCompare(a.title, b.title) }
            SortOrder.NAME_DESC -> searched.sortedWith { a, b -> naturalCompare(b.title, a.title) }
            SortOrder.DATE_DESC -> searched.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> searched.sortedBy { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered media for OFFLINE tab
    val offlineMedia: StateFlow<List<MediaItem>> = combine(
        allMedia,
        _offlineCategory,
        _searchQuery,
        _sortOrder
    ) { media, category, query, sort ->
        val locals = media.filter { it.mediaSource == MediaSource.LOCAL || it.drawableResId != null }
        val catFiltered = when (category) {
            OfflineCategory.ALL -> locals
            OfflineCategory.COMICS -> locals.filter { it.mediaType == MediaType.MANGA }
            OfflineCategory.PHOTOS -> locals.filter { it.mediaType == MediaType.PHOTO }
            OfflineCategory.VIDEOS -> locals.filter { it.mediaType == MediaType.VIDEO }
            OfflineCategory.DOCUMENTS -> locals.filter { it.mediaType == MediaType.PDF }
        }
        val searched = if (query.isBlank()) {
            catFiltered
        } else {
            catFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
            }
        }
        when (sort) {
            SortOrder.NAME_ASC -> searched.sortedWith { a, b -> naturalCompare(a.title, b.title) }
            SortOrder.NAME_DESC -> searched.sortedWith { a, b -> naturalCompare(b.title, a.title) }
            SortOrder.DATE_DESC -> searched.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> searched.sortedBy { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stream URLs list for CLOUD tab
    val cloudStreams: StateFlow<List<MediaItem>> = combine(
        allMedia,
        _searchQuery
    ) { media, query ->
        val cloudItems = media.filter { it.mediaSource == MediaSource.CLOUD_URL }
        if (query.isBlank()) {
            cloudItems
        } else {
            cloudItems.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.clearPlaceholderMedia()
            configManager.autoSaveToFile()
            configManager.refreshStats()
            repository.activeJellyfinServer.collect { server ->
                _jellyfinState.value = _jellyfinState.value.copy(connectedServer = server)
                if (server != null) {
                    loadJellyfinLibraries(server)
                }
            }
        }
    }

    // Navigation and tabs
    fun selectNavTab(tab: BottomNavTab) {
        _currentNavTab.value = tab
    }

    fun selectCloudOption(option: CloudOption) {
        _cloudOption.value = option
    }

    fun setHomeCategory(category: HomeCategory) {
        _homeCategory.value = category
    }

    fun setOfflineCategory(category: OfflineCategory) {
        _offlineCategory.value = category
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Media Viewer Navigation
    fun openMedia(item: MediaItem, contextPhotos: List<MediaItem>? = null) {
        when (item.mediaType) {
            MediaType.MANGA -> _activeViewer.value = ActiveViewer.Manga(item)
            MediaType.PHOTO -> {
                if (GooglePhotosHelper.isGooglePhotosUrl(item.uriString) || item.subtitle == "Google Photos Album") {
                    val albumPhotos = allMedia.value.filter {
                        it.id != item.id && (
                            (item.folderId != null && it.folderId == item.folderId) ||
                            (item.folderName != null && it.folderName.equals(item.folderName, ignoreCase = true))
                        )
                    }
                    _activeViewer.value = ActiveViewer.GooglePhotosAlbum(item, albumPhotos)
                    return
                }

                val currentSort = _sortOrder.value
                val basePhotos = if (!contextPhotos.isNullOrEmpty() && contextPhotos.any { it.id == item.id }) {
                    contextPhotos.filter { it.mediaType == MediaType.PHOTO }
                } else when {
                    _folderNavStack.value.isNotEmpty() -> {
                        val currentNode = _folderNavStack.value.last()
                        val folderPhotos = allMedia.value.filter {
                            it.mediaType == MediaType.PHOTO &&
                            ((currentNode.folderId != null && it.folderId == currentNode.folderId) ||
                             it.folderName.equals(currentNode.folderName, ignoreCase = true))
                        }
                        if (folderPhotos.any { it.id == item.id }) folderPhotos else allMedia.value.filter { it.mediaType == MediaType.PHOTO }
                    }
                    _searchQuery.value.isNotBlank() -> {
                        val searchedPhotos = filteredMedia.value.filter { it.mediaType == MediaType.PHOTO }
                        if (searchedPhotos.any { it.id == item.id }) searchedPhotos else allMedia.value.filter { it.mediaType == MediaType.PHOTO }
                    }
                    _currentNavTab.value == BottomNavTab.OFFLINE -> {
                        val offPhotos = offlineMedia.value.filter { it.mediaType == MediaType.PHOTO }
                        if (offPhotos.any { it.id == item.id }) offPhotos else allMedia.value.filter { it.mediaType == MediaType.PHOTO }
                    }
                    else -> filteredMedia.value.filter { it.mediaType == MediaType.PHOTO }.ifEmpty {
                        allMedia.value.filter { it.mediaType == MediaType.PHOTO }
                    }
                }

                val sortedPhotos = when (currentSort) {
                    SortOrder.NAME_ASC -> basePhotos.sortedWith { a, b -> naturalCompare(a.title, b.title) }
                    SortOrder.NAME_DESC -> basePhotos.sortedWith { a, b -> naturalCompare(b.title, a.title) }
                    SortOrder.DATE_DESC -> basePhotos.sortedByDescending { it.dateAdded }
                    SortOrder.DATE_ASC -> basePhotos.sortedBy { it.dateAdded }
                }
                _activeViewer.value = ActiveViewer.Photo(item, sortedPhotos.ifEmpty { listOf(item) })
            }
            MediaType.VIDEO -> _activeViewer.value = ActiveViewer.Video(item)
            MediaType.PDF -> _activeViewer.value = ActiveViewer.Pdf(item)
        }
    }

    fun closeViewer() {
        _activeViewer.value = ActiveViewer.None
    }

    fun showDetail(item: MediaItem?) {
        _selectedDetailItem.value = item
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val newFav = repository.toggleFavorite(item)
            val updated = item.copy(isFavorite = newFav)
            when (val v = _activeViewer.value) {
                is ActiveViewer.Video -> if (v.item.id == item.id) _activeViewer.value = ActiveViewer.Video(updated)
                is ActiveViewer.Photo -> if (v.item.id == item.id) _activeViewer.value = ActiveViewer.Photo(updated, v.allPhotos.map { if (it.id == item.id) updated else it })
                is ActiveViewer.GooglePhotosAlbum -> if (v.item.id == item.id) _activeViewer.value = ActiveViewer.GooglePhotosAlbum(updated, v.photos)
                is ActiveViewer.Manga -> if (v.item.id == item.id) _activeViewer.value = ActiveViewer.Manga(updated)
                is ActiveViewer.Pdf -> if (v.item.id == item.id) _activeViewer.value = ActiveViewer.Pdf(updated)
                ActiveViewer.None -> {}
            }
            if (_selectedDetailItem.value?.id == item.id) {
                _selectedDetailItem.value = updated
            }
            configManager.autoSaveToFile()
        }
    }

    fun deleteMedia(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMedia(item.id)
            if (_selectedDetailItem.value?.id == item.id) {
                _selectedDetailItem.value = null
            }
            configManager.autoSaveToFile()
        }
    }

    fun updateReadingProgress(id: String, page: Int, totalPages: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(id, page, totalPages)
        }
    }

    fun updatePlaybackProgress(id: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.updatePlaybackProgress(id, positionMs, durationMs)
        }
    }

    fun onLocalUrisSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _mediaLoadingProgress.value = MediaLoadingProgress(
                isVisible = true,
                title = if (uris.size > 1) "Importing ${uris.size} Files" else "Loading Media File",
                subtitle = "Reading and indexing media...",
                currentFileName = "",
                processedCount = 0,
                totalCount = uris.size,
                isIndeterminate = false,
                isComplete = false
            )
            var lastItem: MediaItem? = null
            for ((index, uri) in uris.withIndex()) {
                val displayName = repository.getUriDisplayName(uri)
                _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                    currentFileName = displayName,
                    processedCount = index,
                    subtitle = "Importing file ${index + 1} of ${uris.size}..."
                )
                val item = repository.addLocalMediaUri(uri)
                lastItem = item
                _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                    processedCount = index + 1
                )
            }
            _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                subtitle = "Successfully imported ${uris.size} item(s)",
                isComplete = true
            )
            delay(600)
            _mediaLoadingProgress.value = MediaLoadingProgress(isVisible = false)
            if (uris.size == 1 && lastItem != null) {
                openMedia(lastItem)
            }
        }
    }

    fun addCloudMedia(
        title: String,
        url: String,
        type: MediaType,
        format: MediaFormat,
        thumbnailUrl: String? = null
    ) {
        viewModelScope.launch {
            val item = repository.addCloudMedia(title, url, type, format, thumbnailUrl)
            if (item.folderId != null && item.folderName != null) {
                val existing = userSettings.value.configuredFolders.find { it.id == item.folderId }
                if (existing == null) {
                    val folder = MediaFolder(
                        id = item.folderId,
                        name = item.folderName,
                        uriString = item.uriString,
                        fileCount = 1,
                        lastScannedMs = System.currentTimeMillis(),
                        coverUrl = item.thumbnailUrl
                    )
                    settingsRepository.addConfiguredFolder(folder)
                } else if (existing.coverUrl.isNullOrBlank() && !item.thumbnailUrl.isNullOrBlank()) {
                    settingsRepository.updateConfiguredFolder(existing.copy(coverUrl = item.thumbnailUrl))
                }
            }
            configManager.autoSaveToFile()
            configManager.refreshStats()
            _showAddUrlDialog.value = false
            openMedia(item)
        }
    }

    fun updateThumbnailUrl(item: MediaItem, newThumbnailUrl: String?) {
        viewModelScope.launch {
            repository.updateThumbnailUrl(item.id, newThumbnailUrl)
            if (item.folderId != null) {
                val existing = userSettings.value.configuredFolders.find { it.id == item.folderId }
                if (existing != null && newThumbnailUrl?.isNotBlank() == true) {
                    settingsRepository.updateConfiguredFolder(existing.copy(coverUrl = newThumbnailUrl))
                }
            }
            configManager.autoSaveToFile()
            configManager.refreshStats()
        }
    }

    fun refreshAlbumCover(item: MediaItem) {
        viewModelScope.launch {
            val cover = repository.refreshAlbumCover(item)
            if (cover != null && item.folderId != null) {
                val existing = userSettings.value.configuredFolders.find { it.id == item.folderId }
                if (existing != null) {
                    settingsRepository.updateConfiguredFolder(existing.copy(coverUrl = cover))
                }
            }
            configManager.autoSaveToFile()
            configManager.refreshStats()
        }
    }

    fun setShowConnectDialog(show: Boolean) {
        _showConnectDialog.value = show
    }

    fun setShowAddUrlDialog(show: Boolean) {
        _showAddUrlDialog.value = show
    }

    // --- Folder Setup & Selection ---
    fun onFolderSelected(treeUri: Uri) {
        viewModelScope.launch {
            // Persist URI permission immediately so Android will never revoke folder access
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            } catch (_: Exception) {
                try {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            val initialFolderName = repository.getUriDisplayName(treeUri).ifBlank { "Folder" }
            _mediaLoadingProgress.value = MediaLoadingProgress(
                isVisible = true,
                title = "Scanning \"$initialFolderName\"",
                subtitle = "Accessing directory...",
                currentFileName = "Preparing file scanner...",
                processedCount = 0,
                totalCount = 0,
                isIndeterminate = true,
                isComplete = false
            )
            val (folderName, scannedItems) = repository.scanFolderUri(treeUri) { status, fileName, count ->
                _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                    title = "Scanning \"$initialFolderName\"",
                    subtitle = status,
                    currentFileName = fileName,
                    processedCount = count,
                    isIndeterminate = true
                )
            }
            val folder = MediaFolder(
                id = "folder_${treeUri.hashCode()}",
                name = folderName,
                uriString = treeUri.toString(),
                fileCount = scannedItems.size,
                lastScannedMs = System.currentTimeMillis()
            )
            settingsRepository.addConfiguredFolder(folder)
            configManager.autoSaveToFile()
            _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                title = "Folder Scan Complete",
                subtitle = "Indexed ${scannedItems.size} media file(s) in \"$folderName\"",
                currentFileName = "",
                processedCount = scannedItems.size,
                isIndeterminate = false,
                isComplete = true
            )
            delay(800)
            _mediaLoadingProgress.value = MediaLoadingProgress(isVisible = false)
        }
    }

    fun addDemoFolder() {
        viewModelScope.launch {
            val count = allMedia.value.count { it.mediaSource == MediaSource.LOCAL || it.drawableResId != null }
            val demoFolder = MediaFolder(
                id = "folder_demo_vault",
                name = "Omni Local Media Vault",
                uriString = "content://local/omni_vault",
                fileCount = count,
                lastScannedMs = System.currentTimeMillis()
            )
            settingsRepository.addConfiguredFolder(demoFolder)
            configManager.autoSaveToFile()
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            val folder = userSettings.value.configuredFolders.find { it.id == folderId }
            val uriStr = folder?.uriString ?: ""
            val folderName = folder?.name
            repository.removeMediaFromFolder(folderId, uriStr, folderName)
            settingsRepository.removeConfiguredFolder(folderId)
            configManager.autoSaveToFile()
        }
    }

    fun rescanFolder(folder: MediaFolder) {
        viewModelScope.launch {
            _mediaLoadingProgress.value = MediaLoadingProgress(
                isVisible = true,
                title = "Rescanning \"${folder.name}\"",
                subtitle = "Updating folder contents...",
                currentFileName = "Scanning files...",
                processedCount = 0,
                totalCount = 0,
                isIndeterminate = true,
                isComplete = false
            )
            val (resolvedName, scannedItems) = repository.scanFolder(
                folderId = folder.id,
                folderName = folder.name,
                uriString = folder.uriString
            ) { status, fileName, count ->
                _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                    subtitle = status,
                    currentFileName = fileName,
                    processedCount = count
                )
            }
            settingsRepository.updateFolderStats(folder.id, scannedItems.size)
            configManager.autoSaveToFile()
            _mediaLoadingProgress.value = _mediaLoadingProgress.value.copy(
                title = "Folder Updated",
                subtitle = "Found ${scannedItems.size} item(s) in \"${folder.name}\"",
                isComplete = true,
                isIndeterminate = false
            )
            delay(700)
            _mediaLoadingProgress.value = MediaLoadingProgress(isVisible = false)
        }
    }

    // --- Reader & Playback Customization ---
    fun updateReadingDirection(direction: ReadingDirection) {
        settingsRepository.updateReadingDirection(direction)
        viewModelScope.launch { configManager.autoSaveToFile() }
    }

    fun updateVideoAspectMode(mode: VideoAspectMode) {
        settingsRepository.updateVideoAspectMode(mode)
        viewModelScope.launch { configManager.autoSaveToFile() }
    }

    fun updateAutoPlay(autoPlay: Boolean) {
        settingsRepository.updateAutoPlay(autoPlay)
        viewModelScope.launch { configManager.autoSaveToFile() }
    }

    // --- Configuration Export & Import ---
    fun clearConfigMessage() {
        configImportExportMessage.value = null
    }

    fun exportConfiguration(targetUri: Uri) {
        viewModelScope.launch {
            val result = configManager.exportToFileUri(targetUri)
            if (result.isSuccess) {
                val bytes = result.getOrThrow()
                configImportExportMessage.value = "Export successful ($bytes bytes saved to JSON file)"
            } else {
                configImportExportMessage.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun importConfiguration(sourceUri: Uri) {
        viewModelScope.launch {
            val result = configManager.importFromUri(sourceUri)
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                val mediaInfo = if (summary.mediaItemsCount > 0) ", ${summary.mediaItemsCount} media items" else ""
                configImportExportMessage.value = "Configuration imported! Restored ${summary.folderCount} folders$mediaInfo, ${summary.cloudStreamCount} cloud streams" + (if (summary.hasJellyfin) ", Jellyfin server" else "")
                val activeServer = database.jellyfinDao().getActiveServerDirect()
                _jellyfinState.value = _jellyfinState.value.copy(connectedServer = activeServer)
                if (activeServer != null) {
                    loadJellyfinLibraries(activeServer)
                }
                // Automatically detect and scan files from all restored folders
                val folders = userSettings.value.configuredFolders
                for (folder in folders) {
                    val (_, scanned) = repository.scanFolder(folder.id, folder.name, folder.uriString)
                    settingsRepository.updateFolderStats(folder.id, scanned.size)
                }
            } else {
                configImportExportMessage.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun importConfigurationFromString(jsonString: String) {
        viewModelScope.launch {
            val result = configManager.importFromJsonString(jsonString)
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                val mediaInfo = if (summary.mediaItemsCount > 0) ", ${summary.mediaItemsCount} media items" else ""
                configImportExportMessage.value = "Configuration imported! Restored ${summary.folderCount} folders$mediaInfo, ${summary.cloudStreamCount} cloud streams" + (if (summary.hasJellyfin) ", Jellyfin server" else "")
                val activeServer = database.jellyfinDao().getActiveServerDirect()
                _jellyfinState.value = _jellyfinState.value.copy(connectedServer = activeServer)
                if (activeServer != null) {
                    loadJellyfinLibraries(activeServer)
                }
                // Automatically detect and scan files from all restored folders
                val folders = userSettings.value.configuredFolders
                for (folder in folders) {
                    val (_, scanned) = repository.scanFolder(folder.id, folder.name, folder.uriString)
                    settingsRepository.updateFolderStats(folder.id, scanned.size)
                }
            } else {
                configImportExportMessage.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    suspend fun getConfigJsonString(): String = configManager.generateFullConfigJsonString()

    // --- Jellyfin actions ---
    fun connectToJellyfinServer(serverUrl: String, username: String, pass: String) {
        viewModelScope.launch {
            _jellyfinState.value = _jellyfinState.value.copy(isConnecting = true, errorMessage = null)
            val result = repository.authenticateJellyfin(serverUrl, username, pass)
            if (result.isSuccess) {
                val server = result.getOrThrow()
                settingsRepository.saveJellyfinCredentials(serverUrl, username, pass)
                configManager.autoSaveToFile()
                _jellyfinState.value = _jellyfinState.value.copy(
                    isConnecting = false,
                    connectedServer = server,
                    errorMessage = null
                )
                _showConnectDialog.value = false
                loadJellyfinLibraries(server)
            } else {
                _jellyfinState.value = _jellyfinState.value.copy(
                    isConnecting = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to connect to Jellyfin"
                )
            }
        }
    }

    private fun loadJellyfinLibraries(server: JellyfinServerEntity) {
        viewModelScope.launch {
            val libResult = repository.getJellyfinLibraries(server)
            if (libResult.isSuccess) {
                val libs = libResult.getOrThrow()
                _jellyfinState.value = _jellyfinState.value.copy(libraries = libs)
                if (libs.isNotEmpty() && _jellyfinState.value.selectedLibrary == null) {
                    selectJellyfinLibrary(libs.first())
                }
            }
        }
    }

    fun selectJellyfinLibrary(library: JellyfinLibraryItem) {
        val server = _jellyfinState.value.connectedServer ?: return
        _jellyfinState.value = _jellyfinState.value.copy(
            selectedLibrary = library,
            isLoadingItems = true
        )
        viewModelScope.launch {
            val itemsResult = repository.getJellyfinItems(server, library.id)
            if (itemsResult.isSuccess) {
                _jellyfinState.value = _jellyfinState.value.copy(
                    libraryItems = itemsResult.getOrThrow(),
                    isLoadingItems = false
                )
            } else {
                _jellyfinState.value = _jellyfinState.value.copy(
                    isLoadingItems = false,
                    errorMessage = itemsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun openJellyfinItem(item: JellyfinMediaItem) {
        val server = _jellyfinState.value.connectedServer ?: return
        val domainMedia = repository.convertJellyfinItemToMediaItem(item, server)
        openMedia(domainMedia)
    }

    fun getBookmarks(mediaId: String) = repository.getBookmarks(mediaId)

    fun addBookmark(mediaId: String, page: Int, title: String) {
        viewModelScope.launch {
            repository.addBookmark(mediaId, page, title)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }
}
