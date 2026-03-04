package com.ronin71.pdfcustom.model

import android.graphics.Bitmap
import android.net.Uri

data class MyPdfModel(
    val id: String = "",
    val uri: Uri?,
    val pathCache: String?,
    val text: String = "",
    val images: List<Bitmap> = emptyList()
)