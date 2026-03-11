package com.ronin71.pdfcustom.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.coroutineScope

class DemoPdfThemeMode {
//    viết hàm tổng hợp để có thể điều chỉnh màu, chỉnh độ tương phản, đổi màu của text ( giữ nguyên ảnh )
    suspend fun applyDarkModeToBitmap(original: Bitmap): Bitmap = coroutineScope {
        val result = createBitmap(
            original.width,
            original.height,
            original.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint()

        // Tạo color matrix để đảo màu
        val matrix = ColorMatrix().apply {
            // Công thức đảo màu: new = 255 - old
            val invertMatrix = floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,  // Red: -1*R + 255
                0f, -1f, 0f, 0f, 255f,  // Green: -1*G + 255
                0f, 0f, -1f, 0f, 255f,  // Blue: -1*B + 255
                0f, 0f, 0f, 1f, 0f      // Alpha giữ nguyên
            )
            set(invertMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return@coroutineScope result
    }

    suspend fun adjustBrightnessAndContrast(
        original: Bitmap,
        brightness: Float = 0f,  // -255 đến 255
        contrast: Float = 1f      // 0.5 đến 2.0
    ): Bitmap = coroutineScope {
        val result = createBitmap(
            original.width,
            original.height,
            original.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint()

        val matrix = ColorMatrix().apply {
            val translate = brightness * (1f - contrast)

            val contrastMatrix = floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            set(contrastMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return@coroutineScope result
    }
}
