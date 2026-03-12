package com.ronin71.pdfcustom.model

import android.graphics.RectF

data class MyPdfParagraph (
    val text: String,
    val rect: RectF,
    val textHeight: Float,
    val textSize: Int,
    )