package com.ronin71.pdfcustom.model

import android.graphics.Bitmap
import android.util.SizeF

data class MyPdfPage(
    val size: SizeF, /// Kích thước của page (width, height) đơn vị point của page trong pdf
    val texts: ArrayList<MyPdfParagraph>, /// Danh sách những item paragraph trong page đó
    val images: ArrayList<MyPdfImage>, /// Danh sách những item ảnh bên trong page đó
    val bitmapPage: Bitmap, /// Lấy ảnh bitmap của page
)
