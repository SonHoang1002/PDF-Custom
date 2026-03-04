package com.ronin71.pdfcustom.pdf_extractor

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.ronin71.pdfcustom.model.MyPdfModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

class PdfExtractor {
    val TAG = "PdfExtractor"
    fun extract(context: Context, uri: Uri): MyPdfModel? {
        try {
            val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")

            val pdfCacheFile = copyUriToCache(context, uri, id)
            if (pdfCacheFile != null) {
                val pdfPath = pdfCacheFile.path
                Log.d(TAG, "PdfExtractor extract: $pdfPath")
                val result = PDDocument.load(File(pdfPath)).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.getText(document)
                }
                Log.d(TAG, "PdfExtractor extract result: $result")
                return  MyPdfModel(id, uri, pdfPath, result)
            } else {
                return null
            }

        } catch (e: Exception) {
            throw Exception("PdfExtractor extract error: ${e.message}")
        }
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
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}