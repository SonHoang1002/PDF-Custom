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
    /**
     * Get text of a specific page
     */
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