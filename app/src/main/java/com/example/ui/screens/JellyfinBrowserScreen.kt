package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.R
import com.example.data.jellyfin.JellyfinLibraryItem
import com.example.data.jellyfin.JellyfinMediaItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.viewmodel.JellyfinUiState

@Composable
fun JellyfinBrowserScreen(
    state: JellyfinUiState,
    onConnectClick: () -> Unit,
    onSelectLibrary: (JellyfinLibraryItem) -> Unit,
    onOpenItem: (JellyfinMediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("jellyfin_browser_screen")
    ) {
        val server = state.connectedServer

        if (server == null) {
            // Not connected state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = PrimaryViolet.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = PrimaryViolet,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Jellyfin Home Streaming",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect your personal home server to stream comics, photos, and high-definition MP4 videos directly on your device.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .testTag("jellyfin_open_connect_button")
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect to Server", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Connected state: show server banner, library tabs, and media grid
            Column(modifier = Modifier.fillMaxSize()) {
                // Server Info Status Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("jellyfin_server_header"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(PrimaryViolet.copy(alpha = 0.6f), AccentCyan.copy(alpha = 0.4f)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentCyan.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Online",
                                tint = AccentCyan,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = server.serverName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = AccentCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${server.serverUrl} • User: ${server.username.ifBlank { "guest" }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onConnectClick,
                            modifier = Modifier.testTag("jellyfin_switch_server_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Switch Server",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Library Selection Filter Row
                if (state.libraries.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.libraries) { lib ->
                            val isSelected = state.selectedLibrary?.id == lib.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectLibrary(lib) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (lib.collectionType.lowercase()) {
                                                "books" -> Icons.Default.MenuBook
                                                "photos" -> Icons.Default.Photo
                                                else -> Icons.Default.Movie
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(lib.name, fontSize = 13.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryViolet,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("jellyfin_lib_${lib.id}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Library Items Grid
                if (state.isLoadingItems) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryViolet)
                    }
                } else if (state.libraryItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items found in this library",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("jellyfin_items_grid")
                    ) {
                        items(state.libraryItems) { item ->
                            JellyfinItemCard(
                                item = item,
                                onClick = { onOpenItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JellyfinItemCard(
    item: JellyfinMediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("jf_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorder, Color.Transparent)))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (item.type.equals("Book", ignoreCase = true)) 0.72f else 1.33f)
                    .background(SurfaceElevated)
            ) {
                // Fallback / Demo asset mapping
                val fallbackRes = when (item.id) {
                    "jf_book_1" -> R.drawable.img_manga_cover
                    "jf_book_2" -> R.drawable.img_hero_media
                    "jf_photo_1" -> R.drawable.img_photo_scenic
                    "jf_photo_2" -> R.drawable.img_hero_media
                    "jf_movie_1" -> R.drawable.img_hero_media
                    "jf_movie_2" -> R.drawable.img_photo_scenic
                    else -> null
                }

                if (fallbackRes != null) {
                    Image(
                        painter = painterResource(id = fallbackRes),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.type.lowercase()) {
                                "book" -> Icons.Default.MenuBook
                                "photo" -> Icons.Default.Photo
                                else -> Icons.Default.Movie
                            },
                            contentDescription = null,
                            tint = PrimaryViolet,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Type badge
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.container?.uppercase() ?: item.type.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!item.type.equals("Book", true) && !item.type.equals("Photo", true)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Stream",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.overview ?: "Stream from Jellyfin",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
