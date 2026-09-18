package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.ConfigFileStats
import com.example.data.model.MediaFolder
import com.example.data.model.ReadingDirection
import com.example.data.model.UserSettings
import com.example.data.model.VideoAspectMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: UserSettings,
    configFileStats: ConfigFileStats,
    onUpdateReadingDirection: (ReadingDirection) -> Unit,
    onUpdateVideoAspectMode: (VideoAspectMode) -> Unit,
    onUpdateAutoPlay: (Boolean) -> Unit,
    onSelectFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescanFolder: (MediaFolder) -> Unit,
    onExportConfig: (Uri) -> Unit,
    onImportConfig: (Uri) -> Unit,
    onImportConfigFromString: (String) -> Unit,
    onGetConfigJsonString: suspend () -> String,
    configMessage: String? = null,
    onDismissConfigMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showJsonPreviewDialog by remember { mutableStateOf(false) }
    var jsonPreviewContent by remember { mutableStateOf("") }

    var showPasteImportDialog by remember { mutableStateOf(false) }
    var pasteJsonText by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { onSelectFolder(it) }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onExportConfig(it) }
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportConfig(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryViolet.copy(alpha = 0.35f), AccentCyan.copy(alpha = 0.2f))
                        )
                    )
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Preferences & Setup",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Customize reading flow, media folders & display",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }

        // Section 1: Unified Reading Flow & Layout
        item {
            SettingsSectionHeader(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Reading Flow & Display"
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Unified Reading Flow (PDF or PNG/Comics)
                    OrientationOptionSelector(
                        title = "Reading Direction & Flow (PDF & Comics)",
                        description = "Progression mode for all PDF documents, manga, comics, and photo sets",
                        options = listOf(
                            OrientationChoice(
                                label = "Left to Right (Horizontal)",
                                icon = Icons.Default.SwapHoriz,
                                isSelected = settings.readingDirection == ReadingDirection.LEFT_TO_RIGHT,
                                onClick = { onUpdateReadingDirection(ReadingDirection.LEFT_TO_RIGHT) }
                            ),
                            OrientationChoice(
                                label = "Vertical Continuous (Webtoon)",
                                icon = Icons.Default.ViewStream,
                                isSelected = settings.readingDirection == ReadingDirection.VERTICAL_CONTINUOUS,
                                onClick = { onUpdateReadingDirection(ReadingDirection.VERTICAL_CONTINUOUS) }
                            )
                        )
                    )

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                    // Video Aspect Mode
                    OrientationOptionSelector(
                        title = "Video Player Scaling",
                        description = "Default screen fitting for video playback",
                        options = listOf(
                            OrientationChoice(
                                label = "Fit Screen",
                                icon = Icons.Default.Movie,
                                isSelected = settings.videoAspectMode == VideoAspectMode.FIT,
                                onClick = { onUpdateVideoAspectMode(VideoAspectMode.FIT) }
                            ),
                            OrientationChoice(
                                label = "Crop / Fill",
                                icon = Icons.Default.Movie,
                                isSelected = settings.videoAspectMode == VideoAspectMode.FILL_CROP,
                                onClick = { onUpdateVideoAspectMode(VideoAspectMode.FILL_CROP) }
                            ),
                            OrientationChoice(
                                label = "Stretch",
                                icon = Icons.Default.Movie,
                                isSelected = settings.videoAspectMode == VideoAspectMode.STRETCH,
                                onClick = { onUpdateVideoAspectMode(VideoAspectMode.STRETCH) }
                            )
                        )
                    )

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                    // Video Auto Play
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Play Videos",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Immediately start video playback when opened",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = settings.autoPlayVideos,
                            onCheckedChange = onUpdateAutoPlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryViolet,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = DeepObsidian
                            ),
                            modifier = Modifier.testTag("switch_auto_play")
                        )
                    }
                }
            }
        }

        // Section 2: Media Folders Setup
        item {
            SettingsSectionHeader(
                icon = Icons.Default.FolderOpen,
                title = "Setup Media Folders"
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Configured Folders",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Select folders on your device storage containing JPG, PNG comics, photos, PDF documents, or MP4 videos. OmniViewer will index them for offline viewing.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Action Button
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_select_media_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Media Folder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // List of Configured Folders
        if (settings.configuredFolders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No folders added yet",
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap 'Select Folder' above to link your local media collection",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(settings.configuredFolders, key = { it.id }) { folder ->
                ConfiguredFolderCard(
                    folder = folder,
                    onRescan = { onRescanFolder(folder) },
                    onRemove = { onRemoveFolder(folder.id) }
                )
            }
        }

        // Status Banner (if export or import result)
        if (configMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (configMessage.contains("failed", ignoreCase = true) || configMessage.contains("error", ignoreCase = true)) {
                            Color(0xFF7F1D1D)
                        } else {
                            Color(0xFF064E3B)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (configMessage.contains("failed", ignoreCase = true)) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = configMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissConfigMessage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Configuration & Backup (JSON)
        item {
            SettingsSectionHeader(
                icon = Icons.Default.DataObject,
                title = "Configuration & Backup (JSON)"
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_json_configuration")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Synced JSON File",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "omniviewer_config.json",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = configFileStats.sizeFormatted,
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "All application configurations—including configured local folders, Jellyfin credentials (server IP, login, password), saved cloud streams, and reader/player preferences—are continuously saved and backed up in a standardized JSON file.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    // Breakdown chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConfigStatPill(
                            label = "Folders",
                            value = "${configFileStats.folderCount}",
                            modifier = Modifier.weight(1f)
                        )
                        ConfigStatPill(
                            label = "Jellyfin",
                            value = if (configFileStats.hasJellyfin) "Configured" else "None",
                            modifier = Modifier.weight(1f)
                        )
                        ConfigStatPill(
                            label = "Streams",
                            value = "${configFileStats.cloudStreamCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    // Export / Import Primary Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportConfigLauncher.launch("omniviewer_config.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_export_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                importConfigLauncher.launch(
                                    arrayOf("application/json", "text/*", "*/*")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryViolet.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_import_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Secondary actions: View/Copy JSON & Paste JSON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    jsonPreviewContent = onGetConfigJsonString()
                                    showJsonPreviewDialog = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_view_json_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View / Copy JSON", fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                pasteJsonText = ""
                                showPasteImportDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_paste_json_config")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste JSON Text", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section 4: System Status & Supported Formats
        item {
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = "System Engines & Formats"
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineInfoRow("Media3 ExoPlayer", "Hardware-accelerated MP4 & HLS playback")
                    EngineInfoRow("Vector PDF Engine", "High-fidelity rendering via Android PdfRenderer")
                    EngineInfoRow("Comic & Photo Engine", "Sub-pixel zoomable pinch-to-zoom engine")
                    EngineInfoRow("Jellyfin Client", "REST API v10.8+ with token auth & transcoder")
                }
            }
        }
    }

    // View / Copy JSON Preview Dialog
    if (showJsonPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showJsonPreviewDialog = false },
            modifier = Modifier.testTag("dialog_json_preview"),
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved Configuration (JSON)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonPreviewContent,
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepObsidian, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(jsonPreviewContent))
                        showJsonPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_copy_json_to_clipboard")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonPreviewDialog = false }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Paste JSON Text Dialog
    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            modifier = Modifier.testTag("dialog_paste_import"),
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste your omniviewer_config.json content below to restore all settings, folders, Jellyfin credentials, and cloud streams.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = pasteJsonText,
                        onValueChange = { pasteJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("input_paste_json"),
                        placeholder = {
                            Text(
                                "{\n  \"version\": 1,\n  \"app\": \"OmniViewer\",\n  ...\n}",
                                color = Color.White.copy(alpha = 0.3f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = SurfaceElevated
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteJsonText.isNotBlank()) {
                            onImportConfigFromString(pasteJsonText.trim())
                            showPasteImportDialog = false
                        }
                    },
                    enabled = pasteJsonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_apply_pasted_json")
                ) {
                    Text("Apply Configuration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

data class OrientationChoice(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun OrientationOptionSelector(
    title: String,
    description: String,
    options: List<OrientationChoice>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { choice ->
                val bg = if (choice.isSelected) PrimaryViolet.copy(alpha = 0.25f) else DeepObsidian
                val border = if (choice.isSelected) PrimaryViolet else CardBorder

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                        .clickable { choice.onClick() }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = choice.icon,
                            contentDescription = null,
                            tint = if (choice.isSelected) AccentCyan else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = choice.label,
                            fontWeight = if (choice.isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (choice.isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfiguredFolderCard(
    folder: MediaFolder,
    onRescan: () -> Unit,
    onRemove: () -> Unit
) {
    val dateStr = remember(folder.lastScannedMs) {
        if (folder.lastScannedMs > 0) {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            sdf.format(Date(folder.lastScannedMs))
        } else "Not scanned"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${folder.fileCount} items",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scanned $dateStr",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onRescan,
                modifier = Modifier.size(36.dp).testTag("btn_rescan_folder_${folder.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp).testTag("btn_remove_folder_${folder.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EngineInfoRow(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentCyan)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ConfigStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DeepObsidian)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

