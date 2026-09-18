package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.data.util.GooglePhotosHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.jellyfin.JellyfinLibraryItem
import com.example.data.jellyfin.JellyfinMediaItem
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.components.MediaCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.CloudOption
import com.example.ui.viewmodel.JellyfinUiState

@Composable
fun CloudScreen(
    cloudOption: CloudOption,
    onSelectCloudOption: (CloudOption) -> Unit,
    // Jellyfin handlers
    jellyfinState: JellyfinUiState,
    onConnectJellyfinClick: () -> Unit,
    onSelectJellyfinLibrary: (JellyfinLibraryItem) -> Unit,
    onOpenJellyfinItem: (JellyfinMediaItem) -> Unit,
    // Stream URL handlers
    cloudStreams: List<MediaItem>,
    onAddCloudMedia: (title: String, url: String, type: MediaType, format: MediaFormat, thumbnailUrl: String?) -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onInfoClick: (MediaItem) -> Unit,
    onFavoriteToggle: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("cloud_screen")
    ) {
        // Sticky Top Selector: Jellyfin vs Stream URL
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Segmented Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .padding(4.dp)
                ) {
                    // Jellyfin Option
                    val isJellyfin = cloudOption == CloudOption.JELLYFIN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isJellyfin) PrimaryViolet else Color.Transparent)
                            .clickable { onSelectCloudOption(CloudOption.JELLYFIN) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (isJellyfin) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Jellyfin Server",
                                fontWeight = if (isJellyfin) FontWeight.Bold else FontWeight.Medium,
                                color = if (isJellyfin) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Stream URL Option
                    val isStreamUrl = cloudOption == CloudOption.STREAM_URL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isStreamUrl) PrimaryViolet else Color.Transparent)
                            .clickable { onSelectCloudOption(CloudOption.STREAM_URL) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (isStreamUrl) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stream URL",
                                fontWeight = if (isStreamUrl) FontWeight.Bold else FontWeight.Medium,
                                color = if (isStreamUrl) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Body Content based on active Cloud Option
        if (cloudOption == CloudOption.JELLYFIN) {
            JellyfinBrowserScreen(
                state = jellyfinState,
                onConnectClick = onConnectJellyfinClick,
                onSelectLibrary = onSelectJellyfinLibrary,
                onOpenItem = onOpenJellyfinItem
            )
        } else {
            StreamUrlPortal(
                cloudStreams = cloudStreams,
                onAddCloudMedia = onAddCloudMedia,
                onOpenMedia = onOpenMedia,
                onInfoClick = onInfoClick,
                onFavoriteToggle = onFavoriteToggle
            )
        }
    }
}

