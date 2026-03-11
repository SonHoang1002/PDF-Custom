package com.ronin71.pdfcustom.model

import android.graphics.Bitmap
import android.net.Uri

data class MyPdfModel(
    val id: String = "",
    val uri: Uri? = null,
    val pathCache: String? = null,
    val listText: List<String> = emptyList(),
    val images: List<Bitmap> = emptyList()
) {
    fun getPageText(pageIndex: Int): String {
        return if (pageIndex in listText.indices) {
            listText[pageIndex]
        } else {
            ""
        }
    }

    val pageCount: Int
        get() = listText.size

    val hasContent: Boolean
        get() = listText.isNotEmpty() || images.isNotEmpty()

    val hasImages: Boolean
        get() = images.isNotEmpty()
}

sealed class PdfPageResult {
    data class Page(
        val index: Int,
        val pageNumber: Int,
        val totalPages: Int,
        val bitmap: Bitmap,
        val text: String
    ) : PdfPageResult()

    data class Progress(
        val currentPage: Int,
        val totalPages: Int,
        val message: String
    ) : PdfPageResult()

    data class TotalPages(val totalPages: Int) : PdfPageResult()
    data class Info(val message: String) : PdfPageResult()
    object Complete : PdfPageResult()
    data class Error(val message: String) : PdfPageResult()
}
