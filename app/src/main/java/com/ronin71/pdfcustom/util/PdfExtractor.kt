package com.ronin71.pdfcustom.util//package com.ronin71.pdfcustom.util
//
//import android.content.Context
//import android.net.Uri
//import android.provider.DocumentsContract
//import android.util.Log
//import androidx.core.graphics.scale
//import com.ronin71.pdfcustom.model.PdfPageResult
//import com.tom_roush.pdfbox.pdmodel.PDDocument
//import com.tom_roush.pdfbox.rendering.PDFRenderer
//import com.tom_roush.pdfbox.text.PDFTextStripper
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.currentCoroutineContext
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.ensureActive
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOn
//import java.io.File
//import kotlin.math.max
//import kotlin.math.min
//
//class PdfExtractor {
//    val TAG = "PdfExtractor"
//
//    fun extractPagesWithFlow(
//        context: Context,
//        uri: Uri,
//        startPage: Int = 0,
//        pageCount: Int = 20
//    ): Flow<PdfPageResult> = flow {
//        try {
//            val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")
//            val pdfCacheFile = copyUriToCache(context, uri, id)
//                ?: throw Exception("Cannot copy file to cache")
//
//            val file = File(pdfCacheFile.path)
//
//            PDDocument.load(file).use { document ->
//                val totalPages = document.numberOfPages
//                val renderer = PDFRenderer(document)
//
//                // Gửi tổng số trang (chỉ gửi 1 lần)
//                emit(PdfPageResult.TotalPages(totalPages))
//
//                // Giới hạn số trang cần load
//                val endPage = min(startPage + pageCount, totalPages)
//
//                for (pageIndex in startPage until endPage) {
//                    currentCoroutineContext().ensureActive()
//
//                    // Extract text
//                    val stripper = PDFTextStripper()
//                    stripper.startPage = pageIndex + 1
//                    stripper.endPage = pageIndex + 1
//                    val pageText = stripper.getText(document)
//
//                    // Render page
//                    val dpi = 150
//                    val bitmap = renderer.renderImageWithDPI(pageIndex, dpi.toFloat())
//
//                    // Resize nếu cần
//                    val scaledBitmap = if (bitmap.width > 2000 || bitmap.height > 2000) {
//                        val scale = 2000f / max(bitmap.width, bitmap.height)
//                        bitmap.scale(
//                            (bitmap.width * scale).toInt(),
//                            (bitmap.height * scale).toInt()
//                        ).also { bitmap.recycle() }
//                    } else {
//                        bitmap
//                    }
//
//                    Log.d(TAG, "Rendered page ${pageIndex + 1}/$totalPages")
//
//                    // Emit page
//                    emit(
//                        PdfPageResult.Page(
//                            index = pageIndex,
//                            pageNumber = pageIndex + 1,
//                            totalPages = totalPages,
//                            bitmap = scaledBitmap,
//                            text = pageText
//                        )
//                    )
//
//                    // Progress
//                    emit(
//                        PdfPageResult.Progress(
//                            currentPage = pageIndex + 1,
//                            totalPages = totalPages,
//                            message = "Loaded page ${pageIndex + 1}/$totalPages"
//                        )
//                    )
//
//                    delay(10) // Small delay để UI không bị quá tải
//                }
//
//                emit(PdfPageResult.Complete)
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "extractPages error: ${e.message}", e)
//            emit(PdfPageResult.Error(e.message ?: "Unknown error"))
//        }
//    }.flowOn(Dispatchers.IO)
//
//    fun copyUriToCache(context: Context, uri: Uri, id: String): File? {
//        try {
//            val fileName = "imported_${id}.pdf"
//            val file = File(context.cacheDir, fileName)
//            val contentResolver = context.contentResolver
//
//            contentResolver.openInputStream(uri)?.use { input ->
//                file.outputStream().use { output ->
//                    input.copyTo(output)
//                }
//            } ?: return null
//
//            Log.d(TAG, "Copied PDF to cache: ${file.absolutePath}")
//            return file
//        } catch (e: Exception) {
//            Log.e(TAG, "copyUriToCache error: ${e.message}", e)
//            return null
//        }
//    }
//}
//
