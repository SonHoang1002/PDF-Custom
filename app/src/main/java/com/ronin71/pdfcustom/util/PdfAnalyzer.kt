package com.ronin71.pdfcustom.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.ronin71.pdfcustom.model.MyCustomPdfAnalysis
import com.ronin71.pdfcustom.model.MyPdfModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

suspend fun analyzePdf(model: MyPdfModel): MyCustomPdfAnalysis {
    if (model.images.isEmpty()) {
        return MyCustomPdfAnalysis(
            dominantTextColor = Color.Black,
            dominantBackgroundColor = Color.White,
            suggestedMode = false
        )
    }

    return withContext(Dispatchers.Default) {
        var totalTextColorR = 0f
        var totalTextColorG = 0f
        var totalTextColorB = 0f
        var textPixelCount = 0

        var totalBgColorR = 0f
        var totalBgColorG = 0f
        var totalBgColorB = 0f
        var bgPixelCount = 0

        // Phân tích mẫu từ page đầu tiên
        val sampleBitmap = model.images.firstOrNull() ?: return@withContext MyCustomPdfAnalysis(
            dominantTextColor = Color.Black,
            dominantBackgroundColor = Color.White,
            suggestedMode = false
        )

        val width = min(sampleBitmap.width, 500) // Lấy mẫu tối đa 500px
        val height = min(sampleBitmap.height, 500)

        val pixels = IntArray(width * height)
        sampleBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Phân tích màu sắc
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = color.red
            val g = color.green
            val b = color.blue
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b)

            // Phân loại text vs background dựa trên độ tương phản
            if (luminance < 128) { // Giả định pixel tối là text
                totalTextColorR += r
                totalTextColorG += g
                totalTextColorB += b
                textPixelCount++
            } else { // Pixel sáng là background
                totalBgColorR += r
                totalBgColorG += g
                totalBgColorB += b
                bgPixelCount++
            }
        }

        val avgTextColor = if (textPixelCount > 0) {
            Color(
                (totalTextColorR / textPixelCount).toInt(),
                (totalTextColorG / textPixelCount).toInt(),
                (totalTextColorB / textPixelCount).toInt()
            )
        } else {
            Color.Black
        }

        val avgBgColor = if (bgPixelCount > 0) {
            Color(
                (totalBgColorR / bgPixelCount).toInt(),
                (totalBgColorG / bgPixelCount).toInt(),
                (totalBgColorB / bgPixelCount).toInt()
            )
        } else {
            Color.White
        }

        // Đề xuất chế độ
        val bgLuminance = (0.299 * avgBgColor.red + 0.587 * avgBgColor.green + 0.114 * avgBgColor.blue)
        val suggestedDarkMode = bgLuminance > 128 // Nền sáng thì đề xuất dark mode

        MyCustomPdfAnalysis(
            dominantTextColor = avgTextColor,
            dominantBackgroundColor = avgBgColor,
            suggestedMode = suggestedDarkMode
        )
    }
}

// Extension để kiểm tra màu sáng/tối
fun Color.isLight(): Boolean {
    return (red * 0.299 + green * 0.587 + blue * 0.114) > 128
}