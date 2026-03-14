package com.ronin71.pdfcustom.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.util.Size
import android.util.SizeF
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.ronin71.pdfcustom.model.MyCustomPdfAnalysis
import com.ronin71.pdfcustom.model.MyPdfImage
import com.ronin71.pdfcustom.model.MyPdfModel
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.model.MyPdfPage
import com.ronin71.pdfcustom.model.MyPdfParagraph
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import androidx.core.graphics.createBitmap


class PdfAnalyzer {

    val TAG = "PdfAnalyzer"

    suspend fun extractPdf(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit
    ): MyPdfModelMain = withContext(Dispatchers.IO) {
        val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")
        Log.d(TAG, "=== BẮT ĐẦU XỬ LÝ PDF ===")
        Log.d(TAG, "URI: $uri")
        Log.d(TAG, "ID: $id")

        val pdfCacheFile = copyUriToCache(context, uri, id)
            ?: throw Exception("Cannot copy file to cache")

        Log.d(TAG, "File cache: ${pdfCacheFile.absolutePath}")
        Log.d(TAG, "File size: ${pdfCacheFile.length()} bytes")

        val file = File(pdfCacheFile.path)
        val pages = ArrayList<MyPdfPage>()

        PDDocument.load(file).use { document ->
            val totalPages = document.numberOfPages
            Log.d(TAG, "Tổng số trang: $totalPages")

            // Khởi tạo PDFRenderer để render bitmap
            val renderer = PDFRenderer(document)

            // Copy file xong => 10%
            onProgress(0.1f)

            for (pageIndex in 0 until totalPages) {
                Log.d(TAG, "--- Đang xử lý trang ${pageIndex + 1}/$totalPages ---")

                try {
                    val page = document.getPage(pageIndex)
                    val pageSize = getPageSize(page)
                    Log.d(TAG, "Kích thước trang: ${pageSize.width} x ${pageSize.height}")

                    val bitmapPage = renderer.renderImageWithDPI(pageIndex, 72f)
                    Log.d(TAG, "  Đã render bitmap: ${bitmapPage.width}x${bitmapPage.height}")

                    // Extract text
                    val textsWithPosition = try {
                        extractTextWithPosition(document, pageIndex).also { texts ->
                            Log.d(TAG, "  Trang ${pageIndex + 1}: Tìm thấy ${texts.size} paragraphs")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "  Lỗi extract text: ${e.message}")
                        emptyList()
                    }

                    // Extract images
                    val imagesWithPosition = try {
                        extractImagesWithPosition(page).also { images ->
                            Log.d(TAG, "  Trang ${pageIndex + 1}: Tìm thấy ${images.size} images")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "  Lỗi extract images: ${e.message}")
                        emptyList()
                    }

                    pages.add(
                        MyPdfPage(
                            size = pageSize,
                            texts = ArrayList(textsWithPosition),
                            images = ArrayList(imagesWithPosition),
                            bitmapPage = bitmapPage // THÊM BITMAP VÀO ĐÂY
                        )
                    )

                    // Tính progress
                    val progress = 0.1f + (0.9f * (pageIndex + 1) / totalPages)
                    onProgress(progress)

                    Log.d(TAG, "✅ Hoàn thành trang ${pageIndex + 1}/$totalPages")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi xử lý trang ${pageIndex + 1}: ${e.message}")

                    // Tạo bitmap trắng khi lỗi
                    val defaultBitmap = createBitmap(595, 842).apply {
                        eraseColor(android.graphics.Color.WHITE)
                    }

                    pages.add(
                        MyPdfPage(
                            size = SizeF(595f, 842f),
                            texts = ArrayList(),
                            images = ArrayList(),
                            bitmapPage = defaultBitmap
                        )
                    )

                    val progress = 0.1f + (0.9f * (pageIndex + 1) / totalPages)
                    onProgress(progress)
                }
            }

            Log.d(TAG, "=== KẾT THÚC ===")
            Log.d(TAG, "Tổng số trang: ${pages.size}")
        }

        return@withContext MyPdfModelMain(uri, pages)
    }

