package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MediaItem
import com.example.data.model.MediaSource
import com.example.data.model.VideoAspectMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.PrimaryViolet
import kotlinx.coroutines.delay

private fun isWebStreamUrl(url: String): Boolean {
    val u = url.trim().lowercase()
    if (!u.startsWith("http://") && !u.startsWith("https://")) return false
    val rawMediaExtensions = listOf(".mp4", ".m3u8", ".mpd", ".mkv", ".webm", ".avi", ".mov", ".flv", ".ts", ".m4v", ".3gp")
    if (rawMediaExtensions.any { u.endsWith(it) || u.contains("$it?") }) return false
    val webKeywords = listOf(
        "mycloudz", "mycloud", "streamtape", "vidcloud", "rapidcloud",
        "youtube", "youtu.be", "vimeo", "dailymotion", "mega.nz",
        "/v/", "/e/", "/embed/", "/watch", "html"
    )
    return webKeywords.any { u.contains(it) } || !rawMediaExtensions.any { u.contains(it) }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    item: MediaItem,
    onClose: () -> Unit,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onFavoriteToggle: () -> Unit,
    defaultAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current

    // Immediate local favorite state for instant responsive UI & haptic feedback
    var isFavorite by remember(item.id, item.isFavorite) { mutableStateOf(item.isFavorite) }
    var favoriteToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(favoriteToastMessage) {
        if (favoriteToastMessage != null) {
            delay(2000)
            favoriteToastMessage = null
        }
    }

    // Engine Selection: Auto-detect Web Player (e.g. for mycloudz.cc, embeds) vs Native ExoPlayer
    val initialWebMode = remember(item.uriString) { isWebStreamUrl(item.uriString) }
    var isWebStreamMode by remember(item.uriString) { mutableStateOf(initialWebMode) }

    var isFullscreenCustomView by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var exitCustomViewAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Intercept back button to exit HTML5 fullscreen or close player
    BackHandler {
        if (isFullscreenCustomView) {
            exitCustomViewAction?.invoke()
        } else {
            onClose()
        }
    }

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var currentPosition by remember { mutableLongStateOf(item.lastPositionMs) }
    var duration by remember { mutableLongStateOf(item.durationMs.coerceAtLeast(1L)) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val initialAspectIndex = when (defaultAspectMode) {
        VideoAspectMode.FIT -> 0
        VideoAspectMode.FILL_CROP -> 1
        VideoAspectMode.STRETCH -> 2
    }
    var resizeModeIndex by remember(defaultAspectMode) { mutableStateOf(initialAspectIndex) }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
    )

    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    // Build ExoPlayer with robust HTTP/HLS streaming support (used when not in Web Stream Mode)
    val exoPlayer = remember(retryKey, isWebStreamMode) {
        if (isWebStreamMode) null else {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36 OmniViewer/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(25000)

            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    val mediaUri = Uri.parse(item.uriString)
                    val exoItemBuilder = ExoMediaItem.Builder().setUri(mediaUri)
                    val urlLower = item.uriString.lowercase()
                    if (urlLower.contains(".m3u8")) {
                        exoItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                    } else if (urlLower.contains(".mpd")) {
                        exoItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                    }
                    setMediaItem(exoItemBuilder.build())
                    prepare()
                    if (item.lastPositionMs > 0) {
                        seekTo(item.lastPositionMs)
                    }
                    playWhenReady = autoPlay
                }
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        playbackError = null
                        duration = exoPlayer.duration.coerceAtLeast(1L)
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    val cause = error.cause?.message ?: error.message ?: "Failed to load stream"
                    playbackError = "ExoPlayer stream error: $cause"
                    // If it's a web page, notify user they can switch to Web Stream mode
                    if (item.uriString.startsWith("http")) {
                        playbackError = "Direct stream reader could not decode stream format.\nUse the In-App Web Video Player below to play this web stream."
                    }
                }
            }
            exoPlayer.addListener(listener)

            onDispose {
                val finalPos = exoPlayer.currentPosition
                val finalDur = exoPlayer.duration.coerceAtLeast(1L)
                onProgressUpdate(finalPos, finalDur)
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    // Periodic progress ticker for ExoPlayer
    LaunchedEffect(exoPlayer, isPlaying) {
        if (exoPlayer != null) {
            while (true) {
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    bufferedPosition = exoPlayer.bufferedPosition
                    if (exoPlayer.duration > 0) {
                        duration = exoPlayer.duration
                    }
                }
                delay(500)
            }
        }
    }

    // Auto-hide controls timer (both for ExoPlayer and Web Stream mode)
    LaunchedEffect(showControls, isPlaying, isWebStreamMode) {
        if (showControls) {
            if (isWebStreamMode) {
                // In Web Stream mode, automatically hide header after 3.5 seconds
                delay(3500)
                showControls = false
            } else if (isPlaying) {
                // In native ExoPlayer, auto-hide controls while video is playing
                delay(4000)
                showControls = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("video_player_screen")
    ) {
        if (isWebStreamMode) {
            // --- In-App Web Stream Player (for URLs like mycloudz.cc, embed players, etc.) ---
            WebStreamVideoPlayer(
                url = item.uriString,
                onFullscreenChanged = { isFullscreen, exitAction ->
                    isFullscreenCustomView = isFullscreen
                    exitCustomViewAction = exitAction
                },
                onWebViewCreated = { webView ->
                    webViewRef = webView
                }
            )
        } else if (exoPlayer != null) {
            // --- Native ExoPlayer Surface ---
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = resizeModes[resizeModeIndex].first
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeModes[resizeModeIndex].first
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    }
                    .testTag("exoplayer_surface")
            )

            // Buffering Indicator
            if (isBuffering && playbackError == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        color = PrimaryViolet,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (item.mediaSource == MediaSource.CLOUD_URL || item.mediaSource == MediaSource.JELLYFIN) "Buffering cloud stream..." else "Loading video...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            // ExoPlayer Stream Error Notification Card
            if (playbackError != null) {
                val err = playbackError!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepObsidian.copy(alpha = 0.95f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Playback Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Stream Format Notice",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch to Web Player Button (Fixes mycloudz.cc & web embed issues)
                        Button(
                            onClick = {
                                playbackError = null
                                isWebStreamMode = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play in Web Stream Player", color = DeepObsidian, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onClose() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go Back")
                            }
                            Button(
                                onClick = {
                                    playbackError = null
                                    isBuffering = true
                                    retryKey++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // --- Center Big Play / Skip Controls (For ExoPlayer mode) ---
        if (!isWebStreamMode && exoPlayer != null) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("video_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(PrimaryViolet.copy(alpha = 0.9f), CircleShape)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("video_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // --- Top Header Overlay (Accessible in both Native and Web Stream modes) ---
        AnimatedVisibility(
            visible = showControls && !isFullscreenCustomView,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer?.let {
                                onProgressUpdate(it.currentPosition, it.duration.coerceAtLeast(1L))
                            }
                            onClose()
                        },
                        modifier = Modifier.testTag("video_back_button")
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
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = if (isWebStreamMode) "In-App Web Player • Cloud Video" else when (item.mediaSource) {
                                MediaSource.JELLYFIN -> "Jellyfin Home Server Stream"
                                MediaSource.CLOUD_URL -> "Cloud Stream (Direct)"
                                MediaSource.LOCAL -> "Local Video"
                                MediaSource.DEMO -> "High Definition Showcase"
                            },
                            color = AccentCyan,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Engine Mode Switcher (Web Stream vs ExoPlayer)
                    if (item.uriString.startsWith("http")) {
                        // Web player action buttons: Reload and Open in External Browser
                        if (isWebStreamMode) {
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.testTag("video_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Stream",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.uriString))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                modifier = Modifier.testTag("video_open_browser_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in External Browser",
                                    tint = Color.White
                                )
                            }
                        }

                        Surface(
                            color = if (isWebStreamMode) AccentCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable {
                                    isWebStreamMode = !isWebStreamMode
                                    playbackError = null
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("video_engine_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWebStreamMode) Icons.Default.Language else Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = if (isWebStreamMode) AccentCyan else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isWebStreamMode) "Web Stream" else "Native",
                                    color = if (isWebStreamMode) AccentCyan else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ExoPlayer Specific Controls (Resize Aspect, Speed, Mute)
                    if (!isWebStreamMode && exoPlayer != null) {
                        IconButton(
                            onClick = {
                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                            },
                            modifier = Modifier.testTag("video_aspect_button")
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = resizeModes[resizeModeIndex].second,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    else -> 1.0f
                                }
                                exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                            },
                            modifier = Modifier.testTag("video_speed_button")
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier.testTag("video_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White
                            )
                        }
                    }

                    // LIKE / FAVORITE BUTTON - Highly visible, with immediate state feedback and sync to Favorites tag
                    Surface(
                        color = if (isFavorite) Color(0xFFFF4081).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                isFavorite = !isFavorite
                                onFavoriteToggle()
                                favoriteToastMessage = if (isFavorite) "Added to Favorites" else "Removed from Favorites"
                            }
                            .testTag("video_fav_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "In Favorites" else "Add to Favorites",
                                tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Hide Controls button
                    IconButton(
                        onClick = { showControls = false },
                        modifier = Modifier.testTag("video_hide_controls_btn")
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide Controls",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating pill to reveal controls when hidden (especially handy in Web Stream mode)
        AnimatedVisibility(
            visible = !showControls && !isFullscreenCustomView,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .clickable { showControls = true }
                    .testTag("video_show_controls_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Show Controls",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Controls",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- Bottom Progress & Seekbar Overlay (For ExoPlayer mode) ---
        if (!isWebStreamMode && exoPlayer != null) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var sliderPosition by remember { mutableStateOf<Float?>(null) }

                        Slider(
                            value = sliderPosition ?: currentPosition.toFloat(),
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    exoPlayer.seekTo(it.toLong())
                                    currentPosition = it.toLong()
                                    sliderPosition = null
                                }
                            },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryViolet,
                                activeTrackColor = PrimaryViolet,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_seek_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(sliderPosition?.toLong() ?: currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Quick Like Status pill in bottom bar
                            Surface(
                                color = if (isFavorite) Color(0xFFFF4081).copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable {
                                        isFavorite = !isFavorite
                                        onFavoriteToggle()
                                        favoriteToastMessage = if (isFavorite) "Added to Favorites" else "Removed from Favorites"
                                    }
                                    .padding(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFavorite) "Favorited" else "Favorite",
                                        color = if (isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Text(
                                text = formatTime(duration),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- Animated Floating Favorite Confirmation Badge ---
        AnimatedVisibility(
            visible = favoriteToastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 }),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp)
        ) {
            Surface(
                color = DeepObsidian.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.2f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = favoriteToastMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Robust In-App Web Video Stream Player.
 * - Hardware-accelerated with HTML5 Fullscreen support via WebChromeClient.onShowCustomView
 * - Blocks popup ad redirects and prevents intent:// / market:// deep-link crashes
 * - Suppresses background crashes with onRenderProcessGone
 * - Injects auto-play and anti-overlay scripts for seamless web streaming
 */
@Composable
private fun WebStreamVideoPlayer(
    url: String,
    onFullscreenChanged: (isFullscreen: Boolean, exitAction: (() -> Unit)?) -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentWebView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            currentWebView?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.destroy()
                } catch (e: Exception) {
                    // Safe cleanup
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("web_stream_player_container")
    ) {
        AndroidView(
            factory = { ctx ->
                val rootLayout = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                }

                val fullscreenContainer = FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    visibility = View.GONE
                }

                var customView: View? = null
                var customCallback: WebChromeClient.CustomViewCallback? = null

                fun hideCustom() {
                    customView?.let { cv ->
                        fullscreenContainer.removeView(cv)
                        fullscreenContainer.visibility = View.GONE
                        customCallback?.onCustomViewHidden()
                        customView = null
                        customCallback = null
                        onFullscreenChanged(false, null)
                    }
                }

                val webView = WebView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        allowContentAccess = false
                        allowFileAccess = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 70) {
                                isLoading = false
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customCallback = callback
                            fullscreenContainer.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            fullscreenContainer.visibility = View.VISIBLE
                            visibility = View.GONE
                            onFullscreenChanged(true) {
                                hideCustom()
                                visibility = View.VISIBLE
                            }
                        }

                        override fun onHideCustomView() {
                            hideCustom()
                            visibility = View.VISIBLE
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val reqUrl = request?.url ?: return false
                            val scheme = reqUrl.scheme?.lowercase() ?: ""

                            // CRITICAL CRASH PROTECTION: Block all non-http/https schemes (intent:, market:, etc.)
                            // Video hosts attempt to open ad schemes or app store deep links which cause ActivityNotFoundException crashes!
                            if (scheme != "http" && scheme != "https") {
                                return true
                            }

                            val reqHost = reqUrl.host?.lowercase() ?: ""
                            val origHost = try { Uri.parse(url).host?.lowercase() ?: "" } catch (e: Exception) { "" }

                            // If main frame navigates, block typical ad and tracking domains
                            if (request.isForMainFrame) {
                                val isSameDomain = origHost.isNotEmpty() && (
                                    reqHost == origHost ||
                                    reqHost.endsWith(".$origHost") ||
                                    origHost.endsWith(".$reqHost") ||
                                    reqHost.contains("mycloud") ||
                                    reqHost.contains("stream")
                                )

                                val isAdDomain = reqHost.contains("ad") ||
                                    reqHost.contains("track") ||
                                    reqHost.contains("pop") ||
                                    reqHost.contains("click") ||
                                    reqHost.contains("banner") ||
                                    reqHost.contains("bet") ||
                                    reqHost.contains("casino") ||
                                    reqHost.contains("traffic") ||
                                    reqHost.contains("redir") ||
                                    reqHost.contains("doubleclick")

                                if (isAdDomain || (!isSameDomain && reqUrl.path?.contains("/v/") != true && reqUrl.path?.contains("/embed") != true)) {
                                    // Block malicious ad redirection so stream doesn't stop or navigate away
                                    return true
                                }
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            isLoading = false
                            // Inject anti-ad-trap & auto-play helper script
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        window.open = function() { return null; };
                                        function removeTraps() {
                                            var els = document.querySelectorAll('div, a, span');
                                            for (var i = 0; i < els.length; i++) {
                                                var el = els[i];
                                                var s = window.getComputedStyle(el);
                                                if (s.position === 'fixed' || s.position === 'absolute') {
                                                    var z = parseInt(s.zIndex, 10);
                                                    if (z > 50 && !el.querySelector('video') && el.tagName !== 'VIDEO') {
                                                        if (el.offsetWidth > window.innerWidth * 0.6 && el.offsetHeight > window.innerHeight * 0.6) {
                                                            el.style.display = 'none';
                                                            el.style.pointerEvents = 'none';
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        removeTraps();
                                        setTimeout(removeTraps, 800);
                                        setTimeout(removeTraps, 2000);

                                        var v = document.querySelector('video');
                                        if (v) {
                                            v.style.width = '100%';
                                            v.style.height = '100%';
                                            v.play().catch(function(e){});
                                        }
                                    } catch(e) {}
                                })();
                                """.trimIndent(),
                                null
                            )
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            // CRITICAL: Return true so the host Android app process is NOT killed
                            return true
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                            }
                        }
                    }

                    loadUrl(url)
                }

                currentWebView = webView
                onWebViewCreated(webView)

                rootLayout.addView(webView)
                rootLayout.addView(fullscreenContainer)
                rootLayout
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepObsidian.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading Cloud Web Player...",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = url,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
