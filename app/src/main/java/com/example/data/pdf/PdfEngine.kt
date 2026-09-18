package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfEngine(private val context: Context) {

    suspend fun getPageCount(uriString: String): Int = withContext(Dispatchers.IO) {
        try {
            openFileDescriptor(uriString)?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    return@withContext renderer.pageCount
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun renderPage(uriString: String, pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        try {
            openFileDescriptor(uriString)?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val height = (targetWidth * ratio).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        return@withContext bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun openFileDescriptor(uriString: String): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        try {
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
            } else if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                val cachedFile = resolveRemotePdf(uriString)
                if (cachedFile != null && cachedFile.exists()) {
                    ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            } else {
                val file = File(uriString)
                if (file.exists()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveRemotePdf(urlStr: String): File? {
        return try {
            val cacheKey = "stream_pdf_${urlStr.hashCode().toString().replace("-", "n")}.pdf"
            val cachedFile = File(context.cacheDir, cacheKey)
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                return cachedFile
            }
            val url = java.net.URL(urlStr)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OmniViewer/1.0")
            }
            connection.connect()
            if (connection.responseCode in 200..299) {
                val tempFile = File(context.cacheDir, "$cacheKey.tmp")
                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.renameTo(cachedFile)
                if (cachedFile.exists() && cachedFile.length() > 0L) {
                    return cachedFile
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createSampleComicPdf(): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "sample_comic_chronicles.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pagesData = listOf(
            Triple("CHRONICLES OF AETHER", "Chapter 1: The Awakening", "#1E1B4B"),
            Triple("PANEL 1", "The ancient sanctuary began to glow under the twilight moon...", "#0F172A"),
            Triple("PANEL 2", "Runes etched into stone tablets awakened with vibrant cyan energy.", "#1E293B"),
            Triple("PANEL 3", "'It is time,' the traveler whispered, gazing toward the floating citadel.", "#312E81")
        )

        pagesData.forEachIndexed { index, data ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Background
            val bgPaint = Paint().apply {
                color = Color.parseColor(data.third)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Comic frame
            val borderPaint = Paint().apply {
                color = Color.parseColor("#8B5CF6")
                strokeWidth = 6f
                style = Paint.Style.STROKE
            }
            canvas.drawRoundRect(RectF(30f, 40f, (pageWidth - 30).toFloat(), (pageHeight - 50).toFloat()), 16f, 16f, borderPaint)

            // Title
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(data.first, (pageWidth / 2).toFloat(), 120f, textPaint)

            // Subtitle / Story
            val subPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 16f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(data.second, (pageWidth / 2).toFloat(), 200f, subPaint)

            // Stylized Comic Panel Box in center
            val panelPaint = Paint().apply {
                color = Color.parseColor("#334155")
                style = Paint.Style.FILL
            }
            val centerRect = RectF(60f, 260f, (pageWidth - 60).toFloat(), (pageHeight - 160).toFloat())
            canvas.drawRoundRect(centerRect, 12f, 12f, panelPaint)

            val innerTextPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 14f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("[ HIGH DEFINITION COMIC ARTWORK ]", (pageWidth / 2).toFloat(), 480f, innerTextPaint)
            canvas.drawText("Page ${index + 1} of ${pagesData.size}", (pageWidth / 2).toFloat(), (pageHeight - 80).toFloat(), subPaint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        file
    }
}
