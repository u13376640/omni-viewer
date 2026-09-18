package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MediaFolder
import com.example.data.model.ReadingDirection
import com.example.data.model.UserSettings
import com.example.data.model.VideoAspectMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("omni_viewer_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val directionStr = prefs.getString("reading_direction", null)
        val direction = when (directionStr) {
            ReadingDirection.LEFT_TO_RIGHT.name -> ReadingDirection.LEFT_TO_RIGHT
            ReadingDirection.VERTICAL_CONTINUOUS.name -> ReadingDirection.VERTICAL_CONTINUOUS
            // Legacy migrations
            "VERTICAL_WEBTOON" -> ReadingDirection.VERTICAL_CONTINUOUS
            "HORIZONTAL_PAGED" -> ReadingDirection.LEFT_TO_RIGHT
            "RIGHT_TO_LEFT" -> ReadingDirection.LEFT_TO_RIGHT
            else -> ReadingDirection.LEFT_TO_RIGHT
        }

        val aspectModeStr = prefs.getString("video_aspect", VideoAspectMode.FIT.name)
        val aspectMode = runCatching { VideoAspectMode.valueOf(aspectModeStr!!) }
            .getOrDefault(VideoAspectMode.FIT)

        val autoPlay = prefs.getBoolean("video_autoplay", true)

        val foldersJson = prefs.getString("configured_folders", "[]") ?: "[]"
        val folders = parseFolders(foldersJson)

        return UserSettings(
            readingDirection = direction,
            videoAspectMode = aspectMode,
            autoPlayVideos = autoPlay,
            configuredFolders = folders
        )
    }

    private fun parseFolders(jsonStr: String): List<MediaFolder> {
        val list = mutableListOf<MediaFolder>()
        runCatching {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MediaFolder(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        uriString = obj.getString("uriString"),
                        fileCount = obj.optInt("fileCount", 0),
                        lastScannedMs = obj.optLong("lastScannedMs", System.currentTimeMillis()),
                        coverUrl = obj.optString("coverUrl").ifBlank { null }
                    )
                )
            }
        }
        return list
    }

    private fun saveFolders(folders: List<MediaFolder>) {
        val array = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("uriString", f.uriString)
            obj.put("fileCount", f.fileCount)
            obj.put("lastScannedMs", f.lastScannedMs)
            obj.put("coverUrl", f.coverUrl ?: "")
            array.put(obj)
        }
        prefs.edit().putString("configured_folders", array.toString()).apply()
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        prefs.edit().putString("reading_direction", direction.name).apply()
        _settings.value = _settings.value.copy(readingDirection = direction)
    }

    fun updateVideoAspectMode(mode: VideoAspectMode) {
        prefs.edit().putString("video_aspect", mode.name).apply()
        _settings.value = _settings.value.copy(videoAspectMode = mode)
    }

    fun updateAutoPlay(autoPlay: Boolean) {
        prefs.edit().putBoolean("video_autoplay", autoPlay).apply()
        _settings.value = _settings.value.copy(autoPlayVideos = autoPlay)
    }

    fun addConfiguredFolder(folder: MediaFolder) {
        val current = _settings.value.configuredFolders.toMutableList()
        val index = current.indexOfFirst { it.uriString == folder.uriString || it.id == folder.id }
        if (index >= 0) {
            current[index] = folder
        } else {
            current.add(folder)
        }
        saveFolders(current)
        _settings.value = _settings.value.copy(configuredFolders = current)
    }

    fun updateConfiguredFolder(folder: MediaFolder) {
        val current = _settings.value.configuredFolders.toMutableList()
        val index = current.indexOfFirst { it.id == folder.id }
        if (index >= 0) {
            current[index] = folder
            saveFolders(current)
            _settings.value = _settings.value.copy(configuredFolders = current)
        }
    }

    fun removeConfiguredFolder(folderId: String) {
        val updated = _settings.value.configuredFolders.filterNot { it.id == folderId }
        saveFolders(updated)
        _settings.value = _settings.value.copy(configuredFolders = updated)
    }

    fun updateFolderStats(folderId: String, count: Int) {
        val updated = _settings.value.configuredFolders.map {
            if (it.id == folderId) it.copy(fileCount = count, lastScannedMs = System.currentTimeMillis())
            else it
        }
        saveFolders(updated)
        _settings.value = _settings.value.copy(configuredFolders = updated)
    }

    fun restoreFullSettings(
        direction: ReadingDirection,
        aspectMode: VideoAspectMode,
        autoPlay: Boolean,
        folders: List<MediaFolder>
    ) {
        prefs.edit()
            .putString("reading_direction", direction.name)
            .putString("video_aspect", aspectMode.name)
            .putBoolean("video_autoplay", autoPlay)
            .apply()
        saveFolders(folders)
        _settings.value = UserSettings(
            readingDirection = direction,
            videoAspectMode = aspectMode,
            autoPlayVideos = autoPlay,
            configuredFolders = folders
        )
    }

    fun saveJellyfinCredentials(serverUrl: String, username: String, password: String) {
        prefs.edit()
            .putString("jellyfin_server_url", serverUrl)
            .putString("jellyfin_username", username)
            .putString("jellyfin_password", password)
            .apply()
    }

    fun getJellyfinServerUrl(): String = prefs.getString("jellyfin_server_url", "") ?: ""
    fun getJellyfinUsername(): String = prefs.getString("jellyfin_username", "") ?: ""
    fun getJellyfinPassword(): String = prefs.getString("jellyfin_password", "") ?: ""
}
