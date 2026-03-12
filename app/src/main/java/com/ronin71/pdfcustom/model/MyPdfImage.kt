package com.ronin71.pdfcustom.model

import android.graphics.Bitmap
import android.graphics.RectF

data class MyPdfImage (
    val bitmap: Bitmap,
    val rect: RectF,
)