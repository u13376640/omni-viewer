package com.example.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GooglePhotosAlbumInfo(
    val title: String,
    val coverUrl: String,
    val photoUrls: List<String> = emptyList(),
    val directShareUrl: String = ""
)

object GooglePhotosHelper {
    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Crawler and browser user-agents known to receive Open Graph meta tags from Google Photos
    private val USER_AGENTS = listOf(
        "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
        "Twitterbot/1.0",
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    )

    fun isGooglePhotosUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.contains("photos.app.goo.gl") ||
                u.contains("photos.google.com") ||
                u.contains("goo.gl/photos")
    }

    fun isWebPhotoAlbumUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        if (!u.startsWith("http://") && !u.startsWith("https://")) return false
        val directImageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".svg")
        if (directImageExtensions.any { u.endsWith(it) || u.contains("$it?") }) return false
        return isGooglePhotosUrl(url) ||
                u.contains("flickr.com") ||
                u.contains("imgur.com/a/") ||
                u.contains("/album") ||
                u.contains("/gallery") ||
                u.contains("/photos")
    }

    suspend fun extractAlbumInfo(url: String): GooglePhotosAlbumInfo? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext null

        // Try extracting with social crawler UA first (most reliable for og:image from Google Photos),
        // falling back to desktop/mobile browser user agents if needed.
        for (ua in USER_AGENTS) {
            try {
                val info = fetchAndParseAlbum(cleanUrl, ua)
                if (info != null && info.coverUrl.isNotBlank()) {
                    return@withContext info
                }
            } catch (_: Exception) {
                // Try next user-agent
            }
        }

        // Return best effort if any was found without cover
        try {
            fetchAndParseAlbum(cleanUrl, USER_AGENTS[1])
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchAndParseAlbum(url: String, userAgent: String): GooglePhotosAlbumInfo? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null
        val finalUrl = response.request.url.toString()

        // 1. Extract Album Title
        val ogTitle1 = Regex("""<meta[^>]+property=["'](?:og:title)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val ogTitle2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["'](?:og:title)["']""", RegexOption.IGNORE_CASE)
        val twitterTitle1 = Regex("""<meta[^>]+name=["'](?:twitter:title)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val twitterTitle2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+name=["'](?:twitter:title)["']""", RegexOption.IGNORE_CASE)
        val docTitle = Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE)

        val rawTitle = ogTitle1.find(html)?.groupValues?.get(1)
            ?: ogTitle2.find(html)?.groupValues?.get(1)
            ?: twitterTitle1.find(html)?.groupValues?.get(1)
            ?: twitterTitle2.find(html)?.groupValues?.get(1)
            ?: docTitle.find(html)?.groupValues?.get(1)
            ?: "Google Photos Album"

        val cleanTitle = unescapeHtml(rawTitle)
            .replace(" - Google Photos", "")
            .replace(" – Google Photos", "")
            .replace("Google Photos", "")
            .trim()
            .ifBlank { "Google Photos Album" }

        // 2. Extract Album Cover Image
        // Matches <meta property="og:image" content="..."> as well as <meta content="..." property="og:image">
        val ogImg1 = Regex("""<meta[^>]+property=["'](?:og:image|og:image:secure_url)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val ogImg2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["'](?:og:image|og:image:secure_url)["']""", RegexOption.IGNORE_CASE)
        val twImg1 = Regex("""<meta[^>]+name=["'](?:twitter:image|twitter:image:src)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val twImg2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+name=["'](?:twitter:image|twitter:image:src)["']""", RegexOption.IGNORE_CASE)
        val itemPropImg = Regex("""<meta[^>]+itemprop=["']image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val linkImg = Regex("""<link[^>]+rel=["'](?:image_src|shortcut icon)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        var rawCover = ogImg1.find(html)?.groupValues?.get(1)
            ?: ogImg2.find(html)?.groupValues?.get(1)
            ?: twImg1.find(html)?.groupValues?.get(1)
            ?: twImg2.find(html)?.groupValues?.get(1)
            ?: itemPropImg.find(html)?.groupValues?.get(1)
            ?: linkImg.find(html)?.groupValues?.get(1)

        // 3. Extract individual photos if embedded in HTML source
        val photoRegex = Regex("""https://lh[0-9]\.googleusercontent\.com/(?:pw/)?[a-zA-Z0-9_\-]+(?:=[a-zA-Z0-9_\-]+)?""")
        val matchedUrls = photoRegex.findAll(html)
            .map { unescapeHtml(it.value) }
            .distinct()
            .filter { it.length > 50 } // Exclude small static icons/placeholders
            .map { formatGooglePhotosUrl(it) }
            .toList()

        if (rawCover.isNullOrBlank() && matchedUrls.isNotEmpty()) {
            rawCover = matchedUrls.first()
        }

        val coverUrl = rawCover?.let { formatGooglePhotosUrl(unescapeHtml(it)) } ?: ""

        return GooglePhotosAlbumInfo(
            title = cleanTitle,
            coverUrl = coverUrl,
            photoUrls = matchedUrls,
            directShareUrl = finalUrl
        )
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    fun formatGooglePhotosUrl(rawUrl: String): String {
        val clean = unescapeHtml(rawUrl)
        return if (clean.contains("googleusercontent.com")) {
            if (clean.contains("=")) {
                // If it already has sizing like =w...-h... or =s..., keep it as high quality
                clean
            } else {
                // Append high resolution modifier
                "$clean=w1200-h800-no"
            }
        } else {
            clean
        }
    }
}