    private fun extractTextWithPosition(
        document: PDDocument,
        pageIndex: Int
    ): List<MyPdfParagraph> {
        val paragraphs = mutableListOf<MyPdfParagraph>()
        var lineCount = 0

        try {
            var currentParagraph = StringBuilder()
            var textRect: RectF? = null
            var currentTextSize = 0f
            var lastY = -1f
            var lastTextHeight = 0f

            val stripper = object : PDFTextStripper() {
                init {
                    setStartPage(pageIndex + 1)
                    setEndPage(pageIndex + 1)
                    setSortByPosition(true)
                }

                override fun writeString(text: String, textPositions: List<TextPosition>) {
                    if (textPositions.isEmpty()) return

                    try {
                        val firstPos = textPositions.first()
                        val lastPos = textPositions.last()

                        val fontSize = firstPos.fontSize
                        val textHeight = firstPos.height
                        val x = firstPos.x
                        val y = firstPos.y
                        val width = (lastPos.x + lastPos.width) - x

                        lineCount++
                        Log.v(TAG, "    Dòng $lineCount: '$text' tại ($x, $y)")

                        if (lastY != -1f && kotlin.math.abs(y - lastY) > textHeight * 1.5f) {
                            if (currentParagraph.isNotEmpty() && textRect != null) {
                                paragraphs.add(
                                    MyPdfParagraph(
                                        text = currentParagraph.toString().trim(),
                                        rect = textRect!!,
                                        textSize = currentTextSize.toInt(),
                                        textHeight = lastTextHeight,
                                    )
                                )
                                Log.v(TAG, "    📝 Lưu paragraph: ${currentParagraph.length} chars")
                            }
                            currentParagraph = StringBuilder()
                            textRect = RectF(x, y - textHeight, x + width, y)
                            currentTextSize = fontSize
                            lastTextHeight = textHeight
                        } else {
                            if (textRect == null) {
                                textRect = RectF(x, y - textHeight, x + width, y)
                                currentTextSize = fontSize
                                lastTextHeight = textHeight
                            } else {
                                textRect.right = maxOf(textRect.right, x + width)
                                textRect.top = minOf(textRect.top, y - textHeight)
                                currentTextSize = (currentTextSize + fontSize) / 2
                            }
                        }

                        currentParagraph.append(text)
                        lastY = y

                    } catch (e: Exception) {
                        Log.e(TAG, "    Lỗi xử lý dòng: ${e.message}")
                    }
                }
            }

            stripper.getText(document)

            // Thêm paragraph cuối cùng
            if (currentParagraph.isNotEmpty() && textRect != null) {
                paragraphs.add(
                    MyPdfParagraph(
                        text = currentParagraph.toString().trim(),
                        rect = textRect,
                        textSize = currentTextSize.toInt(),
                        textHeight = lastTextHeight
                    )
                )
                Log.v(TAG, "    📝 Lưu paragraph cuối: ${currentParagraph.length} chars")
            }

        } catch (e: Exception) {
            Log.e(TAG, "extractTextWithPosition error: ${e.message}")
        }

        return paragraphs
    }

    private fun extractImagesWithPosition(page: PDPage): List<MyPdfImage> {
        val images = mutableListOf<MyPdfImage>()
        var imageCount = 0

        try {
            val resources = page.resources
            val xObjectNames = resources.xObjectNames


            for (xObjectName in xObjectNames) {
                try {
                    val xObject = resources.getXObject(xObjectName)
                    if (xObject is PDImageXObject) {
                        imageCount++

                        val inputStream = xObject.createInputStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()

                        if (bitmap != null) {
                            val position = getImagePosition(page, xObjectName.name)
                            images.add(
                                MyPdfImage(
                                    bitmap = bitmap,
                                    rect = position
                                )
                            )
                            Log.v(TAG, "    🖼️ Image $imageCount: ${bitmap.width}x${bitmap.height}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "    Lỗi extract image $xObjectName: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "extractImagesWithPosition error: ${e.message}")
        }

        return images
    }

    private fun getImagePosition(page: PDPage, imageName: String): RectF {
        val mediaBox = page.mediaBox
        return RectF(0f, 0f, mediaBox.width, mediaBox.height)
    }

    private fun getPageSize(page: PDPage): SizeF {
        val mediaBox = page.mediaBox
        return SizeF(mediaBox.width, mediaBox.height)
    }

    fun copyUriToCache(context: Context, uri: Uri, id: String): File? {
        return try {
            val fileName = "imported_${id}.pdf"
            val file = File(context.cacheDir, fileName)
            val contentResolver = context.contentResolver

            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Log.d(TAG, "Copied PDF to cache: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "copyUriToCache error: ${e.message}")
            null
        }
    }
}


