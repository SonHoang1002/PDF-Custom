package com.ronin71.pdfcustom.ui.screen

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ronin71.pdfcustom.model.MyPdfModelMain
import com.ronin71.pdfcustom.viewmodel.MyPdfViewModel

@Composable
fun HomeScreen(
    onNavigateToPreview: (uri: Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onNavigateToPreview(it)
            Log.d("HomeScreen", "Chọn file PDF: $uri")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "PDF Custom",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { launcher.launch("application/pdf") },

                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import PDF")
                    }
                }
            }
        }
    }
}

private fun getProcessingStatus(progress: Float, currentPage: Int, totalPages: Int): String {
    return when {
        progress < 0.1f -> "Đang copy file..."
        progress < 0.2f -> "Đang mở file PDF..."
        totalPages > 0 -> "Đang xử lý trang $currentPage/$totalPages..."
        else -> "Đang xử lý..."
    }
}

