package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.model.MediaItem
import com.example.data.util.GooglePhotosHelper
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.PrimaryViolet

private val GPhotoBlue = Color(0xFF4285F4)
private val GPhotoRed = Color(0xFFEA4335)
private val GPhotoYellow = Color(0xFFFBBC05)
private val GPhotoGreen = Color(0xFF34A853)

@Composable
fun FolderCard(
    name: String,
    itemCount: Int,
    subfolderCount: Int = 0,
    previewItem: MediaItem? = null,
    coverUrl: String? = null,
    onClick: () -> Unit,
    onRescan: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val resolvedCover = coverUrl?.takeIf { it.isNotBlank() }
        ?: previewItem?.thumbnailUrl?.takeIf { it.isNotBlank() }

    val isGooglePhotos = (previewItem != null && GooglePhotosHelper.isGooglePhotosUrl(previewItem.uriString)) ||
            (previewItem != null && previewItem.subtitle == "Google Photos Album") ||
            name.contains("Google Photos", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("folder_card_${name.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, if (isGooglePhotos) GPhotoBlue.copy(alpha = 0.5f) else CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Folder Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.33f)
                    .background(DeepObsidian)
            ) {
                if (previewItem?.drawableResId != null) {
                    Image(
                        painter = painterResource(id = previewItem.drawableResId),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!resolvedCover.isNullOrBlank()) {
                    // Display the Google Photos Album cover or custom folder thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(resolvedCover)
                            .crossfade(true)
                            .videoFrameMillis(1500)
                            .build(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay to keep text legible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                } else if (previewItem != null && !previewItem.uriString.startsWith("http") && previewItem.uriString.isNotBlank()) {
                    // Local media file / video
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(previewItem.uriString)
                            .crossfade(true)
                            .videoFrameMillis(1500)
                            .build(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )
                } else if (isGooglePhotos) {
                    // Modern stylized Google Photos Album Art when cover is still fetching
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        GPhotoBlue.copy(alpha = 0.25f),
                                        GPhotoRed.copy(alpha = 0.25f),
                                        GPhotoYellow.copy(alpha = 0.25f),
                                        GPhotoGreen.copy(alpha = 0.25f),
                                        GPhotoBlue.copy(alpha = 0.25f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Google Photos Album",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "Google Photos Album",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Standard Folder Art Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryViolet.copy(alpha = 0.35f),
                                        DeepObsidian
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = PrimaryViolet.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Folder Badge & Icon at Top-Left
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isGooglePhotos) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GPhotoBlue))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GPhotoRed))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GPhotoYellow))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GPhotoGreen))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GOOGLE ALBUM",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FOLDER",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Count Badge at Bottom-Right
                Surface(
                    color = if (isGooglePhotos) GPhotoBlue.copy(alpha = 0.85f) else PrimaryViolet.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (subfolderCount > 0) "$itemCount items • $subfolderCount folders" else "$itemCount items",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Folder Label & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isGooglePhotos) "Google Photos Album Folder" else "Tap to open folder",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (isGooglePhotos) AccentCyan else Color(0xFF94A3B8)
                        )
                    )
                }

                if (onRescan != null || onDelete != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Folder options",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            if (onRescan != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isGooglePhotos) "Refresh album cover" else "Rescan folder",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = if (isGooglePhotos) GPhotoBlue else AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onRescan()
                                    }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove folder", color = Color(0xFFFF5252), fontSize = 13.sp) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open folder",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
