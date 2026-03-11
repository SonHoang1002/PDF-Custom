package com.ronin71.pdfcustom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ronin71.pdfcustom.ui.screen.DemoDecodeBitmap
import com.ronin71.pdfcustom.ui.screen.MainScreen
import com.ronin71.pdfcustom.ui.theme.DarkCustomTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            DarkCustomTheme {
//                MainScreen()
//                 DemoDecodeBitmap()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    DarkCustomTheme {
        MainScreen()
    }
}