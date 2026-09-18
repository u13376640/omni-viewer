package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.config.ConfigurationManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.MediaFormat
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConfigurationThumbnailTest {

    private lateinit var db: AppDatabase
    private lateinit var configManager: ConfigurationManager
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settingsRepository = com.example.data.repository.SettingsRepository(context)
        configManager = ConfigurationManager(
            context = context,
            settingsRepository = settingsRepository,
            mediaDao = db.mediaDao(),
            jellyfinDao = db.jellyfinDao(),
            bookmarkDao = db.bookmarkDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `cloud stream with thumbnail url is saved and serialized to config json`() = runBlocking {
        val streamId = "cloud_${UUID.randomUUID()}"
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val testThumbnail = "https://example.com/thumbnails/mux_preview.jpg"

        val entity = MediaItemEntity(
            id = streamId,
            title = "Test Web View Stream",
            subtitle = "Cloud MP4 Stream",
            uriString = testUrl,
            mediaType = MediaType.VIDEO.name,
            mediaFormat = MediaFormat.MP4.name,
            mediaSource = MediaSource.CLOUD_URL.name,
            thumbnailUrl = testThumbnail,
            fileSizeFormatted = "Streaming"
        )
        db.mediaDao().insertMedia(entity)

        // Verify entity in DB
        val retrieved = db.mediaDao().getMediaByIdDirect(streamId)
        assertNotNull(retrieved)
        assertEquals(testThumbnail, retrieved?.thumbnailUrl)

        // Generate Configuration JSON
        val jsonString = configManager.generateFullConfigJsonString()
        val jsonObject = JSONObject(jsonString)

        assertTrue(jsonObject.has("savedCloudStreams"))
        val cloudArray = jsonObject.getJSONArray("savedCloudStreams")
        assertEquals(1, cloudArray.length())

        val streamObj = cloudArray.getJSONObject(0)
        assertEquals(streamId, streamObj.getString("id"))
        assertEquals(testUrl, streamObj.getString("url"))
        assertEquals(testThumbnail, streamObj.getString("thumbnailUrl"))

        // Delete from DB and test import back from JSON
        db.mediaDao().deleteMedia(streamId)
        assertEquals(0, db.mediaDao().getCloudStreamsDirect().size)

        // Import the config
        val result = configManager.importFromJsonString(jsonString).getOrThrow()
        assertTrue(result.success)

        val reimported = db.mediaDao().getCloudStreamsDirect()
        assertEquals(1, reimported.size)
        assertEquals(testThumbnail, reimported[0].thumbnailUrl)
        assertEquals(testUrl, reimported[0].uriString)
    }

    @Test
    fun `update thumbnail url query updates database`() = runBlocking {
        val streamId = "cloud_${UUID.randomUUID()}"
        val initialThumb = "https://example.com/thumb1.jpg"
        val updatedThumb = "https://example.com/thumb2.jpg"

        val entity = MediaItemEntity(
            id = streamId,
            title = "Stream with Updating Thumbnail",
            subtitle = "Cloud MP4 Stream",
            uriString = "https://example.com/stream.mp4",
            mediaType = MediaType.VIDEO.name,
            mediaFormat = MediaFormat.MP4.name,
            mediaSource = MediaSource.CLOUD_URL.name,
            thumbnailUrl = initialThumb,
            fileSizeFormatted = "Streaming"
        )
        db.mediaDao().insertMedia(entity)

        db.mediaDao().updateThumbnailUrl(streamId, updatedThumb)

        val updated = db.mediaDao().getMediaByIdDirect(streamId)
        assertEquals(updatedThumb, updated?.thumbnailUrl)
    }
}
