package com.ronin71.pdfcustom.pdf_extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.graphics.scale
import com.ronin71.pdfcustom.model.MyPdfModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.math.max


class PdfExtractor {
    val TAG = "PdfExtractor"

    fun extract(context: Context, uri: Uri): MyPdfModel? {
        return try {
            // Get document ID from URI
            val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")

            // Copy file to cache
            val pdfCacheFile = copyUriToCache(context, uri, id)
                ?: return null

            val pdfPath = pdfCacheFile.path
            val file = File(pdfPath)

            val listText = mutableListOf<String>()
            val pageBitmaps = mutableListOf<Bitmap>() // Danh sách bitmap của từng page

            PDDocument.load(file).use { document ->
                // 1. Extract text from all pages
                val stripper = PDFTextStripper()
                val totalPages = document.numberOfPages

                Log.d(TAG, "Total pages: $totalPages")

                for (pageIndex in 0 until totalPages) {
                    // Extract text
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    val pageText = stripper.getText(document)
                    listText.add(pageText)

                    Log.d(TAG, "Page ${pageIndex + 1} text length: ${pageText.length}")
                }

                val renderer = PDFRenderer(document)

                for (pageIndex in 0 until totalPages) {
                    val dpi = 150

                    val bitmap = renderer.renderImageWithDPI(pageIndex, dpi.toFloat())

                    // Option: Resize bitmap nếu quá lớn
                    val scaledBitmap = if (bitmap.width > 2000 || bitmap.height > 2000) {
                        val scale = 2000f / max(bitmap.width, bitmap.height)
                        bitmap.scale(
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt()
                        ).also {
                            bitmap.recycle() // Giải phóng bitmap gốc
                        }
                    } else {
                        bitmap
                    }

                    pageBitmaps.add(scaledBitmap)

                    Log.d(TAG, "Rendered page ${pageIndex + 1}: ${scaledBitmap.width}x${scaledBitmap.height}")
                }
            }

            Log.d(TAG, "Extracted ${listText.size} pages of text")
            Log.d(TAG, "Rendered ${pageBitmaps.size} page bitmaps")

            // Return model với page bitmaps (không phải extracted images)
            MyPdfModel(
                id = id,
                uri = uri,
                pathCache = pdfPath,
                listText = listText,
                images = pageBitmaps // Đây là bitmap của từng page đã render
            )

        } catch (e: Exception) {
            Log.e(TAG, "PdfExtractor extract error: ${e.message}", e)
            throw Exception("PdfExtractor extract error: ${e.message}")
        }
    }

    /**
     * Alternative: Sử dụng PdfRenderer của Android (nhanh hơn, nhưng chỉ từ API 21)
     */
    fun extractWithPdfRenderer(context: Context, uri: Uri): MyPdfModel? {
        return try {
            val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")
            val pdfCacheFile = copyUriToCache(context, uri, id) ?: return null
            val pdfPath = pdfCacheFile.path
            val file = File(pdfPath)

            val listText = mutableListOf<String>()
            val pageBitmaps = mutableListOf<Bitmap>()

            // Dùng PdfRenderer của Android
            val parcelFileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            PdfRenderer(parcelFileDescriptor).use { renderer ->
                val pageCount = renderer.pageCount
                Log.d(TAG, "Total pages: $pageCount")

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)

                    // Tạo bitmap với kích thước gấp đôi để chất lượng tốt
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )

                    // Render page vào bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    pageBitmaps.add(bitmap)
                    page.close()

                    Log.d(TAG, "Rendered page ${i + 1}: ${bitmap.width}x${bitmap.height}")
                }
            }

            parcelFileDescriptor.close()

            // Extract text bằng PDFBox (vì PdfRenderer không extract text được)
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                val totalPages = document.numberOfPages

                for (pageIndex in 0 until totalPages) {
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    val pageText = stripper.getText(document)
                    listText.add(pageText)
                }
            }

            MyPdfModel(
                id = id,
                uri = uri,
                pathCache = pdfPath,
                listText = listText,
                images = pageBitmaps
            )

        } catch (e: Exception) {
            Log.e(TAG, "extractWithPdfRenderer error: ${e.message}", e)
            throw Exception("PdfExtractor extract error: ${e.message}")
        }
    }

    // Các hàm phụ trợ giữ nguyên
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
            return file
        } catch (e: Exception) {
            Log.e(TAG, "copyUriToCache error: ${e.message}", e)
            return null
        }
    }

    fun cleanCache(context: Context, fileName: String): Boolean {
        return try {
            val file = File(context.cacheDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "cleanCache error: ${e.message}")
            false
        }
    }
}