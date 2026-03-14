package com.ronin71.pdfcustom.common

import android.util.SizeF

fun SizeF.aspectRatio(): Float {
    return this.width / this.height
}
