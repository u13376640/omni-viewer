package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.ReadingDirection
import com.example.data.pdf.PdfEngine
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import kotlinx.coroutines.launch

@Composable
fun PdfReaderScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onProgressUpdate: (page: Int, totalPages: Int) -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddBookmark: (page: Int, title: String) -> Unit,
    initialReadingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT
) {
    val context = LocalContext.current
    val pdfEngine = remember { PdfEngine(context) }
    var pageCount by remember { mutableIntStateOf(item.totalPages.coerceAtLeast(1)) }
    val pageBitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }
    var isLoadingPages by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isBookmarked by remember { mutableStateOf(false) }
    var readingDirection by remember(initialReadingDirection) { mutableStateOf(initialReadingDirection) }

    val coroutineScope = rememberCoroutineScope()

    // Determine initial page count
    LaunchedEffect(item.uriString) {
        val count = pdfEngine.getPageCount(item.uriString)
        if (count > 0) {
            pageCount = count
        }
        isLoadingPages = false
    }

    val initialIndex = item.lastPageIndex.coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { pageCount }
    )
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Preload current, previous and next page
    LaunchedEffect(pagerState.currentPage, listState.firstVisibleItemIndex, readingDirection, pageCount) {
        val cur = if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) pagerState.currentPage else listState.firstVisibleItemIndex
        onProgressUpdate(cur, pageCount)

        listOf(cur, cur - 1, cur + 1).filter { it in 0 until pageCount }.forEach { page ->
            if (!pageBitmapCache.containsKey(page)) {
                coroutineScope.launch {
                    val bmp = pdfEngine.renderPage(item.uriString, page)
                    if (bmp != null) {
                        pageBitmapCache[page] = bmp
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("pdf_reader_screen")
    ) {
        if (isLoadingPages) {
            CircularProgressIndicator(
                color = PrimaryViolet,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) {
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
                    .testTag("pdf_horizontal_pager")
            ) { pageIndex ->
                val bitmap = pageBitmapCache[pageIndex]
                if (bitmap != null) {
                    ZoomablePdfPage(bitmap = bitmap)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryViolet,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
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
                    .testTag("pdf_vertical_list")
            ) {
                items(pageCount) { pageIndex ->
                    val bitmap = pageBitmapCache[pageIndex]
                    if (bitmap != null) {
                        ZoomablePdfPage(bitmap = bitmap)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryViolet,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Controls Overlay
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
                        modifier = Modifier.testTag("pdf_back_button")
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
                        val currentPageNum = if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) pagerState.currentPage + 1 else listState.firstVisibleItemIndex + 1
                        Text(
                            text = "Page $currentPageNum of $pageCount • ${readingDirection.shortLabel}",
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
                        modifier = Modifier.testTag("pdf_toggle_view_mode_button")
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

                    // Bookmark Button
                    IconButton(
                        onClick = {
                            val curr = if (readingDirection != ReadingDirection.VERTICAL_CONTINUOUS) pagerState.currentPage else listState.firstVisibleItemIndex
                            isBookmarked = !isBookmarked
                            onAddBookmark(curr, "Page ${curr + 1}")
                        },
                        modifier = Modifier.testTag("pdf_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) PrimaryViolet else Color.White
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.testTag("pdf_fav_button")
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

        // Bottom Page Slider Overlay
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )

                    Slider(
                        value = pagerState.currentPage.toFloat(),
                        onValueChange = { targetPage ->
                            coroutineScope.launch {
                                pagerState.scrollToPage(targetPage.toInt())
                            }
                        },
                        valueRange = 0f..(pageCount - 1).toFloat().coerceAtLeast(0f),
                        steps = (pageCount - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryViolet,
                            activeTrackColor = PrimaryViolet,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pdf_page_slider")
                    )

                    Text(
                        text = "$pageCount",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePdfPage(bitmap: Bitmap) {
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
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF Page",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
