package com.ronin71.pdfcustom.model

import android.net.Uri

data class MyPdfModel(
    val id: String = "",
    val uri: Uri?,
    val pathCache: String?,
    val text: String = "",

)