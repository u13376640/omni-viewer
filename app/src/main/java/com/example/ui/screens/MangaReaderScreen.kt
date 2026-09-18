package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaItem
import com.example.data.model.ReadingDirection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import kotlinx.coroutines.launch

@Composable
fun MangaReaderScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onProgressUpdate: (page: Int, totalPages: Int) -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddBookmark: (page: Int, title: String) -> Unit,
    initialReadingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT
) {
    // Pages list: either provided item pages, URLs, or local media (no fake demo placeholders)
    val pagesList: List<Any> = remember(item) {
        if (item.pages.isNotEmpty()) {
            item.pages
        } else if (item.pagesDrawableResIds.isNotEmpty()) {
            item.pagesDrawableResIds
        } else if (item.uriString.isNotBlank()) {
            listOf(item.uriString)
        } else if (item.thumbnailUrl != null) {
            listOf(item.thumbnailUrl)
        } else if (item.drawableResId != null) {
            listOf(item.drawableResId)
        } else {
            emptyList()
        }
    }

    val totalPages = pagesList.size.coerceAtLeast(1)
    val coroutineScope = rememberCoroutineScope()

    var readingDirection by remember(initialReadingDirection) {
        mutableStateOf(initialReadingDirection)
    }
    var showControls by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }

    val initialPage = item.lastPageIndex.coerceIn(0, totalPages - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    // Sync reading progress
    LaunchedEffect(pagerState.currentPage, listState.firstVisibleItemIndex, readingDirection) {
        val cur = if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) pagerState.currentPage else listState.firstVisibleItemIndex
        onProgressUpdate(cur, totalPages)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("manga_reader_screen")
    ) {
        // Main Viewer Content
        if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) {
            HorizontalPager(
                state = pagerState,
                reverseLayout = false,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    }
                    .testTag("manga_horizontal_pager")
            ) { pageIndex ->
                ZoomableMangaPage(
                    pageData = pagesList.getOrNull(pageIndex),
                    contentDescription = "Manga Page ${pageIndex + 1}"
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    }
                    .testTag("manga_vertical_webtoon")
            ) {
                itemsIndexed(pagesList) { index, pageData ->
                    ZoomableMangaPage(
                        pageData = pageData,
                        contentDescription = "Page ${index + 1}",
                        fillHeight = false
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Top Control Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = GlassSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("manga_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        val currentPage = if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) {
                            pagerState.currentPage + 1
                        } else {
                            listState.firstVisibleItemIndex + 1
                        }
                        Text(
                            text = "Page $currentPage of $totalPages • ${readingDirection.shortLabel}",
                            color = AccentCyan,
                            fontSize = 12.sp
                        )
                    }

                    // Unified Reading Flow Toggle (LTR <-> Vertical Webtoon)
                    IconButton(
                        onClick = {
                            readingDirection = when (readingDirection) {
                                ReadingDirection.LEFT_TO_RIGHT -> ReadingDirection.VERTICAL_CONTINUOUS
                                ReadingDirection.VERTICAL_CONTINUOUS -> ReadingDirection.LEFT_TO_RIGHT
                            }
                        },
                        modifier = Modifier.testTag("reading_direction_toggle")
                    ) {
                        Icon(
                            imageVector = when (readingDirection) {
                                ReadingDirection.LEFT_TO_RIGHT -> Icons.Default.SwapHoriz
                                ReadingDirection.VERTICAL_CONTINUOUS -> Icons.Default.ViewStream
                            },
                            contentDescription = "Reading Flow: ${readingDirection.label}",
                            tint = Color.White
                        )
                    }

                    // Bookmark
                    IconButton(
                        onClick = {
                            val curr = pagerState.currentPage
                            isBookmarked = !isBookmarked
                            onAddBookmark(curr, "Page ${curr + 1}")
                        },
                        modifier = Modifier.testTag("manga_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) PrimaryViolet else Color.White
                        )
                    }

                    // Favorite
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.testTag("manga_fav_button")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFFF4081) else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Page Slider & Thumbnail Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = GlassSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val currentPage = pagerState.currentPage

                    // Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentPage + 1}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetPage.toInt())
                                }
                            },
                            valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(0f),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryViolet,
                                activeTrackColor = PrimaryViolet,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manga_page_slider")
                        )

                        Text(
                            text = "$totalPages",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(32.dp)
                        )
                    }

                    // Page Thumbnails Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        itemsIndexed(pagesList) { idx, pageData ->
                            val isSelected = idx == currentPage
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) PrimaryViolet else Color(0xFF1E1B2C),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) {
                                                pagerState.animateScrollToPage(idx)
                                            } else {
                                                listState.animateScrollToItem(idx)
                                            }
                                        }
                                    }
                                    .testTag("manga_thumb_$idx")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (pageData is Int) {
                                        Image(
                                            painter = painterResource(id = pageData),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (pageData is String) {
                                        AsyncImage(
                                            model = pageData,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        text = "${idx + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableMangaPage(
    pageData: Any?,
    contentDescription: String,
    fillHeight: Boolean = true
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.2f
                        offset = Offset.Zero
                    }
                )
            }
            .transformable(
                state = transformState,
                enabled = true
            )
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentAlignment = Alignment.Center
    ) {
        if (pageData is Int) {
            Image(
                painter = painterResource(id = pageData),
                contentDescription = contentDescription,
                contentScale = if (fillHeight) ContentScale.Fit else ContentScale.FillWidth,
                modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
            )
        } else if (pageData is String) {
            AsyncImage(
                model = pageData,
                contentDescription = contentDescription,
                contentScale = if (fillHeight) ContentScale.Fit else ContentScale.FillWidth,
                modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
            )
        }
    }
}