@Composable
private fun StreamUrlPortal(
    cloudStreams: List<MediaItem>,
    onAddCloudMedia: (title: String, url: String, type: MediaType, format: MediaFormat, thumbnailUrl: String?) -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onInfoClick: (MediaItem) -> Unit,
    onFavoriteToggle: (MediaItem) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var thumbnailUrlInput by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(MediaFormat.MP4) }
    val coroutineScope = rememberCoroutineScope()
    var isResolvingAlbum by remember { mutableStateOf(false) }

    val selectedType = when (selectedFormat) {
        MediaFormat.MP4 -> MediaType.VIDEO
        MediaFormat.PDF -> MediaType.PDF
        MediaFormat.JPG, MediaFormat.PNG -> MediaType.PHOTO
        MediaFormat.UNKNOWN -> MediaType.VIDEO
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("stream_url_grid")
    ) {
        // Direct URL Input Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().testTag("card_stream_url_input")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Stream Direct Media URL",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Stream MP4, HLS (.m3u8), PDF, or images from any web URL",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { 
                            urlInput = it
                            val trimmed = it.trim()
                            val lower = trimmed.lowercase()
                            if (GooglePhotosHelper.isGooglePhotosUrl(trimmed)) {
                                selectedFormat = MediaFormat.JPG
                                coroutineScope.launch {
                                    isResolvingAlbum = true
                                    try {
                                        val info = GooglePhotosHelper.extractAlbumInfo(trimmed)
                                        info?.let { album ->
                                            if (titleInput.isBlank() && album.title.isNotBlank()) {
                                                titleInput = album.title
                                            }
                                            if (thumbnailUrlInput.isBlank() && album.coverUrl.isNotBlank()) {
                                                thumbnailUrlInput = album.coverUrl
                                            }
                                        }
                                    } catch (_: Exception) {
                                    } finally {
                                        isResolvingAlbum = false
                                    }
                                }
                            } else if (lower.contains("mycloudz") || lower.contains("/v/") || lower.contains("embed") || lower.contains("streamtape") || lower.contains("vidcloud") || lower.contains(".mp4") || lower.contains(".m3u8")) {
                                selectedFormat = MediaFormat.MP4
                            } else if (lower.contains(".pdf")) {
                                selectedFormat = MediaFormat.PDF
                            } else if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp")) {
                                selectedFormat = MediaFormat.JPG
                            } else {
                                selectedFormat = MediaFormat.MP4
                            }
                        },
                        placeholder = { Text("https://mycloudz.cc/v/... or photos.app.goo.gl/...", fontSize = 13.sp) },
                        label = { Text("Stream Media / Google Photos / Web URL") },
                        supportingText = {
                            if (isResolvingAlbum) {
                                Text(
                                    "Detecting Google Photos album cover & title...",
                                    color = AccentCyan,
                                    fontSize = 11.sp
                                )
                            } else if (GooglePhotosHelper.isGooglePhotosUrl(urlInput.trim())) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Google Photos album detected",
                                        color = Color(0xFF4285F4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Fetch Cover",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            coroutineScope.launch {
                                                isResolvingAlbum = true
                                                try {
                                                    val info = GooglePhotosHelper.extractAlbumInfo(urlInput.trim())
                                                    info?.let { album ->
                                                        if (album.title.isNotBlank()) titleInput = album.title
                                                        if (album.coverUrl.isNotBlank()) thumbnailUrlInput = album.coverUrl
                                                    }
                                                } catch (_: Exception) {
                                                } finally {
                                                    isResolvingAlbum = false
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    "Supports direct video (.mp4, .m3u8), Google Photos albums, documents (.pdf), and Web Streams",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = SurfaceElevated,
                            focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                            unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_stream_url")
                    )

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        placeholder = { Text("Optional title (e.g., Live Cyber Stream)", fontSize = 13.sp) },
                        label = { Text("Stream Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = SurfaceElevated,
                            focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                            unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_stream_title")
                    )

                    OutlinedTextField(
                        value = thumbnailUrlInput,
                        onValueChange = { thumbnailUrlInput = it },
                        placeholder = { Text("https://example.com/poster.jpg (webURL)", fontSize = 13.sp) },
                        label = { Text("Thumbnail Link (webURL - Optional)") },
                        supportingText = {
                            Text(
                                "Web image URL for card thumbnail. Essential for web view streams and saved to JSON config.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (thumbnailUrlInput.isNotBlank()) {
                                IconButton(onClick = { thumbnailUrlInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = SurfaceElevated,
                            focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                            unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_stream_thumbnail_url")
                    )

                    // Live Thumbnail Preview if URL is entered
                    if (thumbnailUrlInput.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated.copy(alpha = 0.6f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DeepObsidian),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = thumbnailUrlInput.trim(),
                                    contentDescription = "Thumbnail Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (GooglePhotosHelper.isGooglePhotosUrl(urlInput.trim())) "Google Photos Album Cover" else "Thumbnail Preview",
                                    color = if (GooglePhotosHelper.isGooglePhotosUrl(urlInput.trim())) Color(0xFF4285F4) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (GooglePhotosHelper.isGooglePhotosUrl(urlInput.trim())) "Saved as folder cover and backed up in configuration JSON" else "Will be saved in configuration JSON and displayed on cards",
                                    color = AccentCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    val defaultTitle = when {
                                        GooglePhotosHelper.isGooglePhotosUrl(urlInput) -> "Google Photos Album"
                                        selectedFormat == MediaFormat.MP4 -> "Web Video Stream"
                                        selectedFormat == MediaFormat.PDF -> "Online PDF"
                                        else -> "Online Photo"
                                    }
                                    val finalTitle = titleInput.ifBlank { defaultTitle }
                                    val cleanThumb = thumbnailUrlInput.trim().ifBlank { null }
                                    val streamType = when (selectedFormat) {
                                        MediaFormat.MP4 -> MediaType.VIDEO
                                        MediaFormat.PDF -> MediaType.PDF
                                        else -> MediaType.PHOTO
                                    }
                                    onAddCloudMedia(finalTitle, urlInput.trim(), streamType, selectedFormat, cleanThumb)
                                    urlInput = ""
                                    titleInput = ""
                                    thumbnailUrlInput = ""
                                }
                            },
                            enabled = urlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_play_stream_url")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Saved Cloud Streams Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Saved Cloud Streams (${cloudStreams.size})",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Saved Streams Grid
        if (cloudStreams.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No saved stream URLs yet",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Enter any stream URL above to stream",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(cloudStreams, key = { it.id }) { streamItem ->
                MediaCard(
                    item = streamItem,
                    onClick = { onOpenMedia(streamItem) },
                    onInfoClick = { onInfoClick(streamItem) },
                    onFavoriteToggle = { onFavoriteToggle(streamItem) }
                )
            }
        }
    }
}
