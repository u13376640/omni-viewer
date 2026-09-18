package com.example.data.jellyfin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class JellyfinClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean.removeSuffix("/")
    }

    private fun createAuthHeader(token: String? = null): String {
        return buildString {
            append("MediaBrowser Client=\"OmniViewer\", ")
            append("Device=\"Android\", ")
            append("DeviceId=\"omniviewer-android-client\", ")
            append("Version=\"1.0.0\"")
            if (!token.isNullOrBlank()) {
                append(", Token=\"$token\"")
            }
        }
    }

    suspend fun testServerConnection(serverUrl: String): Result<JellyfinServerInfo> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$base/System/Info/Public")
                .header("X-Emby-Authorization", createAuthHeader())
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Server returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val name = json.optString("ServerName", "Jellyfin Server")
            val version = json.optString("Version", "Unknown")
            val id = json.optString("Id", "")

            Result.success(JellyfinServerInfo(name, version, id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinAuthResult> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val jsonBody = JSONObject().apply {
                put("Username", username)
                put("Pw", password)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$base/Users/AuthenticateByName")
                .header("X-Emby-Authorization", createAuthHeader())
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Authentication failed (HTTP ${response.code}): Invalid username or password"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val token = json.getString("AccessToken")
            val userObj = json.getJSONObject("User")
            val userId = userObj.getString("Id")
            val userName = userObj.optString("Name", username)
            val serverObj = json.optJSONObject("ServerInfo")
            val serverName = serverObj?.optString("Name") ?: "Jellyfin Server"

            Result.success(JellyfinAuthResult(token, userId, userName, serverName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLibraries(
        serverUrl: String,
        userId: String,
        accessToken: String
    ): Result<List<JellyfinLibraryItem>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val request = Request.Builder()
                .url("$base/Users/$userId/Views")
                .header("X-Emby-Authorization", createAuthHeader(accessToken))
                .header("X-Emby-Token", accessToken)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch libraries (HTTP ${response.code})"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val itemsArray = json.optJSONArray("Items") ?: JSONArray()
            val list = mutableListOf<JellyfinLibraryItem>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val id = item.getString("Id")
                val name = item.getString("Name")
                val type = item.optString("CollectionType", "mixed")
                val imageTag = item.optJSONObject("ImageTags")?.optString("Primary")

                list.add(JellyfinLibraryItem(id, name, type, imageTag))
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemsInLibrary(
        serverUrl: String,
        userId: String,
        accessToken: String,
        parentId: String? = null
    ): Result<List<JellyfinMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val base = sanitizeUrl(serverUrl)
            val urlBuilder = StringBuilder("$base/Users/$userId/Items?Recursive=true&Fields=Overview,RunTimeTicks,PrimaryImageAspectRatio,ProductionYear,Container")
            if (!parentId.isNullOrBlank()) {
                urlBuilder.append("&ParentId=$parentId")
            }
            urlBuilder.append("&IncludeItemTypes=Movie,Episode,Video,Photo,Book")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("X-Emby-Authorization", createAuthHeader(accessToken))
                .header("X-Emby-Token", accessToken)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to load media (HTTP ${response.code})"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val itemsArray = json.optJSONArray("Items") ?: JSONArray()
            val list = mutableListOf<JellyfinMediaItem>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val id = item.getString("Id")
                val name = item.getString("Name")
                val type = item.optString("Type", "Video")
                val overview = item.optString("Overview")
                val runTimeTicks = if (item.has("RunTimeTicks")) item.optLong("RunTimeTicks") else null
                val year = if (item.has("ProductionYear")) item.optInt("ProductionYear") else null
                val container = item.optString("Container")
                val imageTag = item.optJSONObject("ImageTags")?.optString("Primary")
                val isFolder = item.optBoolean("IsFolder", false)

                list.add(
                    JellyfinMediaItem(
                        id = id,
                        name = name,
                        type = type,
                        overview = overview,
                        runTimeTicks = runTimeTicks,
                        productionYear = year,
                        container = container,
                        primaryImageTag = imageTag,
                        parentId = parentId,
                        isFolder = isFolder
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getImageUrl(serverUrl: String, itemId: String, width: Int = 600, quality: Int = 90): String {
        val base = sanitizeUrl(serverUrl)
        return "$base/Items/$itemId/Images/Primary?fillWidth=$width&quality=$quality"
    }

    fun getVideoDirectStreamUrl(serverUrl: String, itemId: String, accessToken: String): String {
        val base = sanitizeUrl(serverUrl)
        return "$base/Videos/$itemId/stream.mp4?static=true&api_key=$accessToken"
    }
}
