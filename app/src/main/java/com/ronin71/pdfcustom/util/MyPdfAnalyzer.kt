package com.ronin71.pdfcustom.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.util.SizeF
import androidx.core.graphics.createBitmap
import com.ronin71.pdfcustom.model.MyPageResult
import com.ronin71.pdfcustom.model.MyPdfImage
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


class MyPdfAnalyzer(val pdfCacheFile: File, val context: Context, val uri: Uri) {

    val TAG = "PdfAnalyzer"

    // Document và renderer dùng chung
    private var _document: PDDocument
    private var _renderer: PDFRenderer
    private var _totalPages: Int = 0

    init {
        try {
            _document = PDDocument.load(pdfCacheFile)
            _renderer = PDFRenderer(_document)
            _totalPages = _document.numberOfPages
            Log.d(TAG, "PDF Document loaded: $_totalPages pages")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading PDF: ${e.message}")
            throw e
        }
    }

    // Lấy tổng số trang
    suspend fun getTotalPages(): Int = withContext(Dispatchers.IO) {
        return@withContext _totalPages
    }

    // Extract toàn bộ PDF (dùng cho cache)
    suspend fun extractPdf(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): MyPdfModelMain = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== BẮT ĐẦU XỬ LÝ PDF ===")
        Log.d(TAG, "URI: $uri")

        val pages = ArrayList<MyPdfPage>()
        val totalPages = _totalPages

        onProgress(0.1f)

        for (pageIndex in 0 until totalPages) {
            Log.d(TAG, "--- Đang xử lý trang ${pageIndex + 1}/$totalPages ---")

            try {
                val page = _document.getPage(pageIndex)
                val pageSize = SizeF(page.mediaBox.width, page.mediaBox.height)
                Log.d(TAG, "Kích thước trang: ${pageSize.width} x ${pageSize.height}")

                // Render bitmap
                val bitmapPage = _renderer.renderImageWithDPI(pageIndex, 72f)
                Log.d(TAG, "  Đã render bitmap: ${bitmapPage.width}x${bitmapPage.height}")

                // Extract text
                val textsWithPosition = try {
                    extractTextWithPosition(_document, pageIndex).also { texts ->
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
                        bitmapPage = bitmapPage
                    )
                )

                val progress = 0.1f + (0.9f * (pageIndex + 1) / totalPages)
                onProgress(progress)

                Log.d(TAG, "✅ Hoàn thành trang ${pageIndex + 1}/$totalPages")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi xử lý trang ${pageIndex + 1}: ${e.message}")

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

        return@withContext MyPdfModelMain(uri, pages)
    }

    // Flow để generate từng page
    suspend fun extractPdfFlow(
        startPage: Int = 0,
        pageCount: Int = Int.MAX_VALUE
    ): Flow<MyPageResult> = flow {
        val totalPages = _totalPages
        val endPage = minOf(startPage + pageCount, totalPages)

        if (startPage == 0) {
            emit(MyPageResult.Progress(0.1f))
        }

        for (pageIndex in startPage until endPage) {
            try {
                Log.d(TAG, "🔄 Generate trang $pageIndex")

                val page = generatePage(pageIndex)
                emit(MyPageResult.Page(pageIndex, page))

                val progress = 0.1f + (0.9f * (pageIndex + 1) / totalPages)
                emit(MyPageResult.Progress(progress))

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý trang $pageIndex", e)

                val defaultBitmap = createBitmap(595, 842).apply {
                    eraseColor(android.graphics.Color.WHITE)
                }

                val errorPage = MyPdfPage(
                    size = SizeF(595f, 842f),
                    texts = ArrayList(),
                    images = ArrayList(),
                    bitmapPage = defaultBitmap
                )
                emit(MyPageResult.Page(pageIndex, errorPage))
            }
        }
    }.flowOn(Dispatchers.IO)

    // Generate một trang cụ thể
    suspend fun generatePage(pageIndex: Int): MyPdfPage = withContext(Dispatchers.IO) {
        val page = _document.getPage(pageIndex)
        val pageSize = SizeF(page.mediaBox.width, page.mediaBox.height)

        // Render bitmap
        val bitmapPage = _renderer.renderImageWithDPI(pageIndex, 72f)

        // Extract text
        val textsWithPosition = extractTextWithPosition(_document, pageIndex)

        // Extract images
        val imagesWithPosition = extractImagesWithPosition(page)

        MyPdfPage(
            size = pageSize,
            texts = ArrayList(textsWithPosition),
            images = ArrayList(imagesWithPosition),
            bitmapPage = bitmapPage
        )
    }

    // Generate nhiều trang cùng lúc
    suspend fun generatePages(pageIndices: List<Int>): List<MyPdfPage> =
        withContext(Dispatchers.IO) {
            val pages = mutableListOf<MyPdfPage>()
            for (index in pageIndices) {
                pages.add(generatePage(index))
            }
            return@withContext pages
        }

    // Extract text với vị trí
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

    // Extract images với vị trí
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
                            val position = getImagePosition(page)
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

    // Lấy vị trí image (mặc định full page)
    private fun getImagePosition(page: PDPage): RectF {
        val mediaBox = page.mediaBox
        return RectF(0f, 0f, mediaBox.width, mediaBox.height)
    }

    // Giải phóng tài nguyên
    fun close() {
        try {
            _document.close()
            Log.d(TAG, "PDF Document closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing document: ${e.message}")
        }
    }
}

fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")
        val fileName = "imported_${id}.pdf"
        val file = File(context.cacheDir, fileName)
        val contentResolver = context.contentResolver

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: return null

        Log.d("copyUriToCache", "Copied PDF to cache: ${file.absolutePath}")
        file
    } catch (e: Exception) {
        Log.e("copyUriToCache", "copyUriToCache error: ${e.message}")
        null
    }
}