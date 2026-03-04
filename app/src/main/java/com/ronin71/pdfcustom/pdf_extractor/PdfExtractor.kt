package com.ronin71.pdfcustom.pdf_extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import com.ronin71.pdfcustom.model.MyPdfModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

class PdfExtractor {
    val TAG = "PdfExtractor"


    fun extract(context: Context, uri: Uri): MyPdfModel? {
        try {
            val id = DocumentsContract.getDocumentId(uri).substringAfter(":", "")

            val pdfCacheFile = copyUriToCache(context, uri, id)
            if (pdfCacheFile != null) {
                val pdfPath = pdfCacheFile.path
                val file = File(pdfPath)

                var extractedText = ""
                val extractedImages = mutableListOf<Bitmap>()

                PDDocument.load(file).use { document ->
                    // ✅ Extract text
                    val stripper = PDFTextStripper()
                    extractedText = stripper.getText(document)

                    // ✅ Extract images
                    for (page in document.pages) {
                        val resources = page.resources
                        extractImagesFromResources(resources, extractedImages)
                    }
                }

                Log.d(TAG, "PdfExtractor extract text: $extractedText")
                Log.d(TAG, "PdfExtractor extract images count: ${extractedImages.size}")

                return MyPdfModel(id, uri, pdfPath, extractedText, extractedImages)
            } else {
                return null
            }

        } catch (e: Exception) {
            throw Exception("PdfExtractor extract error: ${e.message}")
        }
    }


    private fun extractImagesFromResources(
        resources: PDResources,
        images: MutableList<Bitmap>
    ) {
        try {
            for (xObjectName in resources.xObjectNames) {
                val xObject = resources.getXObject(xObjectName)
                if (xObject is PDImageXObject) {
                    try {
                        val inputStream = xObject.createInputStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) images.add(bitmap)
                    } catch (e: Exception) {
                        Log.e(TAG, "extract image error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractImagesFromResources error: ${e.message}")
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