package com.ronin71.pdfcustom.model

import androidx.compose.ui.graphics.Color


data class MyCustomPdfSettings(
    val isDarkMode: Boolean = false,
    val contrast: Float = 1f,
    val fontFamily: String = "System",
    val backgroundColor: Color = Color.White,
    val textColor: Color = Color.Black
)

data class MyCustomPdfAnalysis(
    val dominantTextColor: Color,
    val dominantBackgroundColor: Color,
    val suggestedMode: Boolean // true = dark mode, false = light mode
)