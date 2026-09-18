package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated

enum class GoogleAlbumViewMode {
    WEB_ALBUM,
    PHOTO_GRID
}

/**
 * Dedicated, full-featured online Google Photos album viewer.
 * Provides interactive in-app live web album browsing, responsive native photo grid,
 * deep linking to the native Google Photos app, and instant reload/share controls.
 */
@Composable
fun GooglePhotosAlbumViewerScreen(
    item: MediaItem,
    albumPhotos: List<MediaItem> = emptyList(),
    onClose: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit = {},
    onUpdateCover: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var viewMode by remember {
        mutableStateOf(if (albumPhotos.isNotEmpty()) GoogleAlbumViewMode.PHOTO_GRID else GoogleAlbumViewMode.WEB_ALBUM)
    }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(true) }
    var webProgress by remember { mutableFloatStateOf(0.1f) }
    var webHasError by remember { mutableStateOf(false) }

    // Android hardware/system back button handling
    BackHandler {
        if (viewMode == GoogleAlbumViewMode.WEB_ALBUM && webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(item.uriString) {
        onDispose {
            webViewInstance?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.destroy()
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .statusBarsPadding()
            .testTag("google_photos_album_viewer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern High-tech Header Bar
            GooglePhotosAlbumHeader(
                title = item.title.ifBlank { "Google Photos Album" },
                photoCount = albumPhotos.size,
                viewMode = viewMode,
                hasPhotosExtracted = albumPhotos.isNotEmpty(),
                onBack = onClose,
                onToggleViewMode = {
                    viewMode = if (viewMode == GoogleAlbumViewMode.WEB_ALBUM) {
                        GoogleAlbumViewMode.PHOTO_GRID
                    } else {
                        GoogleAlbumViewMode.WEB_ALBUM
                    }
                },
                onRefresh = {
                    webHasError = false
                    isLoadingWeb = true
                    webViewInstance?.reload()
                },
                onOpenExternal = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.uriString)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open Google Photos Album"))
                    } catch (_: Exception) {}
                },
                onShare = {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            putExtra(Intent.EXTRA_TEXT, "${item.title}: ${item.uriString}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Album Link"))
                    } catch (_: Exception) {}
                }
            )

            // Web Loading Progress Indicator
            if (viewMode == GoogleAlbumViewMode.WEB_ALBUM && isLoadingWeb) {
                LinearProgressIndicator(
                    progress = { webProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentCyan,
                    trackColor = PrimaryViolet.copy(alpha = 0.2f)
                )
            }

            // Main Content Area (Web Album vs Native Photo Grid)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (viewMode) {
                    GoogleAlbumViewMode.WEB_ALBUM -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                isLoadingWeb = true
                                                webHasError = false
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                isLoadingWeb = false
                                                webProgress = 1f
                                                // Extract live album cover if available in loaded DOM
                                                view?.evaluateJavascript(
                                                    """
                                                    (function() {
                                                        try {
                                                            var meta = document.querySelector('meta[property="og:image"], meta[content*="googleusercontent.com"], meta[name="twitter:image"]');
                                                            if (meta && meta.content && meta.content.indexOf('googleusercontent.com') !== -1) {
                                                                return meta.content;
                                                            }
                                                            var imgs = document.querySelectorAll('img[src*="googleusercontent.com"]');
                                                            for (var i = 0; i < imgs.length; i++) {
                                                                var s = imgs[i].src;
                                                                if (s && (s.indexOf('=w') !== -1 || s.indexOf('/pw/') !== -1 || imgs[i].naturalWidth > 150)) {
                                                                    return s;
                                                                }
                                                            }
                                                        } catch(e) {}
                                                        return '';
                                                    })()
                                                    """.trimIndent()
                                                ) { result ->
                                                    if (!result.isNullOrBlank() && result != "\"\"" && result != "null") {
                                                        val clean = result.trim().removeSurrounding("\"").replace("\\u003d", "=").replace("\\/", "/")
                                                        if (clean.startsWith("http")) {
                                                            onUpdateCover?.invoke(clean)
                                                        }
                                                    }
                                                }
                                            }

                                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                if (request?.isForMainFrame == true) {
                                                    webHasError = true
                                                    isLoadingWeb = false
                                                }
                                            }

                                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                                return true
                                            }

                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                val reqUrl = request?.url?.toString() ?: return false
                                                // Keep google photos and web navigation in-app
                                                if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                                    return false
                                                }
                                                return try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                                                    ctx.startActivity(intent)
                                                    true
                                                } catch (_: Exception) {
                                                    true
                                                }
                                            }
                                        }

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                webProgress = (newProgress / 100f).coerceIn(0.1f, 1f)
                                                if (newProgress >= 70) {
                                                    isLoadingWeb = false
                                                }
                                            }
                                        }

                                        loadUrl(item.uriString)
                                        webViewInstance = this
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("google_photos_webview")
                            )

                            // Error Overlay if network fails
                            if (webHasError) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(DeepObsidian),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "Unable to Load Google Photos Album",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = "Please check your internet connection or open in browser.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = {
                                                    webHasError = false
                                                    isLoadingWeb = true
                                                    webViewInstance?.reload()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Retry")
                                            }
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.uriString))
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open in App")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GoogleAlbumViewMode.PHOTO_GRID -> {
                        GooglePhotosNativeGrid(
                            albumItem = item,
                            photos = albumPhotos,
                            onOpenPhoto = onOpenPhoto,
                            onOpenWeb = { viewMode = GoogleAlbumViewMode.WEB_ALBUM }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GooglePhotosAlbumHeader(
    title: String,
    photoCount: Int,
    viewMode: GoogleAlbumViewMode,
    hasPhotosExtracted: Boolean,
    onBack: () -> Unit,
    onToggleViewMode: () -> Unit,
    onRefresh: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = GlassSurface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .testTag("google_album_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("google_album_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Google Photos Identity Emblem
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (photoCount > 0) "Google Photos • $photoCount Photos" else "Google Photos • Shared Album",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Mode switcher button (if photos extracted)
            if (hasPhotosExtracted) {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onToggleViewMode)
                        .testTag("google_album_view_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (viewMode == GoogleAlbumViewMode.WEB_ALBUM) Icons.Default.GridOn else Icons.Default.Language,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (viewMode == GoogleAlbumViewMode.WEB_ALBUM) "Grid" else "Web",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Refresh Web View
            if (viewMode == GoogleAlbumViewMode.WEB_ALBUM) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("google_album_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Share Album Link
            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("google_album_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Album",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Open in external Google Photos App / Browser
            IconButton(
                onClick = onOpenExternal,
                modifier = Modifier.testTag("google_album_open_external")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Open in Google Photos App",
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * High-speed native photo grid for the Google Photos album items.
 */
@Composable
private fun GooglePhotosNativeGrid(
    albumItem: MediaItem,
    photos: List<MediaItem>,
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenWeb: () -> Unit
) {
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Live Web Album",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Switch to Web view to browse this Google Photos album interactively.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Button(
                    onClick = onOpenWeb,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Live Web Album")
                }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("google_album_photos_grid")
    ) {
        itemsIndexed(photos, key = { index, photo -> "${photo.id}_$index" }) { index, photo ->
            val photoUrl = photo.thumbnailUrl?.takeIf { it.isNotBlank() } ?: photo.uriString
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable { onOpenPhoto(photo) }
                    .testTag("google_album_photo_$index")
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Index Tag Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
