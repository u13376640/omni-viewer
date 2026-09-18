package com.example.ui.components

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.example.data.model.MediaFormat
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("media_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorder, Color.Transparent)))
    ) {
        Column {
            // Media Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (item.mediaType == MediaType.MANGA || item.mediaType == MediaType.PDF) 0.72f else 1.33f)
                    .background(DeepObsidian)
            ) {
                // Image or Artwork (loads thumbnails for pictures & videos, local & cloud)
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
                    androidx.compose.foundation.Image(
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
                } else {
                    // Fallback placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryViolet.copy(alpha = 0.3f), DeepObsidian)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.mediaType) {
                                MediaType.MANGA -> Icons.Default.MenuBook
                                MediaType.PHOTO -> Icons.Default.Photo
                                MediaType.VIDEO -> Icons.Default.Movie
                                MediaType.PDF -> Icons.Default.PictureAsPdf
                            },
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Vignette gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Top Badges (Format tag and Favorite)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type & Format Badge
                    Surface(
                        color = when (item.mediaType) {
                            MediaType.MANGA -> PrimaryViolet.copy(alpha = 0.9f)
                            MediaType.PHOTO -> AccentCyan.copy(alpha = 0.9f)
                            MediaType.VIDEO -> Color(0xFFEF4444).copy(alpha = 0.9f)
                            MediaType.PDF -> Color(0xFFF59E0B).copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("badge_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.mediaFormat.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.mediaSource == MediaSource.JELLYFIN) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• JF",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .testTag("favorite_toggle_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Play Icon if Video
                if (item.mediaType == MediaType.VIDEO) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(28.dp)
                        )
                    }
                }

                // Bottom Overlay Bar (Reading progress or duration)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (item.mediaType == MediaType.MANGA || item.mediaType == MediaType.PDF) {
                        if (item.totalPages > 1) {
                            Text(
                                text = "Page ${item.lastPageIndex + 1}/${item.totalPages}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (item.mediaType == MediaType.VIDEO && item.durationMs > 0) {
                        val minutes = (item.durationMs / 1000) / 60
                        val seconds = (item.durationMs / 1000) % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (!item.fileSizeFormatted.isBlank()) {
                        Text(
                            text = item.fileSizeFormatted,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Progress Bar at very bottom edge
                if (item.mediaType == MediaType.MANGA && item.totalPages > 1 && item.lastPageIndex > 0) {
                    val progress = (item.lastPageIndex + 1).toFloat() / item.totalPages.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = PrimaryViolet,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                } else if (item.mediaType == MediaType.VIDEO && item.durationMs > 0 && item.lastPositionMs > 0) {
                    val progress = (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = AccentCyan,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            // Media Info Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle.ifBlank { item.mediaType.displayName },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("info_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
