package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import coil.compose.AsyncImage
import com.example.data.model.MediaFolder
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.util.GooglePhotosHelper
import com.example.ui.components.AddUrlDialog
import com.example.ui.components.FolderCard
import com.example.ui.components.JellyfinConnectDialog
import com.example.ui.components.MediaCard
import com.example.ui.components.MediaDetailBottomSheet
import com.example.ui.components.MediaLoadingProgressDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.ActiveViewer
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.CloudOption
import com.example.ui.viewmodel.HomeCategory
import com.example.ui.viewmodel.OmniViewModel
import com.example.ui.viewmodel.SortOrder
import com.example.ui.viewmodel.naturalCompare

@Composable
fun HomeScreen(
    viewModel: OmniViewModel
) {
    val navTab by viewModel.currentNavTab.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val homeCategory by viewModel.homeCategory.collectAsState()
    val offlineCategory by viewModel.offlineCategory.collectAsState()
    val cloudOption by viewModel.cloudOption.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredMedia by viewModel.filteredMedia.collectAsState()
    val offlineMedia by viewModel.offlineMedia.collectAsState()
    val cloudStreams by viewModel.cloudStreams.collectAsState()
    val recentMedia by viewModel.recentMedia.collectAsState()
    val activeViewer by viewModel.activeViewer.collectAsState()
    val selectedDetailItem by viewModel.selectedDetailItem.collectAsState()
    val showConnectDialog by viewModel.showConnectDialog.collectAsState()
    val showAddUrlDialog by viewModel.showAddUrlDialog.collectAsState()
    val jellyfinState by viewModel.jellyfinState.collectAsState()
    val mediaLoadingProgress by viewModel.mediaLoadingProgress.collectAsState()
    val folderNavStack by viewModel.folderNavStack.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    // Intercept hardware/system back button to navigate up folders if open
    BackHandler(enabled = folderNavStack.isNotEmpty()) {
        viewModel.navigateUpFolder()
    }

    // Pop up window displaying progress of loading media/folder
    if (mediaLoadingProgress.isVisible) {
        MediaLoadingProgressDialog(
            progress = mediaLoadingProgress,
            onDismiss = { viewModel.dismissLoadingProgress() }
        )
    }

    // Activity Result Launchers for local files:
    // 1. Android Photo & Video Picker (zero-permission standard)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onLocalUrisSelected(uris)
        }
    }

    // 2. Android Storage Document Picker for PDF and arbitrary files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onLocalUrisSelected(uris)
        }
    }

    // 3. Folder Picker for Media Folder Setup
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFolderSelected(it) }
    }

    var showPickerMenu by remember { mutableStateOf(false) }

    // Check if a viewer is currently active
    when (val viewer = activeViewer) {
        is ActiveViewer.Manga -> {
            MangaReaderScreen(
                item = viewer.item,
                onClose = { viewModel.closeViewer() },
                onProgressUpdate = { page, total ->
                    viewModel.updateReadingProgress(viewer.item.id, page, total)
                },
                onFavoriteToggle = { viewModel.toggleFavorite(viewer.item) },
                onAddBookmark = { page, title ->
                    viewModel.addBookmark(viewer.item.id, page, title)
                },
                initialReadingDirection = userSettings.readingDirection
            )
            return
        }
        is ActiveViewer.Photo -> {
            PhotoViewerScreen(
                initialItem = viewer.item,
                allPhotos = viewer.allPhotos,
                initialReadingDirection = userSettings.readingDirection,
                activeSortOrder = sortOrder,
                onClose = { viewModel.closeViewer() },
                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                onShowInfo = { viewModel.showDetail(it) }
            )
            return
        }
        is ActiveViewer.GooglePhotosAlbum -> {
            GooglePhotosAlbumViewerScreen(
                item = viewer.item,
                albumPhotos = viewer.photos,
                onClose = { viewModel.closeViewer() },
                onOpenPhoto = { photo ->
                    viewModel.openMedia(photo, viewer.photos)
                },
                onUpdateCover = { newCover ->
                    viewModel.updateThumbnailUrl(viewer.item, newCover)
                }
            )
            return
        }
        is ActiveViewer.Video -> {
            VideoPlayerScreen(
                item = viewer.item,
                onClose = { viewModel.closeViewer() },
                onProgressUpdate = { pos, dur ->
                    viewModel.updatePlaybackProgress(viewer.item.id, pos, dur)
                },
                onFavoriteToggle = { viewModel.toggleFavorite(viewer.item) },
                defaultAspectMode = userSettings.videoAspectMode,
                autoPlay = userSettings.autoPlayVideos
            )
            return
        }
        is ActiveViewer.Pdf -> {
            PdfReaderScreen(
                item = viewer.item,
                onClose = { viewModel.closeViewer() },
                onProgressUpdate = { page, total ->
                    viewModel.updateReadingProgress(viewer.item.id, page, total)
                },
                onFavoriteToggle = { viewModel.toggleFavorite(viewer.item) },
                onAddBookmark = { page, title ->
                    viewModel.addBookmark(viewer.item.id, page, title)
                },
                initialReadingDirection = userSettings.readingDirection
            )
            return
        }
        ActiveViewer.None -> {
            // Display main app scaffold
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_scaffold"),
        containerColor = DeepObsidian,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                // Home Tab
                NavigationBarItem(
                    selected = navTab == BottomNavTab.HOME,
                    onClick = { viewModel.selectNavTab(BottomNavTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryViolet,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Offline Tab
                NavigationBarItem(
                    selected = navTab == BottomNavTab.OFFLINE,
                    onClick = { viewModel.selectNavTab(BottomNavTab.OFFLINE) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Offline") },
                    label = { Text("Offline") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryViolet,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_offline")
                )

                // Cloud Tab (Option between Jellyfin or Stream URL)
                NavigationBarItem(
                    selected = navTab == BottomNavTab.CLOUD,
                    onClick = { viewModel.selectNavTab(BottomNavTab.CLOUD) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Cloud") },
                    label = { Text("Cloud") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryViolet,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_cloud")
                )

                // Settings Tab
                NavigationBarItem(
                    selected = navTab == BottomNavTab.SETTINGS,
                    onClick = { viewModel.selectNavTab(BottomNavTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = PrimaryViolet,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (navTab) {
                BottomNavTab.HOME -> {
                    // Header Bar
                    Surface(
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF0D0B18),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.app_logo_earth),
                                            contentDescription = "OmniViewer App Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "OmniViewer",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = Color.White
                                        )
                                    )
                                }

                                // Quick Action Buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { showPickerMenu = true },
                                        modifier = Modifier.testTag("header_open_file_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Open Local Media",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search input
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search manga, photos, videos, or PDFs...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryViolet,
                                    unfocusedBorderColor = SurfaceElevated,
                                    focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                                    unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("home_search_field")
                            )

                            // Filter chips row
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    HomeCategory.ALL to "All Media",
                                    HomeCategory.COMICS to "Comics & Manga",
                                    HomeCategory.PHOTOS to "Photos",
                                    HomeCategory.VIDEOS to "Videos",
                                    HomeCategory.FAVORITES to "Favorites"
                                ).forEach { (cat, label) ->
                                    item {
                                        FilterChip(
                                            selected = homeCategory == cat,
                                            onClick = { viewModel.setHomeCategory(cat) },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryViolet,
                                                selectedLabelColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("filter_tab_${cat.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val isSearching = searchQuery.isNotBlank()
                    val currentFolderNode = folderNavStack.lastOrNull()

                    // Compute folders and files for current navigation depth
                    val (displayFolders, displayFiles) = remember(
                        filteredMedia,
                        userSettings.configuredFolders,
                        folderNavStack,
                        isSearching,
                        sortOrder
                    ) {
                        if (isSearching) {
                            // When searching, show all matched media directly
                            emptyList<Pair<String, List<MediaItem>>>() to filteredMedia
                        } else if (currentFolderNode == null) {
                            // Root of Home:
                            val configured = userSettings.configuredFolders
                            val configuredIds = configured.map { it.id }.toSet()
                            val configuredNames = configured.map { it.name.trim().lowercase() }.toSet()
                            val folderMap = mutableMapOf<String, MutableList<MediaItem>>()
                            val rootFiles = mutableListOf<MediaItem>()

                            for (item in filteredMedia) {
                                val fId = item.folderId
                                val fName = item.folderName
                                val isCloud = item.mediaSource == MediaSource.CLOUD_URL
                                if (fId != null) {
                                    // If configured folders are present, ensure item belongs to an active configured folder or is cloud media
                                    if (isCloud || configured.isEmpty() || configuredIds.contains(fId) || (fName != null && configuredNames.contains(fName.trim().lowercase()))) {
                                        folderMap.getOrPut(fId) { mutableListOf() }.add(item)
                                    }
                                } else if (!fName.isNullOrBlank()) {
                                    if (isCloud || configured.isEmpty() || configuredNames.contains(fName.trim().lowercase())) {
                                        folderMap.getOrPut(fName) { mutableListOf() }.add(item)
                                    }
                                } else {
                                    rootFiles.add(item)
                                }
                            }

                            // Include configured folders even if currently 0 items in category
                            for (cf in configured) {
                                if (!folderMap.containsKey(cf.id)) {
                                    folderMap[cf.id] = mutableListOf()
                                }
                            }

                            val folderList = folderMap.entries.map { entry ->
                                val folderKey = entry.key
                                val items = entry.value
                                val folderName = configured.find { it.id == folderKey }?.name
                                    ?: items.firstOrNull()?.folderName
                                    ?: folderKey
                                folderName to items
                            }.let { list ->
                                when (sortOrder) {
                                    SortOrder.NAME_ASC -> list.sortedWith { a, b -> naturalCompare(a.first, b.first) }
                                    SortOrder.NAME_DESC -> list.sortedWith { a, b -> naturalCompare(b.first, a.first) }
                                    else -> list
                                }
                            }

                            folderList to rootFiles
                        } else {
                            // Inside a specific folder / subfolder!
                            val targetFolderId = currentFolderNode.folderId
                            val targetFolderName = currentFolderNode.folderName
                            val currentPath = currentFolderNode.subfolderPath

                            val folderItems = filteredMedia.filter { item ->
                                (targetFolderId != null && item.folderId == targetFolderId) ||
                                item.folderName.equals(targetFolderName, ignoreCase = true)
                            }

                            val directFiles = folderItems.filter { (it.subfolderPath ?: "") == currentPath }

                            val subfolderMap = mutableMapOf<String, MutableList<MediaItem>>()
                            for (item in folderItems) {
                                val itemSub = item.subfolderPath.orEmpty()
                                if (currentPath.isEmpty()) {
                                    if (itemSub.isNotEmpty()) {
                                        val immediateSub = itemSub.substringBefore("/")
                                        subfolderMap.getOrPut(immediateSub) { mutableListOf() }.add(item)
                                    }
                                } else {
                                    if (itemSub.startsWith("$currentPath/")) {
                                        val relative = itemSub.removePrefix("$currentPath/")
                                        val immediateSub = relative.substringBefore("/")
                                        subfolderMap.getOrPut(immediateSub) { mutableListOf() }.add(item)
                                    }
                                }
                            }

                            val subfolderList = subfolderMap.entries.map { entry ->
                                entry.key to entry.value
                            }.let { list ->
                                when (sortOrder) {
                                    SortOrder.NAME_ASC -> list.sortedWith { a, b -> naturalCompare(a.first, b.first) }
                                    SortOrder.NAME_DESC -> list.sortedWith { a, b -> naturalCompare(b.first, a.first) }
                                    else -> list
                                }
                            }

                            subfolderList to directFiles
                        }
                    }

                    // Home Main Content Area
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("media_grid")
                    ) {
                        // Continue Reading / Watching Carousel
                        if (currentFolderNode == null && homeCategory == HomeCategory.ALL && !isSearching && recentMedia.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(
                                        text = "Continue Reading & Watching",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentMedia, key = { it.id }) { recentItem ->
                                            RecentMediaItemCard(
                                                item = recentItem,
                                                onClick = { viewModel.openMedia(recentItem, recentMedia) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Navigation Breadcrumbs & Sort Row
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (currentFolderNode != null) {
                                    // Breadcrumb row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = PrimaryViolet.copy(alpha = 0.25f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .clickable { viewModel.navigateUpFolder() }
                                                .size(32.dp)
                                                .testTag("folder_nav_back")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Home",
                                            color = AccentCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { viewModel.navigateToHomeRoot() }
                                        )
                                        folderNavStack.forEachIndexed { idx, node ->
                                            Text(
                                                text = " / ",
                                                color = Color(0xFF64748B),
                                                fontSize = 13.sp
                                            )
                                            val label = if (node.subfolderPath.isNotEmpty()) {
                                                node.subfolderPath.substringAfterLast("/")
                                            } else {
                                                node.folderName
                                            }
                                            Text(
                                                text = label,
                                                color = if (idx == folderNavStack.lastIndex) Color.White else AccentCyan,
                                                fontWeight = if (idx == folderNavStack.lastIndex) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.clickable { viewModel.navigateToFolderCrumb(idx) }
                                            )
                                        }
                                    }
                                } else {
                                    val sectionTitle = if (isSearching) {
                                        "Search Results (${filteredMedia.size})"
                                    } else {
                                        when (homeCategory) {
                                            HomeCategory.ALL -> "Collections (${filteredMedia.size})"
                                            HomeCategory.COMICS -> "Comics & Manga (${filteredMedia.size})"
                                            HomeCategory.PHOTOS -> "Photos (${filteredMedia.size})"
                                            HomeCategory.VIDEOS -> "Videos (${filteredMedia.size})"
                                            HomeCategory.FAVORITES -> "Favorites (${filteredMedia.size})"
                                        }
                                    }
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    )
                                }

                                // Sort Selector Dropdown Button
                                Box {
                                    Surface(
                                        color = SurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { showSortMenu = true }
                                            .testTag("sort_order_selector")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = "Sort order",
                                                tint = AccentCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sortOrder.label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(SurfaceDark)
                                    ) {
                                        SortOrder.values().forEach { order ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = order.label,
                                                        color = if (order == sortOrder) AccentCyan else Color.White,
                                                        fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                onClick = {
                                                    showSortMenu = false
                                                    viewModel.setSortOrder(order)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Folders Section
                        if (displayFolders.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = if (currentFolderNode == null) "Folders (${displayFolders.size})" else "Subfolders (${displayFolders.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(displayFolders, key = { "folder_${it.first}" }) { (folderName, itemsInFolder) ->
                                val matchingConfigured = userSettings.configuredFolders.find {
                                    it.name == folderName || (currentFolderNode != null && it.id == currentFolderNode.folderId)
                                }
                                val albumPrimaryItem = itemsInFolder.find { 
                                    GooglePhotosHelper.isGooglePhotosUrl(it.uriString) || it.subtitle == "Google Photos Album"
                                }
                                val previewMedia = itemsInFolder.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }
                                    ?: itemsInFolder.firstOrNull { it.drawableResId != null }
                                    ?: albumPrimaryItem
                                    ?: itemsInFolder.firstOrNull()
                                val resolvedFolderCover = matchingConfigured?.coverUrl?.takeIf { it.isNotBlank() }
                                    ?: previewMedia?.thumbnailUrl?.takeIf { it.isNotBlank() }
                                    ?: albumPrimaryItem?.thumbnailUrl?.takeIf { it.isNotBlank() }

                                FolderCard(
                                    name = folderName,
                                    itemCount = itemsInFolder.size,
                                    previewItem = previewMedia,
                                    coverUrl = resolvedFolderCover,
                                    onClick = {
                                        if (currentFolderNode == null) {
                                            val fId = itemsInFolder.firstOrNull()?.folderId ?: matchingConfigured?.id
                                            viewModel.enterFolder(fId, folderName, "")
                                        } else {
                                            val nextPath = if (currentFolderNode.subfolderPath.isEmpty()) {
                                                folderName
                                            } else {
                                                "${currentFolderNode.subfolderPath}/$folderName"
                                            }
                                            viewModel.enterFolder(currentFolderNode.folderId, currentFolderNode.folderName, nextPath)
                                        }
                                    },
                                    onRescan = {
                                        if (albumPrimaryItem != null) {
                                            viewModel.refreshAlbumCover(albumPrimaryItem)
                                        } else if (currentFolderNode == null && matchingConfigured != null) {
                                            viewModel.rescanFolder(matchingConfigured)
                                        }
                                    },
                                    onDelete = if (currentFolderNode == null && matchingConfigured != null) {
                                        { viewModel.removeFolder(matchingConfigured.id) }
                                    } else null
                                )
                            }
                        }

                        // Direct Media Files Section
                        if (displayFiles.isNotEmpty()) {
                            if (displayFolders.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "Files (${displayFiles.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }

                            items(displayFiles, key = { it.id }) { item ->
                                MediaCard(
                                    item = item,
                                    onClick = { viewModel.openMedia(item, displayFiles) },
                                    onInfoClick = { viewModel.showDetail(item) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(item) }
                                )
                            }
                        }

                        // Empty State
                        if (displayFolders.isEmpty() && displayFiles.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSearching) "No media matched \"$searchQuery\"" else "No media items found in this section",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                BottomNavTab.OFFLINE -> {
                    OfflineScreen(
                        mediaList = offlineMedia,
                        configuredFolders = userSettings.configuredFolders,
                        selectedCategory = offlineCategory,
                        onSelectCategory = { viewModel.setOfflineCategory(it) },
                        onOpenMedia = { viewModel.openMedia(it, offlineMedia) },
                        onInfoClick = { viewModel.showDetail(it) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onPickFilesClick = { showPickerMenu = true },
                        onSelectFolderClick = { folderPickerLauncher.launch(null) },
                        onRescanFolder = { viewModel.rescanFolder(it) },
                        onNavigateToSettings = { viewModel.selectNavTab(BottomNavTab.SETTINGS) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) }
                    )
                }

                BottomNavTab.CLOUD -> {
                    CloudScreen(
                        cloudOption = cloudOption,
                        onSelectCloudOption = { viewModel.selectCloudOption(it) },
                        jellyfinState = jellyfinState,
                        onConnectJellyfinClick = { viewModel.setShowConnectDialog(true) },
                        onSelectJellyfinLibrary = { viewModel.selectJellyfinLibrary(it) },
                        onOpenJellyfinItem = { viewModel.openJellyfinItem(it) },
                        cloudStreams = cloudStreams,
                        onAddCloudMedia = { title, url, type, format, thumbnailUrl ->
                            viewModel.addCloudMedia(title, url, type, format, thumbnailUrl)
                        },
                        onOpenMedia = { viewModel.openMedia(it) },
                        onInfoClick = { viewModel.showDetail(it) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                    )
                }

                BottomNavTab.SETTINGS -> {
                    val configFileStats by viewModel.configFileStats.collectAsStateWithLifecycle()
                    val configMessage by viewModel.configImportExportMessage.collectAsStateWithLifecycle()
                    SettingsScreen(
                        settings = userSettings,
                        configFileStats = configFileStats,
                        onUpdateReadingDirection = { viewModel.updateReadingDirection(it) },
                        onUpdateVideoAspectMode = { viewModel.updateVideoAspectMode(it) },
                        onUpdateAutoPlay = { viewModel.updateAutoPlay(it) },
                        onSelectFolder = { viewModel.onFolderSelected(it) },
                        onRemoveFolder = { viewModel.removeFolder(it) },
                        onRescanFolder = { viewModel.rescanFolder(it) },
                        onExportConfig = { viewModel.exportConfiguration(it) },
                        onImportConfig = { viewModel.importConfiguration(it) },
                        onImportConfigFromString = { viewModel.importConfigurationFromString(it) },
                        onGetConfigJsonString = { viewModel.getConfigJsonString() },
                        configMessage = configMessage,
                        onDismissConfigMessage = { viewModel.clearConfigMessage() }
                    )
                }
            }
        }
    }

    // Local Picker Selector Dialog (Photos/Videos or PDF/Documents)
    if (showPickerMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPickerMenu = false },
            containerColor = SurfaceDark,
            title = { Text("Choose Local Media Type", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showPickerMenu = false
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pick_photos_videos_button")
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Photos & Videos (JPG, PNG, MP4)")
                    }

                    OutlinedButton(
                        onClick = {
                            showPickerMenu = false
                            documentPickerLauncher.launch(
                                arrayOf("application/pdf", "image/*", "video/*")
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        modifier = Modifier.fillMaxWidth().testTag("pick_pdf_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Comics & Media Files")
                    }

                    OutlinedButton(
                        onClick = {
                            showPickerMenu = false
                            folderPickerLauncher.launch(null)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryViolet),
                        modifier = Modifier.fillMaxWidth().testTag("pick_folder_button")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Setup / Select Media Folder")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPickerMenu = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Bottom Sheet Detail Dialog
    selectedDetailItem?.let { detailItem ->
        MediaDetailBottomSheet(
            item = detailItem,
            onDismiss = { viewModel.showDetail(null) },
            onOpen = {
                viewModel.showDetail(null)
                viewModel.openMedia(detailItem)
            },
            onFavoriteToggle = { viewModel.toggleFavorite(detailItem) },
            onDelete = { viewModel.deleteMedia(detailItem) }
        )
    }

    // Jellyfin Server Connection Dialog
    if (showConnectDialog) {
        JellyfinConnectDialog(
            onDismiss = { viewModel.setShowConnectDialog(false) },
            onConnect = { url, user, pass ->
                viewModel.connectToJellyfinServer(url, user, pass)
            },
            isConnecting = jellyfinState.isConnecting,
            errorMessage = jellyfinState.errorMessage
        )
    }

    // Add Cloud URL Dialog
    if (showAddUrlDialog) {
        AddUrlDialog(
            onDismiss = { viewModel.setShowAddUrlDialog(false) },
            onAdd = { title, url, type, format, thumbnailUrl ->
                viewModel.addCloudMedia(title, url, type, format, thumbnailUrl)
            }
        )
    }
}

@Composable
private fun RecentMediaItemCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("recent_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(SurfaceElevated)
            ) {
                val mediaImageModel: Any? = when {
                    item.thumbnailUrl?.isNotBlank() == true -> item.thumbnailUrl
                    item.uriString.isNotBlank() -> {
                        if (item.uriString.startsWith("content://") || item.uriString.startsWith("file://")) {
                            android.net.Uri.parse(item.uriString)
                        } else {
                            item.uriString
                        }
                    }
                    else -> null
                }

                if (item.drawableResId != null) {
                    Image(
                        painter = painterResource(id = item.drawableResId),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (mediaImageModel != null) {
                    AsyncImage(
                        model = mediaImageModel,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(6.dp).align(Alignment.BottomStart)
                ) {
                    Text(
                        text = if (item.mediaType == MediaType.MANGA || item.mediaType == MediaType.PDF) {
                            "Page ${item.lastPageIndex + 1}/${item.totalPages}"
                        } else {
                            val mins = (item.lastPositionMs / 1000) / 60
                            val secs = (item.lastPositionMs / 1000) % 60
                            String.format("%02d:%02d", mins, secs)
                        },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
