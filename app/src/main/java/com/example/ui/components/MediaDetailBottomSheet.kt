package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailBottomSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        modifier = Modifier.testTag("media_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (item.mediaType) {
                        MediaType.MANGA -> PrimaryViolet.copy(alpha = 0.2f)
                        MediaType.PHOTO -> AccentCyan.copy(alpha = 0.2f)
                        MediaType.VIDEO -> Color(0x33EF4444)
                        MediaType.PDF -> Color(0x33F59E0B)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.mediaFormat.name,
                            fontWeight = FontWeight.Bold,
                            color = when (item.mediaType) {
                                MediaType.MANGA -> PrimaryViolet
                                MediaType.PHOTO -> AccentCyan
                                MediaType.VIDEO -> Color(0xFFEF4444)
                                MediaType.PDF -> Color(0xFFF59E0B)
                            },
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = item.subtitle.ifBlank { "${item.mediaType.displayName} • ${item.mediaSource.name}" },
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("detail_fav_button")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFF4081) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = SurfaceElevated)
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata rows
            Text(
                text = "Media Properties",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            MetadataItemRow(label = "Type", value = item.mediaType.displayName)
            MetadataItemRow(label = "Format", value = item.mediaFormat.name)
            MetadataItemRow(label = "Source", value = item.mediaSource.name)

            if (item.mediaType == MediaType.MANGA || item.mediaType == MediaType.PDF) {
                MetadataItemRow(label = "Pages", value = "${item.lastPageIndex + 1} of ${item.totalPages}")
            } else if (item.mediaType == MediaType.VIDEO && item.durationMs > 0) {
                val mins = (item.durationMs / 1000) / 60
                val secs = (item.durationMs / 1000) % 60
                MetadataItemRow(label = "Duration", value = String.format("%02d:%02d", mins, secs))
            }

            if (item.fileSizeFormatted.isNotBlank()) {
                MetadataItemRow(label = "File Size", value = item.fileSizeFormatted)
            }

            if (!item.thumbnailUrl.isNullOrBlank()) {
                MetadataItemRow(label = "Thumbnail URL", value = item.thumbnailUrl)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onOpen()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("detail_open_media_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (item.mediaType) {
                            MediaType.MANGA, MediaType.PDF -> "Read Now"
                            MediaType.PHOTO -> "View Photo"
                            MediaType.VIDEO -> "Play Video"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("detail_delete_media_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MetadataItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
