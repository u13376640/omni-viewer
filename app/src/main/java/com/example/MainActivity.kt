package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.OmniViewerTheme
import com.example.ui.util.VideoThumbnailFetcher
import com.example.ui.viewmodel.OmniViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val viewModel: OmniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // OkHttpClient configured to send standard browser headers so CDNs like Google Photos (googleusercontent.com)
        // do not reject image thumbnail requests with 403 Forbidden.
        val coilOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                chain.proceed(requestBuilder.build())
            }
            .build()

        // Configure global Coil ImageLoader to decode video frames and reliably load cloud thumbnails
        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(coilOkHttpClient)
            .components {
                add(VideoThumbnailFetcher.Factory(this@MainActivity))
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            OmniViewerTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
