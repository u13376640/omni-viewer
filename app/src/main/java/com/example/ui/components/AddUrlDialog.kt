package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaFormat
import com.example.data.model.MediaType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated

@Composable
fun AddUrlDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, url: String, type: MediaType, format: MediaFormat, thumbnailUrl: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(MediaFormat.MP4) }
    val coroutineScope = rememberCoroutineScope()
    var isResolvingAlbum by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_url_dialog"),
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryViolet.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add Cloud Media Stream",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter direct HTTP/HTTPS stream link for MP4 video, PDF document, or JPG/PNG image:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        val trimmed = it.trim()
                        val lower = trimmed.lowercase()
                        if (GooglePhotosHelper.isGooglePhotosUrl(trimmed)) {
                            selectedFormat = MediaFormat.JPG
                            coroutineScope.launch {
                                isResolvingAlbum = true
                                try {
                                    val info = GooglePhotosHelper.extractAlbumInfo(trimmed)
                                    info?.let { album ->
                                        if (title.isBlank() && album.title.isNotBlank()) {
                                            title = album.title
                                        }
                                        if (thumbnailUrl.isBlank() && album.coverUrl.isNotBlank()) {
                                            thumbnailUrl = album.coverUrl
                                        }
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    isResolvingAlbum = false
                                }
                            }
                        } else {
                            when {
                                lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("mycloudz") || lower.contains("/v/") || lower.contains("embed") || lower.contains("streamtape") || lower.contains("vidcloud") -> selectedFormat = MediaFormat.MP4
                                lower.contains(".pdf") -> selectedFormat = MediaFormat.PDF
                                lower.contains(".png") -> selectedFormat = MediaFormat.PNG
                                lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".webp") -> selectedFormat = MediaFormat.JPG
                                else -> selectedFormat = MediaFormat.MP4
                            }
                        }
                    },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/video.mp4 or Google Photos album") },
                    supportingText = {
                        if (isResolvingAlbum) {
                            Text(
                                "Detecting Google Photos album cover & title...",
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        } else if (GooglePhotosHelper.isGooglePhotosUrl(url.trim())) {
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
                                                val info = GooglePhotosHelper.extractAlbumInfo(url.trim())
                                                info?.let { album ->
                                                    if (album.title.isNotBlank()) title = album.title
                                                    if (album.coverUrl.isNotBlank()) thumbnailUrl = album.coverUrl
                                                }
                                            } catch (_: Exception) {
                                            } finally {
                                                isResolvingAlbum = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = PrimaryViolet)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = SurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("My Stream") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = SurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text("Thumbnail Link (webURL - Optional)") },
                    placeholder = { Text("https://example.com/poster.jpg") },
                    supportingText = {
                        Text(
                            "Provide a web image URL for preview (saved inside configuration JSON)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (thumbnailUrl.isNotBlank()) {
                            IconButton(onClick = { thumbnailUrl = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = SurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_thumbnail_url_input")
                )

                // Thumbnail Preview
                if (thumbnailUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated.copy(alpha = 0.6f))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepObsidian),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = thumbnailUrl.trim(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = if (GooglePhotosHelper.isGooglePhotosUrl(url.trim())) "Google Photos Album Cover attached" else "Thumbnail preview attached",
                            color = if (GooglePhotosHelper.isGooglePhotosUrl(url.trim())) Color(0xFF4285F4) else AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mediaType = when (selectedFormat) {
                        MediaFormat.MP4 -> MediaType.VIDEO
                        MediaFormat.PDF -> MediaType.PDF
                        MediaFormat.JPG, MediaFormat.PNG -> MediaType.PHOTO
                        MediaFormat.UNKNOWN -> MediaType.PHOTO
                    }
                    val defaultTitle = when {
                        GooglePhotosHelper.isGooglePhotosUrl(url) -> "Google Photos Album"
                        selectedFormat == MediaFormat.MP4 -> "Web Video Stream"
                        selectedFormat == MediaFormat.PDF -> "Online PDF"
                        else -> "Online Photo"
                    }
                    val cleanThumb = thumbnailUrl.trim().ifBlank { null }
                    onAdd(title.ifBlank { defaultTitle }, url.trim(), mediaType, selectedFormat, cleanThumb)
                },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_add_url_button")
            ) {
                Text("Open & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_url_button")
            ) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
