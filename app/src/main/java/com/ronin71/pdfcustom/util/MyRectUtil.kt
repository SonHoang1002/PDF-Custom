package com.ronin71.pdfcustom.util

import android.util.Log
import android.util.SizeF

class MyRectUtil {
    val TAG = "RectUtil"
    fun fillSizeWithAspect(srcSize: SizeF, aspect: Float): SizeF {
        val srcAspect = (srcSize.width / srcSize.height)

        var result: SizeF
        if (aspect > srcAspect) {
            result = SizeF(
                srcSize.width,
                srcSize.width / aspect
            )
        } else {
            result = SizeF(
                srcSize.height * aspect,
                srcSize.height
            )
        }
        Log.d(TAG, "fillSizeWithAspect: aspect = $aspect, rcAspect = ${srcAspect}, result = $result")
        return result
    }
}